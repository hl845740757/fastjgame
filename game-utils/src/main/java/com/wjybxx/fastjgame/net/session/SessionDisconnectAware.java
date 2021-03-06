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

package com.wjybxx.fastjgame.net.session;

/**
 * session断开连接事件处理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/10
 * github - https://github.com/hl845740757
 */
public interface SessionDisconnectAware extends SessionLifecycleAware {

    @Override
    default void onSessionConnected(Session session) {
        // ignore
    }

    /**
     * 当会话彻底断开连接(无法继续断线重连)时会被调用，只会调用一次
     *
     * @param session 注册时的会话信息
     */
    void onSessionDisconnected(Session session);
}
