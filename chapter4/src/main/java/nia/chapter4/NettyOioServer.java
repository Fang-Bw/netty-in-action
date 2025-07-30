package nia.chapter4;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * 使用Netty的阻塞I/O服务器
 * 
 * 这个类演示了如何使用Netty来实现阻塞I/O服务器。
 * 与传统的阻塞I/O相比，Netty提供了以下优势：
 * 1. 统一的API：无论是阻塞还是非阻塞I/O，API保持一致
 * 2. 简化的编程模型：使用ChannelHandler处理业务逻辑
 * 3. 更好的资源管理：自动处理ByteBuf的创建和释放
 * 4. 统一的异常处理机制
 * 5. 更容易进行单元测试
 * 
 * 重要特性：
 * - 使用OioEventLoopGroup提供阻塞I/O实现
 * - 使用OioServerSocketChannel作为阻塞服务器Channel
 * - 业务逻辑通过ChannelHandler实现，代码更清晰
 * - 可以方便地切换到NIO实现，只需更改EventLoopGroup和Channel类型
 * 
 * 代码清单 4.3 使用Netty的阻塞网络编程
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class NettyOioServer {
    
    /**
     * 启动Netty OIO服务器
     * 
     * 这个方法展示了使用Netty实现阻塞I/O服务器的简洁方式：
     * 1. 创建响应数据的ByteBuf
     * 2. 创建OioEventLoopGroup处理I/O事件
     * 3. 配置ServerBootstrap
     * 4. 使用ChannelInitializer设置处理器
     * 5. 绑定端口并启动服务器
     * 
     * @param port 服务器监听的端口号
     * @throws Exception 启动过程中可能抛出的异常
     */
    public void server(int port)
            throws Exception {
        // 创建响应消息的ByteBuf
        // unreleasableBuffer()创建一个不会被释放的ByteBuf，可以安全地被多次重用
        final ByteBuf buf =
                Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hi!\r\n", Charset.forName("UTF-8")));
        
        // 创建OioEventLoopGroup
        // 这个EventLoopGroup提供阻塞I/O的实现，内部仍然使用线程池
        EventLoopGroup group = new OioEventLoopGroup();
        try {
            // 创建ServerBootstrap实例
            ServerBootstrap b = new ServerBootstrap();
            
            // 配置ServerBootstrap
            b.group(group)                                    // 设置EventLoopGroup
                    .channel(OioServerSocketChannel.class)   // 使用阻塞I/O的ServerSocketChannel
                    .localAddress(new InetSocketAddress(port)) // 设置本地绑定地址
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 设置子Channel的初始化器
                        @Override
                        public void initChannel(SocketChannel ch)
                                throws Exception {
                                // 为每个新连接的ChannelPipeline添加处理器
                                ch.pipeline().addLast(
                                    // 创建匿名的ChannelInboundHandlerAdapter
                                    new ChannelInboundHandlerAdapter() {
                                        @Override
                                        public void channelActive(
                                                ChannelHandlerContext ctx)
                                                throws Exception {
                                            // 当连接激活时，发送响应消息并关闭连接
                                            // duplicate()创建ByteBuf的副本，避免并发问题
                                            ctx.writeAndFlush(buf.duplicate())
                                                    .addListener(
                                                            ChannelFutureListener.CLOSE);
                                        }
                                    });
                        }
                    });
            
            // 绑定服务器并同步等待绑定完成
            ChannelFuture f = b.bind().sync();
            
            // 等待服务器Channel关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅关闭EventLoopGroup
            group.shutdownGracefully().sync();
        }
    }
}

