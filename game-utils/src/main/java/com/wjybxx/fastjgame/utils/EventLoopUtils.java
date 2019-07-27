/*
 * Copyright 2019 wjybxx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wjybxx.fastjgame.utils;

import com.wjybxx.fastjgame.concurrent.BlockingOperationException;
import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.function.AnyRunnable;
import com.wjybxx.fastjgame.function.ExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

/**
 * 事件循环辅助方法。
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/14 14:27
 * github - https://github.com/hl845740757
 */
public class EventLoopUtils {

    private static final Logger logger = LoggerFactory.getLogger(EventLoopUtils.class);

    private EventLoopUtils() {

    }

    /**
     * 检查死锁，由于EventLoop是单线程的，因此不能在当前EventLoop上等待另一个任务完成，很可能导致死锁。
     * @param e executor
     *
     */
    public static void checkDeadLock(EventLoop e) {
        if (e != null && e.inEventLoop()) {
            throw new BlockingOperationException();
        }
    }

    /**
     * 检查死锁，由于EventLoop是单线程的，因此不能在当前EventLoop上等待另一个任务完成，很可能导致死锁。
     * @param e executor
     * @param msg 造成死锁的信息，尽量少拼接字符串。
     */
    public static void checkDeadLock(EventLoop e, String msg) {
        if (e != null && e.inEventLoop()) {
            throw new BlockingOperationException(msg);
        }
    }

    /**
     * 如果当前线程就是EventLoop线程，则直接执行任务，否则进行提交
     *
     * @param eventLoop 事件循环
     * @param task 任务
     */
    public static void submitOrRun(@Nonnull EventLoop eventLoop, Runnable task) {
        if (eventLoop.inEventLoop()){
            task.run();
        } else {
            eventLoop.execute(task);
        }
    }

    /**
     * 如果当前线程就是EventLoop线程，则直接执行任务，否则进行提交
     *
     * @param eventLoop 事件循环
     * @param task 任务
     * @param exceptionHandler 异常处理器
     */
    public static void submitOrRun(@Nonnull EventLoop eventLoop, AnyRunnable task, ExceptionHandler exceptionHandler) {
        if (eventLoop.inEventLoop()){
            try {
                task.run();
            } catch (Exception e){
                exceptionHandler.handleException(e);
            }
        } else {
            eventLoop.execute(ConcurrentUtils.safeRunnable(task, exceptionHandler));
        }
    }
}
