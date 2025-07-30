package nia.chapter1;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 连接处理器示例
 * 
 * 这个类演示了Netty中ChannelHandler的回调机制。
 * ChannelHandler是Netty中处理I/O事件的核心组件，它定义了
 * 对各种I/O事件的响应逻辑。
 * 
 * ChannelInboundHandlerAdapter是一个适配器类，提供了
 * ChannelInboundHandler接口的默认实现，我们可以选择性地
 * 重写需要的方法。
 * 
 * 创建者：kerr
 * 
 * 代码清单 1.2 由回调触发的ChannelHandler
 */
public class ConnectHandler extends ChannelInboundHandlerAdapter {
    
    /**
     * 当新的连接被建立时调用此方法
     * 
     * channelActive()是ChannelInboundHandler中的一个重要回调方法，
     * 当Channel变为活跃状态时被触发。对于服务器端，这意味着有新的
     * 客户端连接建立；对于客户端，这意味着连接到服务器成功。
     * 
     * 这种基于回调的事件处理机制是Netty异步编程模型的核心特征：
     * - 事件驱动：不同的网络事件触发不同的回调方法
     * - 非阻塞：回调方法的执行不会阻塞I/O线程
     * - 链式处理：多个Handler可以组成处理链
     * 
     * @param ctx ChannelHandlerContext提供了操作Channel和处理事件的方法
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx)
            throws Exception {
        // 打印客户端连接信息
        // ctx.channel().remoteAddress()获取远程客户端的地址
        System.out.println(
                "Client " + ctx.channel().remoteAddress() + " connected");
    }
}