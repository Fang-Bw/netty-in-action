package nia.chapter11;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.util.concurrent.TimeUnit;

/**
 * 空闲状态处理器初始化器 - 心跳机制实现
 * 
 * 这个类演示了如何使用Netty的IdleStateHandler来实现心跳机制。
 * 心跳机制是网络编程中的重要概念，用于检测连接的存活状态。
 * 
 * 心跳机制的重要性：
 * 1. 连接保活：防止网络设备（如防火墙、路由器）因为长时间无数据传输而关闭连接
 * 2. 故障检测：及时发现网络故障或对端异常
 * 3. 资源清理：清理僵死连接，释放系统资源
 * 4. 负载均衡：帮助负载均衡器识别健康的服务实例
 * 
 * IdleStateHandler的三种空闲检测：
 * 1. readerIdleTime：读空闲超时 - 在指定时间内没有接收到数据
 * 2. writerIdleTime：写空闲超时 - 在指定时间内没有发送数据
 * 3. allIdleTime：读写空闲超时 - 在指定时间内既没有接收也没有发送数据
 * 
 * 工作原理：
 * - IdleStateHandler会定时检查Channel的读写活动
 * - 如果超过设定时间没有活动，会触发IdleStateEvent事件
 * - 应用程序可以通过处理这个事件来实现心跳逻辑
 * 
 * 应用场景：
 * - 长连接的Web应用
 * - 游戏服务器
 * - 实时通信系统
 * - 物联网设备通信
 * 
 * 代码清单 11.7 发送心跳
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class IdleStateHandlerInitializer extends ChannelInitializer<Channel> {
    
    /**
     * 初始化Channel的心跳检测管道
     * 
     * 配置IdleStateHandler和自定义的心跳处理器，
     * 实现基于空闲检测的心跳机制。
     * 
     * @param ch 要初始化的Channel
     * @throws Exception 初始化过程中可能抛出的异常
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        // 添加空闲状态处理器
        // 参数说明：
        // - readerIdleTimeSeconds: 0 表示不检测读空闲
        // - writerIdleTimeSeconds: 0 表示不检测写空闲  
        // - allIdleTimeSeconds: 60 表示读写都空闲60秒时触发事件
        // - TimeUnit.SECONDS: 时间单位为秒
        pipeline.addLast(
                new IdleStateHandler(0, 0, 60, TimeUnit.SECONDS));
        
        // 添加心跳处理器，用于处理空闲状态事件
        pipeline.addLast(new HeartbeatHandler());
    }

    /**
     * 心跳处理器
     * 
     * 这个处理器专门处理IdleStateEvent事件，实现心跳发送逻辑。
     * 当检测到连接空闲时，会自动发送心跳包来保持连接活跃。
     * 
     * 设计特点：
     * - 继承ChannelInboundHandlerAdapter，专注于处理入站事件
     * - 使用静态内部类，避免对外部类的引用
     * - 预定义心跳消息，避免重复创建对象
     * - 发送失败时自动关闭连接
     */
    public static final class HeartbeatHandler extends ChannelInboundHandlerAdapter {
        
        // 预定义的心跳消息
        // 使用unreleasableBuffer确保消息不会被意外释放
        // copiedBuffer创建包含指定内容的ByteBuf
        private static final ByteBuf HEARTBEAT_SEQUENCE =
                Unpooled.unreleasableBuffer(Unpooled.copiedBuffer(
                "HEARTBEAT", CharsetUtil.ISO_8859_1));
        
        /**
         * 处理用户事件
         * 
         * 这个方法会接收各种用户事件，包括IdleStateEvent。
         * 当接收到空闲状态事件时，发送心跳包来保持连接活跃。
         * 
         * 心跳发送逻辑：
         * 1. 检查事件类型是否为IdleStateEvent
         * 2. 如果是，发送预定义的心跳消息
         * 3. 使用duplicate()创建消息副本，避免重复读取
         * 4. 添加失败监听器，发送失败时关闭连接
         * 
         * @param ctx ChannelHandlerContext实例
         * @param evt 用户事件对象
         * @throws Exception 处理过程中可能抛出的异常
         */
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx,
            Object evt) throws Exception {
            
            // 检查是否为空闲状态事件
            if (evt instanceof IdleStateEvent) {
                // 发送心跳消息
                // duplicate()创建共享内容但独立读写指针的副本
                // 这样可以重复使用同一个心跳消息而不会相互干扰
                ctx.writeAndFlush(HEARTBEAT_SEQUENCE.duplicate())
                     .addListener(
                         // 如果心跳发送失败，关闭连接
                         // 这通常意味着网络连接已经断开
                         ChannelFutureListener.CLOSE_ON_FAILURE);
                
                // 心跳发送成功的处理（可选）：
                // - 记录心跳发送日志
                // - 更新连接状态统计
                // - 通知监控系统
                
            } else {
                // 如果不是空闲状态事件，传递给下一个处理器
                super.userEventTriggered(ctx, evt);
            }
        }
        
        // 扩展功能示例：
        
        /**
         * 处理心跳响应（示例扩展）
         * 
         * 在实际应用中，可能需要处理对端返回的心跳响应，
         * 以确认连接的双向可达性。
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 检查是否为心跳响应消息
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                if (buf.toString(CharsetUtil.ISO_8859_1).equals("HEARTBEAT_RESPONSE")) {
                    // 收到心跳响应，更新连接状态
                    // logger.debug("Received heartbeat response");
                    buf.release(); // 释放资源
                    return;
                }
            }
            
            // 如果不是心跳响应，传递给下一个处理器
            super.channelRead(ctx, msg);
        }
    }
    
    // 使用建议：
    // 1. 心跳间隔应该根据网络环境和应用需求来设定
    // 2. 通常设置为网络超时时间的1/3到1/2
    // 3. 考虑实现双向心跳：客户端和服务器都发送心跳
    // 4. 在高并发环境下，注意心跳的性能影响
    // 5. 可以结合应用层心跳和TCP层的Keep-Alive机制
}
