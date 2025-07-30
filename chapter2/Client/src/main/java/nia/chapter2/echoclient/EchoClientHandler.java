package nia.chapter2.echoclient;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * Echo客户端处理器
 * 
 * 这个类实现了Echo客户端的核心业务逻辑：
 * 1. 连接建立后发送消息给服务器
 * 2. 接收服务器的回显响应
 * 3. 处理异常情况
 * 
 * 这里使用了SimpleChannelInboundHandler而不是ChannelInboundHandlerAdapter，
 * 主要区别：
 * - SimpleChannelInboundHandler会自动释放接收到的消息
 * - 需要指定消息的类型（这里是ByteBuf）
 * - 使用channelRead0()而不是channelRead()
 * 
 * 代码清单 2.3 客户端的ChannelHandler
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
@Sharable    // 标记这个Handler可以被多个Channel安全地共享
public class EchoClientHandler
    extends SimpleChannelInboundHandler<ByteBuf> {
    
    /**
     * 在到服务器的连接已经建立之后将被调用
     * 
     * 当客户端成功连接到服务器时，这个方法会被调用。
     * 在Echo客户端中，我们在连接建立后立即发送一条消息给服务器。
     * 
     * @param ctx ChannelHandlerContext实例
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 当连接建立时，发送一条消息给服务器
        // Unpooled.copiedBuffer() 创建一个新的ByteBuf，包含指定的字符串数据
        // writeAndFlush() 写入数据并立即刷新，确保数据被发送
        ctx.writeAndFlush(Unpooled.copiedBuffer("Netty rocks!",
                CharsetUtil.UTF_8));
    }

    /**
     * 当从服务器接收到一条消息时被调用
     * 
     * 这个方法是SimpleChannelInboundHandler的核心方法，当接收到指定类型的消息时被调用。
     * 与ChannelInboundHandlerAdapter的channelRead()不同：
     * - 方法名是channelRead0()
     * - 消息类型已经转换为指定的泛型类型（ByteBuf）
     * - 方法执行完毕后，Netty会自动释放ByteBuf资源
     * 
     * @param ctx ChannelHandlerContext实例
     * @param in 接收到的ByteBuf消息
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        // 打印接收到的服务器响应
        System.out.println(
                "Client received: " + in.toString(CharsetUtil.UTF_8));
        
        // 注意：这里不需要手动释放ByteBuf，SimpleChannelInboundHandler会自动处理
    }

    /**
     * 在处理过程中引发异常时被调用
     * 
     * 这是异常处理的回调方法，当客户端处理过程中发生异常时会被调用。
     * 通常的做法是记录异常信息并关闭Channel。
     * 
     * @param ctx ChannelHandlerContext实例
     * @param cause 异常对象
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx,
        Throwable cause) {
        // 打印异常堆栈跟踪
        cause.printStackTrace();
        
        // 关闭该Channel
        ctx.close();
    }
}
