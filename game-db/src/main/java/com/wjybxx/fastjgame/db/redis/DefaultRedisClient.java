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

package com.wjybxx.fastjgame.db.redis;

import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.LocalFuture;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletionException;
import java.util.function.Function;

/**
 * redisService的默认实现，它是一个本地service，其回调默认环境为用户所在线程{@link #appEventLoop}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/12/12
 * github - https://github.com/hl845740757
 */
public class DefaultRedisClient implements RedisClient {

    private final RedisEventLoop redisEventLoop;
    private final EventLoop appEventLoop;

    public DefaultRedisClient(RedisEventLoop redisEventLoop, EventLoop appEventLoop) {
        this.redisEventLoop = redisEventLoop;
        this.appEventLoop = appEventLoop;
    }

    @Override
    public void execute(@Nonnull PipelineCommand<?> command) {
        redisEventLoop.execute(command, false);
    }

    @Override
    public void executeAndFlush(@Nonnull PipelineCommand<?> command) {
        redisEventLoop.execute(command, true);
    }

    @Override
    public <T, U> LocalFuture<U> call(@Nonnull PipelineCommand<T> command, Function<T, U> decoder) {
        return redisEventLoop.call(command, decoder, false, appEventLoop);
    }

    @Override
    public <T, U> LocalFuture<U> callAndFlush(@Nonnull PipelineCommand<T> command, Function<T, U> decoder) {
        return redisEventLoop.call(command, decoder, true, appEventLoop);
    }

    @Override
    public <T, U> U syncCall(@Nonnull RedisCommand<T> command, Function<T, U> decoder) throws CompletionException {
        return redisEventLoop.syncCall(command, decoder);
    }

}
