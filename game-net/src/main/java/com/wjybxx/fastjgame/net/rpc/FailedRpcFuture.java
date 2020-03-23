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

package com.wjybxx.fastjgame.net.rpc;

import com.wjybxx.fastjgame.utils.concurrent.EventLoop;
import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.timeout.FailedTimeoutFuture;

import javax.annotation.Nonnull;
import java.util.concurrent.Executor;

/**
 * 已完成的Rpc调用，在它上面的任何监听都将立即执行。
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
public class FailedRpcFuture<V> extends FailedTimeoutFuture<V> implements RpcFuture<V> {

    public FailedRpcFuture(@Nonnull EventLoop defaultExecutor, @Nonnull Throwable cause) {
        super(defaultExecutor, cause);
    }

    @Override
    public final boolean isRpcException() {
        return DefaultLocalRpcPromise.isRpcException0(cause());
    }

    @Override
    public final RpcErrorCode getErrorCode() {
        return DefaultLocalRpcPromise.getErrorCode0(cause());
    }

    // 语法支持
    @Override
    public FailedRpcFuture<V> addListener(@Nonnull FutureListener<? super V> listener) {
        super.addListener(listener);
        return this;
    }

    @Override
    public FailedRpcFuture<V> addListener(@Nonnull FutureListener<? super V> listener, @Nonnull Executor bindExecutor) {
        super.addListener(listener, bindExecutor);
        return this;
    }

}