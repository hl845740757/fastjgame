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

package com.wjybxx.fastjgame.module;

import com.google.inject.Singleton;
import com.wjybxx.fastjgame.mgr.*;
import com.wjybxx.fastjgame.world.GateWorld;
import com.wjybxx.fastjgame.world.GateWorldInfoMgr;
import com.wjybxx.fastjgame.world.World;

/**
 * 网关服务器使用的管理器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/11/1
 * github - https://github.com/hl845740757
 */
public class GateModule extends WorldModule {

    @Override
    protected void bindWorldAndWorldInfoMgr() {
        bind(World.class).to(GateWorld.class).in(Singleton.class);
        bind(WorldInfoMgr.class).to(GateWorldInfoMgr.class).in(Singleton.class);
        bind(ProtocolDispatcherMgr.class).in(Singleton.class);
    }

    @Override
    protected void bindOthers() {
        bind(GateWorldInfoMgr.class).in(Singleton.class);
        bind(GateDiscoverMgr.class).in(Singleton.class);
        bind(CenterInGateInfoMgr.class).in(Singleton.class);
        bind(SceneInGateInfoMgr.class).in(Singleton.class);

    }
}
