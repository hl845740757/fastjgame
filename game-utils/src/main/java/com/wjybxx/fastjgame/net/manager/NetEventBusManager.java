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

package com.wjybxx.fastjgame.net.manager;

import com.google.inject.Inject;
import com.wjybxx.fastjgame.util.concurrent.EventLoopUtils;
import com.wjybxx.fastjgame.util.eventbus.DynamicChildEvent;
import com.wjybxx.fastjgame.util.eventbus.EventBus;
import com.wjybxx.fastjgame.util.eventbus.EventHandler;
import com.wjybxx.fastjgame.util.eventbus.IdentityEventBus;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/26
 * github - https://github.com/hl845740757
 */
public class NetEventBusManager implements EventBus {

    private final EventBus eventBus = new IdentityEventBus(10);
    private final NetEventLoopManager eventLoopManager;

    @Inject
    public NetEventBusManager(NetEventLoopManager eventLoopManager) {
        this.eventLoopManager = eventLoopManager;
    }

    @Override
    public void post(@Nonnull Object event) {
        eventBus.post(event);
    }

    @Override
    public <T> boolean register(@Nonnull Class<T> eventType, @Nullable String customData, @Nonnull EventHandler<? super T> handler) {
        // 避免在错误的时间调用
        EventLoopUtils.ensureInEventLoop(eventLoopManager.getEventLoop());
        return eventBus.register(eventType, customData, handler);
    }

    @Override
    public <T extends DynamicChildEvent> boolean register(@Nonnull Class<T> parentType, @Nonnull Object childKey, @Nullable String customData, @Nonnull EventHandler<? super T> handler) {
        // 避免在错误的时间调用
        EventLoopUtils.ensureInEventLoop(eventLoopManager.getEventLoop());
        return eventBus.register(parentType, childKey, customData, handler);
    }

    @Override
    public <T> void deregister(@Nonnull Class<T> eventType, @Nonnull EventHandler<? super T> handler) {
        eventBus.deregister(eventType, handler);
    }

    @Override
    public <T extends DynamicChildEvent> void deregister(@Nonnull Class<T> parentType, @Nonnull Object childKey, @Nonnull EventHandler<? super T> handler) {
        eventBus.deregister(parentType, childKey, handler);
    }

    @Override
    public void release() {
        eventBus.release();
    }
}
