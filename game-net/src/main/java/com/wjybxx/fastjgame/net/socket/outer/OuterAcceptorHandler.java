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

package com.wjybxx.fastjgame.net.socket.outer;

import com.wjybxx.fastjgame.manager.AcceptorManager;
import com.wjybxx.fastjgame.manager.NetManagerWrapper;
import com.wjybxx.fastjgame.net.common.*;
import com.wjybxx.fastjgame.net.session.Session;
import com.wjybxx.fastjgame.net.session.SessionDuplexHandlerAdapter;
import com.wjybxx.fastjgame.net.session.SessionHandlerContext;
import com.wjybxx.fastjgame.net.socket.*;
import com.wjybxx.fastjgame.utils.ConcurrentUtils;
import com.wjybxx.fastjgame.utils.NetUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * session服务端方维持socket使用的handler
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/10/16
 * github - https://github.com/hl845740757
 */
public class OuterAcceptorHandler extends SessionDuplexHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(OuterAcceptorHandler.class);

    private int maxPendingMessages;
    private int maxCacheMessages;
    /**
     * 会话channel一定不为null
     */
    private Channel channel;
    /**
     * 消息队列
     */
    private final MessageQueue messageQueue = new MessageQueue();
    /**
     * 最后一次成功建立连接时对应的请求信息 - 客户端上一次的请求信息
     */
    private SocketConnectRequest connectRequest;
    /**
     * 是否通知对方关闭 - 主动关闭时通知
     */
    private boolean notify = true;

    private OuterAcceptorHandler(Channel channel, SocketConnectRequest connectRequest) {
        this.channel = channel;
        this.connectRequest = connectRequest;
    }

    @Override
    public void handlerAdded(SessionHandlerContext ctx) throws Exception {
        // 缓存，减少堆栈深度
        final SocketSessionConfig config = (SocketSessionConfig) ctx.session().config();
        maxPendingMessages = config.maxPendingMessages();
        maxCacheMessages = config.maxCacheMessages();
    }

    @Override
    public void onSessionActive(SessionHandlerContext ctx) throws Exception {
        ConcurrentUtils.safeExecute(ctx.localEventLoop(), new ConnectAwareTask(ctx.session()));
        ctx.fireSessionActive();
    }

    @Override
    public void onSessionInactive(SessionHandlerContext ctx) throws Exception {
        ConcurrentUtils.safeExecute(ctx.localEventLoop(), new DisconnectAwareTask(ctx.session()));
        ctx.fireSessionInactive();
    }

    @Override
    public void tick(SessionHandlerContext ctx) throws Exception {
        // 继续发送消息，服务端不处理超时，因此ackDeadline赋0即可
        OuterUtils.emit(channel, messageQueue, maxPendingMessages, 0);
    }

    @Override
    public void read(SessionHandlerContext ctx, Object msg) {
        final SocketEvent event = (SocketEvent) msg;
        if (ctx.session().isClosed()) {
            // session已关闭
            NetUtils.closeQuietly(event.channel());
            return;
        }

        if (event.channel() != channel) {
            // 这里不包含建立连接请求，因此channel必须相等
            NetUtils.closeQuietly(event.channel());
            return;
        }

        if (event instanceof SocketMessageEvent) {
            // 对方发来的消息事件 - 它出现的概率更高，因此放在socket断开之前处理
            OuterUtils.readMessage(ctx, (SocketMessageEvent) event, messageQueue, channel, maxPendingMessages, 0);
            return;
        }

        // 走到这应该是socket断开连接事件，不处理，等待客户端重连
        assert event instanceof SocketChannelInactiveEvent;
    }

    @Override
    public void write(SessionHandlerContext ctx, Object msg) throws Exception {
        OuterUtils.write(ctx.session(), channel, messageQueue, (NetMessage) msg, maxPendingMessages, maxCacheMessages, 0);
    }

    @Override
    public void flush(SessionHandlerContext ctx) throws Exception {
        OuterUtils.flush(ctx.session(), channel, messageQueue);
    }

    @Override
    public void close(SessionHandlerContext ctx) throws Exception {
        if (notify) {
            // 通知对方关闭，同时会关闭channel
            notifyClientExit(channel, connectRequest, messageQueue);
        }

        if (logger.isDebugEnabled()) {
            // 打印关闭原因
            logger.debug("close stacktrace {} ", ExceptionUtils.getStackTrace(new RuntimeException()));
        }
    }

    /**
     * 接收到一个重连请求
     */
    private void onRcvReconnectRequest(SessionHandlerContext ctx, SocketConnectRequestEvent event) {
        final SocketConnectRequest connectRequest = event.getConnectRequest();
        SocketSessionImp session = (SocketSessionImp) ctx.session();
        if (session.isClosed()) {
            // session 已关闭
            notifyConnectFail(event.channel(), connectRequest);
            return;
        }

        // verifyingTimes一定是增加的
        if (connectRequest.getVerifyingTimes() <= this.connectRequest.getVerifyingTimes()) {
            // 这是一个旧请求
            notifyConnectFail(event.channel(), connectRequest);
            return;
        }

        // verifiedTimes要么和上次相同，要么+1
        if (connectRequest.getVerifiedTimes() != this.connectRequest.getVerifiedTimes()
                && connectRequest.getVerifiedTimes() != this.connectRequest.getVerifiedTimes() + 1) {
            // 这是一个旧请求
            notifyConnectFail(event.channel(), connectRequest);
            return;
        }

        if (event.isClose()) {
            // 如果这是一个关闭session请求，则关闭session
            notify = false;
            ctx.session().close();
            return;
        }

        if (!messageQueue.isAckOK(event.getAck())) {
            // ack错误 - 无法重连（消息彻底丢失）
            notifyConnectFail(event.channel(), connectRequest);
            return;
        }

        // -- 验证完成
        if (event.channel() != channel) {
            // 如果是新的socket，则需要关闭旧的连接
            NetUtils.closeQuietly(channel);
        }

        // 更新状态
        channel = event.channel();
        messageQueue.updatePendingQueue(event.getAck());
        this.connectRequest = connectRequest;

        // 重连日志
        logger.info("{} reconnect success, verifyingTimes = {}", session.sessionId(), connectRequest.getVerifyingTimes());

        // 通知对方重连成功
        notifyConnectSuccess(channel, connectRequest, messageQueue);

        // 服务器重连成功要干两件事
        // 1. 触发一次读，避免session超时 - 使用instance2不需要返回包
        ctx.fireRead(PingPongMessage.INSTANCE2);
        // 2. 重发消息
        OuterUtils.resend(channel, messageQueue, 0);
    }

    // ------------------------------------------------------- 建立连接请求 ----------------------------------------------

    /**
     * 接收到一个外网建立连接请求 - 开启消息确认机制的请求
     */
    public static void onRcvConnectRequest(SocketConnectRequestEvent event, NetManagerWrapper netManagerWrapper, AcceptorManager acceptorManager) {
        final Channel channel = event.channel();
        final SocketConnectRequest connectRequest = event.getConnectRequest();
        final Session existSession = acceptorManager.getSession(event.sessionId());
        if (existSession == null) {

            if (event.isClose()) {
                // 如果这是一个关闭session请求，则不建立新连接
                return;
            }

            // 尝试建立一个新的session
            if (connectRequest.getVerifiedTimes() != 0) {
                // 这是旧请求，等于0才是首次建立连接请求 - 大于0表示期望重连
                notifyConnectFail(channel, connectRequest);
                return;
            }
            if (event.getAck() != 0) {
                // 建立新连接时ack应该为0，需要根据响应初始化ack
                notifyConnectFail(channel, connectRequest);
                return;
            }

            // -- 建立连接成功
            final SocketPortContext portExtraInfo = event.getPortExtraInfo();
            final SocketSessionImp session = new SocketSessionImp(portExtraInfo.getNetContext(),
                    event.sessionId(),
                    event.remoteGuid(),
                    portExtraInfo.getSessionConfig(),
                    netManagerWrapper,
                    acceptorManager);

            // 初始ack为客户端的初始sequence+1
            final OuterAcceptorHandler acceptorHandler = new OuterAcceptorHandler(channel, connectRequest);
            acceptorHandler.messageQueue.setAck(event.getInitSequence() + 1);

            // 初始化管道
            session.pipeline()
                    .addLast(acceptorHandler)
                    .addLast(new PongSupportHandler())
                    .addLast(new OneWaySupportHandler());

            // 判断是否支持rpc
            if (session.config().isRpcAvailable()) {
                session.pipeline().addLast(new RpcSupportHandler());
            }

            // 激活session并传递激活事件
            session.tryActive();
            session.pipeline().fireSessionActive();

            // 通知对方建立session成功
            notifyConnectSuccess(channel, connectRequest, acceptorHandler.messageQueue);
        } else {
            // 尝试重连
            final SessionHandlerContext firstContext = existSession.pipeline().firstContext();
            if (firstContext == null) {
                // 错误的请求
                notifyConnectFail(channel, connectRequest);
                return;
            }
            if (firstContext.handler() instanceof OuterAcceptorHandler) {
                // 自己处理重连请求
                ((OuterAcceptorHandler) firstContext.handler()).onRcvReconnectRequest(firstContext, event);
            } else {
                // 错误的请求
                notifyConnectFail(channel, connectRequest);
            }
        }
    }

    /**
     * 建立连接成功 - 告知对方成功
     */
    private static void notifyConnectSuccess(Channel channel, SocketConnectRequest connectRequest, MessageQueue messageQueue) {
        final SocketConnectResponse connectResponse = new SocketConnectResponse(true, connectRequest);
        final OuterSocketConnectResponseTO connectResponseTO = new OuterSocketConnectResponseTO(messageQueue.getInitSequence(), messageQueue.getAck(),
                false, connectResponse);
        // 告知重连成功
        channel.writeAndFlush(connectResponseTO);
    }

    /**
     * 建立连接失败 - 告知对方失败
     */
    private static void notifyConnectFail(Channel channel, SocketConnectRequest connectRequest) {
        final SocketConnectResponse connectResponse = new SocketConnectResponse(false, connectRequest);
        final OuterSocketConnectResponseTO connectResponseTO = new OuterSocketConnectResponseTO(-1, -1, false, connectResponse);
        // 告知验证失败
        channel.writeAndFlush(connectResponseTO);
    }

    /**
     * 通知客户端退出 - 完成之后关闭channnel
     */
    private static void notifyClientExit(Channel channel, SocketConnectRequest connectRequest, MessageQueue messageQueue) {
        final SocketConnectResponse connectResponse = new SocketConnectResponse(true, connectRequest);
        final OuterSocketConnectResponseTO connectResponseTO = new OuterSocketConnectResponseTO(messageQueue.getInitSequence(), messageQueue.getAck(),
                true, connectResponse);

        channel.writeAndFlush(connectResponseTO)
                .addListener(ChannelFutureListener.CLOSE);
    }
}
