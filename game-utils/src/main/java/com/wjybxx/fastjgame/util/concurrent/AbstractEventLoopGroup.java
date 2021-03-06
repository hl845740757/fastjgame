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

package com.wjybxx.fastjgame.util.concurrent;


import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * {@link EventLoopGroup }的抽象实现，主要实现任务分配。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/15
 * github - https://github.com/hl845740757
 */
public abstract class AbstractEventLoopGroup implements EventLoopGroup {

    // ----------------------------------- 主要是为了支持 execute和submit -------------------

    /**
     * 将任务分配到某一个子节点上
     *
     * @param command 任务
     */
    @Override
    public final void execute(@Nonnull Runnable command) {
        next().execute(command);
    }

    @Nonnull
    @Override
    public final FluentFuture<?> submit(@Nonnull Runnable task) {
        return next().submit(task);
    }

    @Nonnull
    @Override
    public final <T> FluentFuture<T> submit(@Nonnull Runnable task, T result) {
        return next().submit(task, result);
    }

    @Nonnull
    @Override
    public final <T> FluentFuture<T> submit(@Nonnull Callable<T> task) {
        return next().submit(task);
    }

    // ---------------------------------- 不是很想支持的方法 ------------------------------------
    // 仅仅是简单的将任务分配给某一个线程

    @Nonnull
    @Override
    public final <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return next().invokeAll(tasks);
    }

    @Nonnull
    @Override
    public final <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit)
            throws InterruptedException {
        return next().invokeAll(tasks, timeout, unit);
    }

    @Nonnull
    @Override
    public final <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        return next().invokeAny(tasks);
    }

    @Override
    public final <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return next().invokeAny(tasks, timeout, unit);
    }
}