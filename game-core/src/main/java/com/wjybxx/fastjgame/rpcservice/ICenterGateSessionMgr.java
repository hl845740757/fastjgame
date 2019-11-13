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

package com.wjybxx.fastjgame.rpcservice;

import com.wjybxx.fastjgame.annotation.RpcMethod;
import com.wjybxx.fastjgame.annotation.RpcService;
import com.wjybxx.fastjgame.net.session.Session;

/**
 * 网关服在中心服的信息管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/3
 * github - https://github.com/hl845740757
 */
@RpcService(serviceId = RpcServiceTable.CENTER_GATE_SESSION_MGR)
public interface ICenterGateSessionMgr {

    /**
     * 网关服请求注册到中心服上
     *
     * @param session 网关服session
     */
    @RpcMethod(methodId = 1)
    boolean register(Session session);

    /**
     * 同步在线玩家数量
     *
     * @param session         网关服session
     * @param onlinePlayerNum 在线玩家数
     */
    @RpcMethod(methodId = 2)
    void syncOnlinePlayerNum(Session session, int onlinePlayerNum);
}