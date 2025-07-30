package nia.chapter12;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

/**
 * WebSocket文本帧处理器
 * 
 * 这个处理器专门处理WebSocket连接中的文本消息，是聊天功能的核心组件。
 * 它负责管理客户端连接的生命周期和实现消息的实时广播。
 * 
 * 主要功能：
 * 1. 监听WebSocket握手完成事件
 * 2. 管理客户端连接的加入和退出
 * 3. 实现消息的实时广播
 * 4. 动态调整ChannelPipeline结构
 * 
 * 工作原理：
 * - 当WebSocket握手完成时，将连接添加到ChannelGroup
 * - 接收到文本消息时，广播给所有连接的客户端
 * - 使用引用计数确保消息资源正确管理
 * 
 * 技术特点：
 * - 继承SimpleChannelInboundHandler，自动处理资源释放
 * - 使用ChannelGroup实现高效的群组消息广播
 * - 基于事件驱动的连接管理
 * 
 * 代码清单 12.2 处理文本帧
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class TextWebSocketFrameHandler
    extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    
    // ChannelGroup用于管理所有的WebSocket连接
    private final ChannelGroup group;

    /**
     * 构造函数
     * 
     * @param group ChannelGroup实例，用于管理所有WebSocket连接
     */
    public TextWebSocketFrameHandler(ChannelGroup group) {
        this.group = group;
    }

    /**
     * 处理用户事件
     * 
     * 这个方法处理特殊的用户事件，特别是WebSocket握手完成事件。
     * 当WebSocket握手成功完成后，需要进行以下操作：
     * 1. 清理Pipeline中不再需要的HTTP处理器
     * 2. 通知其他客户端有新用户加入
     * 3. 将新连接添加到ChannelGroup中
     * 
     * 事件驱动的设计使得连接管理变得简单和可靠。
     * 
     * @param ctx ChannelHandlerContext实例
     * @param evt 触发的事件对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,
        Object evt) throws Exception {
        
        // 检查是否是WebSocket握手完成事件
        if (evt == WebSocketServerProtocolHandler
             .ServerHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            
            // 握手完成后，从Pipeline中移除HttpRequestHandler
            // 因为此连接已经升级为WebSocket，不再需要处理HTTP请求
            ctx.pipeline().remove(HttpRequestHandler.class);
            
            // 通知所有已连接的客户端有新用户加入
            // 创建一个包含加入消息的WebSocket文本帧
            // writeAndFlush()会将消息发送给ChannelGroup中的所有Channel
            group.writeAndFlush(new TextWebSocketFrame(
                    "Client " + ctx.channel() + " joined"));
            
            // 将新连接的Channel添加到ChannelGroup中
            // 这样后续的消息广播会包含这个新连接
            group.add(ctx.channel());
        } else {
            // 如果不是握手完成事件，传递给下一个处理器
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 处理接收到的WebSocket文本帧
     * 
     * 这个方法处理客户端发送的文本消息，实现聊天室的核心功能。
     * 当接收到消息时，将其广播给所有连接的客户端。
     * 
     * 消息广播机制：
     * 1. 接收来自某个客户端的消息
     * 2. 使用ChannelGroup将消息转发给所有连接的客户端
     * 3. 使用retain()增加消息的引用计数，确保多次写入安全
     * 
     * 重要提醒：
     * - retain()用于增加引用计数，因为消息会被写入多个Channel
     * - SimpleChannelInboundHandler会在方法结束后自动释放原始消息
     * - ChannelGroup.writeAndFlush()是异步操作，会立即返回
     * 
     * @param ctx ChannelHandlerContext实例
     * @param msg 接收到的WebSocket文本帧
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx,
        TextWebSocketFrame msg) throws Exception {
        
        // 将接收到的消息广播给所有连接的客户端
        // retain()增加消息的引用计数，因为消息将被写入多个Channel
        // ChannelGroup会自动将消息发送给组内所有的Channel
        group.writeAndFlush(msg.retain());
        
        // 注意：
        // 1. 这里使用了msg.retain()来增加引用计数
        // 2. 因为ChannelGroup会将消息写入多个Channel，每个写入都需要一个引用
        // 3. SimpleChannelInboundHandler会在方法结束后自动release()原始消息
        // 4. 这种设计确保了消息资源的正确管理
    }
}
