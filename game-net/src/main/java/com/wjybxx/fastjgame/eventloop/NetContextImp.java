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

package com.wjybxx.fastjgame.eventloop;

import com.wjybxx.fastjgame.concurrent.EventLoop;
import com.wjybxx.fastjgame.concurrent.ListenableFuture;
import com.wjybxx.fastjgame.misc.HostAndPort;
import com.wjybxx.fastjgame.misc.HttpPortContext;
import com.wjybxx.fastjgame.misc.PortRange;
import com.wjybxx.fastjgame.net.http.HttpRequestDispatcher;
import com.wjybxx.fastjgame.net.http.HttpServerInitializer;
import com.wjybxx.fastjgame.net.http.OkHttpCallback;
import com.wjybxx.fastjgame.net.local.DefaultLocalPort;
import com.wjybxx.fastjgame.net.local.LocalPort;
import com.wjybxx.fastjgame.net.local.LocalSessionConfig;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.socket.SocketPort;
import com.wjybxx.fastjgame.net.socket.SocketPortContext;
import com.wjybxx.fastjgame.net.socket.SocketSessionConfig;
import com.wjybxx.fastjgame.net.socket.TCPServerChannelInitializer;
import com.wjybxx.fastjgame.net.ws.WsServerChannelInitializer;
import okhttp3.Response;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.net.BindException;
import java.util.Map;

/**
 * NetContext的基本实现
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/8/3
 * github - https://github.com/hl845740757
 */
@ThreadSafe
public class NetContextImp implements NetContext {

    private final long localGuid;
    private final EventLoop localEventLoop;
    private final NetEventLoopGroup netEventLoopGroup;

    NetContextImp(long localGuid, EventLoop localEventLoop, NetEventLoopGroup NetEventLoopGroup) {
        this.localGuid = localGuid;
        this.localEventLoop = localEventLoop;
        this.netEventLoopGroup = NetEventLoopGroup;
    }

    @Override
    public long localGuid() {
        return localGuid;
    }

    @Override
    public EventLoop localEventLoop() {
        return localEventLoop;
    }

    @Override
    public NetEventLoopGroup netEventLoopGroup() {
        return netEventLoopGroup;
    }

    // ----------------------------------------------------- socket 支持 --------------------------------------------------

    @Override
    public SocketPort bindTcpRange(String host, PortRange portRange, @Nonnull SocketSessionConfig config) throws BindException {
        SocketPortContext portExtraInfo = new SocketPortContext(this, config);
        TCPServerChannelInitializer initializer = new TCPServerChannelInitializer(portExtraInfo);
        return netEventLoopGroup.getNettyThreadManager().bindRange(host, portRange, config.sndBuffer(), config.rcvBuffer(), initializer);
    }

    @Override
    public ListenableFuture<Session> connectTcp(String sessionId, long remoteGuid, HostAndPort remoteAddress, @Nonnull SocketSessionConfig config) {
        return netEventLoopGroup.connectTcp(sessionId, remoteGuid, remoteAddress, config, this);
    }

    @Override
    public SocketPort bindWSRange(String host, PortRange portRange, String websocketPath, @Nonnull SocketSessionConfig config) throws BindException {
        SocketPortContext portExtraInfo = new SocketPortContext(this, config);
        WsServerChannelInitializer initializer = new WsServerChannelInitializer(websocketPath, portExtraInfo);
        return netEventLoopGroup.getNettyThreadManager().bindRange(host, portRange, config.sndBuffer(), config.rcvBuffer(), initializer);
    }

    @Override
    public ListenableFuture<Session> connectWS(String sessionId, long remoteGuid, HostAndPort remoteAddress, String websocketUrl, @Nonnull SocketSessionConfig config) {
        return netEventLoopGroup.connectWS(sessionId, remoteGuid, remoteAddress, websocketUrl, config, this);
    }

    // ----------------------------------------------- 本地调用支持 --------------------------------------------

    @Override
    public LocalPort bindLocal(@Nonnull LocalSessionConfig config) {
        return new DefaultLocalPort(this, config);
    }

    @Override
    public ListenableFuture<Session> connectLocal(String sessionId, long remoteGuid, @Nonnull LocalPort localPort, @Nonnull LocalSessionConfig config) {
        return netEventLoopGroup().connectLocal(sessionId, remoteGuid, localPort, config, this);
    }

    // ------------------------------------------------- http 实现 --------------------------------------------

    @Override
    public SocketPort bindHttpRange(String host, PortRange portRange, @Nonnull HttpRequestDispatcher httpRequestDispatcher) throws BindException {
        final HttpPortContext httpPortContext = new HttpPortContext(this, httpRequestDispatcher);
        final HttpServerInitializer initializer = new HttpServerInitializer(httpPortContext);
        return netEventLoopGroup.getNettyThreadManager().bindRange(host, portRange, 8192, 8192, initializer);
    }

    @Override
    public Response syncGet(String url, @Nonnull Map<String, String> params) throws IOException {
        return netEventLoopGroup.getHttpClientManager().syncGet(url, params);
    }

    @Override
    public void asyncGet(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback) {
        netEventLoopGroup.getHttpClientManager().asyncGet(url, params, localEventLoop, okHttpCallback);
    }

    @Override
    public Response syncPost(String url, @Nonnull Map<String, String> params) throws IOException {
        return netEventLoopGroup.getHttpClientManager().syncPost(url, params);
    }

    @Override
    public void asyncPost(String url, @Nonnull Map<String, String> params, @Nonnull OkHttpCallback okHttpCallback) {
        netEventLoopGroup.getHttpClientManager().asyncPost(url, params, localEventLoop, okHttpCallback);
    }
}
