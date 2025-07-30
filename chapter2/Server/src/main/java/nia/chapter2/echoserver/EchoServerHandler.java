package nia.chapter2.echoserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;

/**
 * Echo服务器处理器
 * 
 * 这个类实现了Echo服务器的核心业务逻辑：接收客户端数据并原样返回。
 * 它继承自ChannelInboundHandlerAdapter，这是一个适配器类，提供了
 * ChannelInboundHandler接口的默认实现。
 * 
 * Echo服务器的处理流程：
 * 1. channelRead() - 接收数据时被调用
 * 2. channelReadComplete() - 数据读取完成时被调用  
 * 3. exceptionCaught() - 处理过程中发生异常时被调用
 * 
 * 代码清单 2.1 EchoServerHandler
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
@Sharable    // 标记这个Handler可以被多个Channel安全地共享
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    
    /**
     * 对于每个传入的消息都要调用这个方法
     * 
     * 当从客户端接收到数据时，这个方法会被Netty调用。
     * 在Echo服务器中，我们只是简单地将接收到的数据写回给客户端。
     * 
     * 重要概念：
     * - 这里的写操作是异步的，数据被写入到ChannelOutboundBuffer中
     * - 实际的网络写入会在稍后进行
     * - 调用write()不会立即发送数据，需要调用flush()才会真正发送
     * 
     * @param ctx ChannelHandlerContext实例，提供了各种操作方法
     * @param msg 接收到的消息对象，这里是ByteBuf类型
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 将消息强制转换为ByteBuf
        ByteBuf in = (ByteBuf) msg;
        
        // 打印接收到的消息内容（以UTF-8编码）
        System.out.println(
                "Server received: " + in.toString(CharsetUtil.UTF_8));
        
        // 将接收到的消息写回给发送者
        // 注意：这里没有释放资源，因为Netty会在写操作完成后自动释放ByteBuf
        ctx.write(in);
    }

    /**
     * 通知Handler最后一次对channelRead()的调用是当前批量读取中的最后一条消息
     * 
     * 当Channel上的读操作完成时，这个方法会被调用。
     * 在这里我们需要将之前写入的数据刷新到远程节点，并关闭Channel。
     * 
     * @param ctx ChannelHandlerContext实例
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
            throws Exception {
        // 将未决消息冲刷到远程节点，并且关闭该Channel
        // Unpooled.EMPTY_BUFFER 是一个空的ByteBuf
        // ChannelFutureListener.CLOSE 是一个预定义的监听器，用于在写操作完成后关闭Channel
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * 在读取操作期间，有异常抛出时会调用这个方法
     * 
     * 这是异常处理的回调方法，当处理过程中发生异常时会被调用。
     * 一般的做法是记录异常信息并关闭Channel。
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
