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

package com.wjybxx.fastjgame.utils.concurrent.timeout;

import com.wjybxx.fastjgame.utils.concurrent.FutureListener;
import com.wjybxx.fastjgame.utils.concurrent.NFuture;

/**
 * 超时future结果监听
 *
 * @author wjybxx
 * @version 1.0
 * date - 2020/3/7
 * github - https://github.com/hl845740757
 */
@FunctionalInterface
public interface TimeoutFutureListener<V> extends FutureListener<V> {

    @Override
    default void onComplete(NFuture<V> future) throws Exception {
        final TimeoutFuture<V> timeoutFuture = (TimeoutFuture<V>) future;
        if (timeoutFuture.isTimeout()) {
            onTimeout(timeoutFuture);
        }
    }

    /**
     * future关联的操作超时
     */
    void onTimeout(TimeoutFuture<V> future) throws Exception;

}
