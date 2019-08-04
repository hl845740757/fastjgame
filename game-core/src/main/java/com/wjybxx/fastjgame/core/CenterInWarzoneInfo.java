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
package com.wjybxx.fastjgame.core;

import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.net.Session;

/**
 * CenterServer在WarzoneServer信息
 * @author wjybxx
 * @version 1.0
 * date - 2019/5/15 14:06
 * github - https://github.com/hl845740757
 */
public class CenterInWarzoneInfo {

    private final long gameWorldGuid;

    private final PlatformType platformType;

    private final int serverId;

    private final Session session;

    public CenterInWarzoneInfo(long gameWorldGuid, PlatformType platformType, int serverId, Session session) {
        this.gameWorldGuid = gameWorldGuid;
        this.platformType = platformType;
        this.serverId = serverId;
        this.session = session;
    }

    public long getGameWorldGuid() {
        return gameWorldGuid;
    }

    public PlatformType getPlatformType() {
        return platformType;
    }

    public int getServerId() {
        return serverId;
    }

    public Session getSession() {
        return session;
    }

}
