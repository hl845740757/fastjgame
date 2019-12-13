/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.redis;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.EventLoopGroup;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandler;
import com.wjybxx.fastjgame.concurrent.SingleThreadEventLoop;
import com.wjybxx.fastjgame.concurrent.disruptor.DisruptorEventLoop;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.util.ArrayDeque;
import java.util.concurrent.ThreadFactory;

/**
 * redis事件循环
 * <p>
 * Q: 为什么要把redis操作单独放在一个线程？
 * A: 由于{@link Pipeline}缓冲区满或显式调用{@link Pipeline#sync()}会引发阻塞，具体阻塞多久是无法估计的。
 * 我们不能让应用线程陷入阻塞。
 * <p>
 * Q: 它为什么不继承{@link DisruptorEventLoop}？
 * A: 它属于中间件，应用层与它之间是双向交互，使用{@link DisruptorEventLoop}可能导致死锁。
 * <p>
 * 对于大多数游戏而言，单个redis线程应该够用了，不过也很容易扩展为线程池模式(连接池)。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class RedisEventLoop extends SingleThreadEventLoop {

    private static final Logger logger = LoggerFactory.getLogger(RedisEventLoop.class);
    /**
     * 它决定了最多多少个命令执行一次{@link #sync()}
     */
    private static final int BATCH_TASK_SIZE = 512;

    private final JedisPoolAbstract jedisPool;

    private Jedis jedis;
    private Pipeline pipeline;

    private final ArrayDeque<JedisPipelineTask<?>> waitResponseTasks = new ArrayDeque<>(BATCH_TASK_SIZE);

    public RedisEventLoop(@Nullable EventLoopGroup parent,
                          @Nonnull ThreadFactory threadFactory,
                          @Nonnull RejectedExecutionHandler rejectedExecutionHandler,
                          @Nonnull JedisPoolAbstract jedisPool) {
        super(parent, threadFactory, rejectedExecutionHandler);
        this.jedisPool = jedisPool;
    }

    @Override
    protected void init() throws Exception {
        super.init();
        jedis = jedisPool.getResource();
        pipeline = jedis.pipelined();
    }

    @Override
    protected void clean() throws Exception {
        // close会进行同步
        closeQuietly(pipeline);
        closeQuietly(jedis);
    }

    @Override
    protected void loop() {
        while (true) {
            try {
                if (confirmShutdown()) {
                    break;
                }

                runTasksBatch(BATCH_TASK_SIZE);

                sync();

                // 等待以降低cpu利用率
                sleepQuietly();
            } catch (Throwable e) {
                // 避免错误的退出循环
                logger.warn("loop caught exception", e);
            }
        }
    }

    private void sync() {
        if (waitResponseTasks.isEmpty()) {
            return;
        }

        try {
            pipeline.sync();
        } catch (JedisException exception) {
            closeQuietly(pipeline);
            closeQuietly(jedis);
            jedis = jedisPool.getResource();
        } finally {
            // pipeline的缺陷：由于多个指令在同一个pipeline中，其中某一个出现异常，将导致后续的指令丢失响应，会抛出未赋值异常。
            JedisPipelineTask<?> task;
            while ((task = waitResponseTasks.pollFirst()) != null) {
                ConcurrentUtils.safeExecute(task.appEventLoop, newCallbackTaskSafely(task));
            }
            pipeline = jedis.pipelined();
        }
    }

    private <T> JedisCallbackTask newCallbackTaskSafely(JedisPipelineTask<T> task) {
        try {
            final T response = task.dependency.get();
            return new JedisCallbackTask(task.redisResponse, response);
        } catch (JedisDataException exception) {
            return new JedisCallbackTask(task.redisResponse, exception);
        }
    }

    <T> RedisResponse<T> enqueue(EventLoop appEventLoop, RedisPipelineCommand<T> pipelineCmd) {
        final DefaultRedisResponse<T> redisResponse = new DefaultRedisResponse<>();
        execute(new JedisPipelineTask<>(appEventLoop, pipelineCmd, redisResponse));
        return redisResponse;
    }

    private class JedisPipelineTask<T> implements Runnable {
        final EventLoop appEventLoop;
        final RedisPipelineCommand<T> pipelineCmd;
        final DefaultRedisResponse<T> redisResponse;
        Response<T> dependency;

        JedisPipelineTask(EventLoop appEventLoop, RedisPipelineCommand<T> pipelineCmd, DefaultRedisResponse<T> redisResponse) {
            this.appEventLoop = appEventLoop;
            this.pipelineCmd = pipelineCmd;
            this.redisResponse = redisResponse;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            waitResponseTasks.add(this);
            try {
                dependency = pipelineCmd.execute(pipeline);
            } catch (JedisException exception) {
                // 出现异常，手动生成结果
                dependency = new Response(BuilderFactory.OBJECT);
                dependency.set(new JedisDataException(exception));

                // 避免在该连接上出现更多的异常
                sync();
            }
        }
    }

    private static class JedisCallbackTask implements Runnable {

        final DefaultRedisResponse<?> redisResponse;
        final Object data;

        JedisCallbackTask(DefaultRedisResponse<?> redisResponse, Object data) {
            this.redisResponse = redisResponse;
            this.data = data;
        }

        @Override
        public void run() {
            redisResponse.onComplete(data);
        }
    }

    private static void closeQuietly(Closeable resource) {
        if (null != resource) {
            try {
                resource.close();
            } catch (Throwable ignore) {

            }
        }
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException ignore) {

        }
    }

}