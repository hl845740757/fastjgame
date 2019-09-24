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

package com.wjybxx.fastjgame.concurrent;

/**
 * 异常处理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/7/13 23:47
 * github - https://github.com/hl845740757
 */
public interface ExceptionHandler {

    /**
     *
     * @param e 对不信任的代码保持怀疑 - 因此使用{@link Throwable}。
     */
    void handleException(Throwable e);
}
