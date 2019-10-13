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

package com.wjybxx.fastjgame.concurrent.adapter;

import com.wjybxx.fastjgame.concurrent.AbstractEventLoop;
import com.wjybxx.fastjgame.concurrent.FailedFuture;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;

import javax.annotation.Nonnull;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 简单的适配一下{@link ExecutorService}
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/13
 * github - https://github.com/hl845740757
 */
public class ExecutorServiceAdapter extends AbstractEventLoop {

    /**
     * 被代理的Executor
     */
    private final ExecutorService executor;

    private final ListenableFuture<?> terminationFuture = new FailedFuture<>(this, new UnsupportedOperationException());

    public ExecutorServiceAdapter(@Nonnull ExecutorService executor) {
        super(null);
        this.executor = executor;
    }

    @Override
    public boolean inEventLoop() {
        // 因为无从推测，因此总是认为不在executor线程中
        return false;
    }

    @Override
    public boolean isShuttingDown() {
        return executor.isShutdown();
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public void shutdown() {
        executor.shutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated();
    }

    @Override
    public ListenableFuture<?> terminationFuture() {
        return terminationFuture;
    }

    @Override
    public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(@Nonnull Runnable command) {
        executor.execute(command);
    }
}
