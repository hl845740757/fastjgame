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

package com.wjybxx.fastjgame.start;

import com.wjybxx.fastjgame.concurrent.DefaultThreadFactory;
import com.wjybxx.fastjgame.concurrent.RejectedExecutionHandlers;
import com.wjybxx.fastjgame.configwrapper.ArrayConfigWrapper;
import com.wjybxx.fastjgame.core.SceneRegion;
import com.wjybxx.fastjgame.core.SceneWorldType;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroup;
import com.wjybxx.fastjgame.eventloop.NetEventLoopGroupImp;
import com.wjybxx.fastjgame.misc.PlatformType;
import com.wjybxx.fastjgame.module.*;
import com.wjybxx.fastjgame.utils.TimeUtils;
import com.wjybxx.fastjgame.world.GameEventLoopGroup;
import com.wjybxx.fastjgame.world.GameEventLoopGroupImp;
import com.wjybxx.fastjgame.world.World;

import java.io.File;

/**
 * 启动器
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/5
 * github - https://github.com/hl845740757
 */
public class StartUp {

    /** 战区服参数 */
    private static final String[] warzoneArgs = new String[] {
            "warzoneId="+ 1
    };

    /** 中心服参数 */
    private static final String[] centerArgs = new String[] {
            "platform="+ PlatformType.TEST.name(),
            "serverId="+ 1
    };

    /** 本服scene参数 */
    private static final String[] singleSceneArgs = new String[] {
            "sceneType="+SceneWorldType.SINGLE.name(),
            "platform="+ PlatformType.TEST.name(),
            "serverId="+ 1,
            "configuredRegions="+ SceneRegion.LOCAL_PKC.name() + "|" + SceneRegion.LOCAL_NORMAL.name()
    };

    /** 跨服scene参数 */
    private static final String[] crossSceneArgs = new String[] {
            "sceneType="+SceneWorldType.CROSS.name(),
            "warzoneId="+ 1,
            "configuredRegions="+ SceneRegion.WARZONE_ANTON.name() + "|" + SceneRegion.WARZONE_LUKE.name()
    };

    /** 登录服参数 */
    private static final String[] loginArgs = new String[] {
            "port=" + 12345
    };


    public static void main(String[] args) throws Exception {
        // 指定一下日志文件
        String logDir=new File("").getAbsolutePath() + File.separator + "log";
        String logPath = logDir + File.separator + "fastjgame.log";
        System.out.println("logPath " + logPath);
        System.setProperty("logPath", logPath);

        // 试一试ALL IN ONE
        // NET线程数最少1个
        final NetEventLoopGroup netEventLoopGroup = new NetEventLoopGroupImp(2, new DefaultThreadFactory("NET"), RejectedExecutionHandlers.log());
        // Game线程数需要多一点，因为目前部分启动实现是阻塞方式的，zookeeper节点不存在/存在的情况下回阻塞，后期会改动
        // center warzone 启动可能阻塞(同一个战区只能启动一个)
        final GameEventLoopGroup gameEventLoopGroup = new GameEventLoopGroupImp(3, new DefaultThreadFactory("WORLD"), RejectedExecutionHandlers.log(), netEventLoopGroup);

        start(gameEventLoopGroup, new WarzoneModule(), warzoneArgs, 10);
        start(gameEventLoopGroup, new CenterModule(), centerArgs, 10);

        start(gameEventLoopGroup, new SceneModule(), singleSceneArgs, 20);

        start(gameEventLoopGroup, new SceneModule(), crossSceneArgs, 20);
        start(gameEventLoopGroup, new SceneModule(), crossSceneArgs, 20);
        start(gameEventLoopGroup, new SceneModule(), crossSceneArgs, 20);
        start(gameEventLoopGroup, new SceneModule(), crossSceneArgs, 20);
        start(gameEventLoopGroup, new SceneModule(), crossSceneArgs, 20);

        start(gameEventLoopGroup, new LoginModule(), loginArgs, 10);


        try {
            Thread.sleep(2 * TimeUtils.MIN);
        } catch (InterruptedException ignore){
        }
        // 试一试能否安全关闭
        gameEventLoopGroup.shutdown();
        netEventLoopGroup.shutdown();
        System.out.println(" ******* invoked shutdown *******");
    }

    private static World start(GameEventLoopGroup gameEventLoopGroup, WorldModule module, String[] args, int framesPerSecond) throws Exception {
        return gameEventLoopGroup.registerWorld(module, new ArrayConfigWrapper(args), framesPerSecond).get();
    }
}