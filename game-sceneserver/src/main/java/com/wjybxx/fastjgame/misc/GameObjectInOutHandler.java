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

package com.wjybxx.fastjgame.misc;

import com.wjybxx.fastjgame.scene.Scene;
import com.wjybxx.fastjgame.scene.gameobject.GameObject;

/**
 * 游戏对象进出场景处理器
 * @author wjybxx
 * @version 1.0
 * @date 2019/6/4 19:35
 * @github - https://github.com/hl845740757
 */
@Stateless
public interface GameObjectInOutHandler<T extends GameObject> {

    /**
     * 执行游戏对象进入场景逻辑
     * @param scene gameObject将要进入的场景
     * @param gameObject 场景对象
     */
    void processEnterScene(Scene scene, T gameObject);

    /**
     * 执行游戏对象离开场景逻辑
     * @param scene gameObject当前所在的场景
     * @param gameObject 场景对象
     */
    void processLeaveScene(Scene scene, T gameObject);

}
