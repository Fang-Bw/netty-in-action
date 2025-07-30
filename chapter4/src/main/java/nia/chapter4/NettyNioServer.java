package nia.chapter4;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * 使用Netty的NIO服务器
 * 
 * 这个类演示了如何使用Netty来实现非阻塞I/O服务器。
 * 与传统的NIO编程相比，Netty提供了巨大的优势：
 * 1. 极大简化了NIO编程的复杂性
 * 2. 隐藏了Selector、SelectionKey等底层细节
 * 3. 提供了统一的编程模型和API
 * 4. 自动处理各种边界条件和异常情况
 * 5. 跨平台的一致性，避免了平台相关的Bug
 * 
 * 与NettyOioServer的对比：
 * - 只需要更改EventLoopGroup（NioEventLoopGroup）
 * - 只需要更改Channel类型（NioServerSocketChannel）
 * - 业务逻辑代码完全相同，体现了Netty API的一致性
 * - 性能更好，可以处理更多并发连接
 * 
 * 代码清单 4.4 使用Netty的异步网络编程
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class NettyNioServer {
    
    /**
     * 启动Netty NIO服务器
     * 
     * 这个方法展示了使用Netty实现NIO服务器的简洁方式。
     * 注意与NettyOioServer的代码几乎完全相同，只是使用了不同的
     * EventLoopGroup和Channel类型，这体现了Netty统一API的强大之处。
     * 
     * Netty NIO的优势：
     * 1. 高性能：基于epoll/kqueue等高效的I/O多路复用机制
     * 2. 低资源消耗：用少量线程处理大量连接
     * 3. 可扩展性好：适合高并发场景
     * 4. 编程简单：相比原生NIO，代码量大幅减少
     * 
     * @param port 服务器监听的端口号
     * @throws Exception 启动过程中可能抛出的异常
     */
    public void server(int port) throws Exception {
        // 创建响应消息的ByteBuf
        // 与OIO版本完全相同，体现了Netty API的一致性
        final ByteBuf buf =
                Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hi!\r\n",
                        Charset.forName("UTF-8")));
        
        // 创建NioEventLoopGroup
        // 这是关键差异：使用NIO实现的EventLoopGroup
        // 内部使用Selector进行事件多路复用，但对开发者透明
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            // 创建ServerBootstrap实例
            ServerBootstrap b = new ServerBootstrap();
            
            // 配置ServerBootstrap
            // 注意与OIO版本的区别只在于EventLoopGroup和Channel类型
            b.group(group).channel(NioServerSocketChannel.class)  // 使用NIO的ServerSocketChannel
                    .localAddress(new InetSocketAddress(port))      // 设置本地绑定地址
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                                      @Override
                                      public void initChannel(SocketChannel ch)
                                              throws Exception {
                                              // 添加处理器到ChannelPipeline
                                              ch.pipeline().addLast(
                                                  // 业务逻辑与OIO版本完全相同
                                                  new ChannelInboundHandlerAdapter() {
                                                      @Override
                                                      public void channelActive(
                                                              ChannelHandlerContext ctx) throws Exception {
                                                                // 连接激活时发送响应并关闭连接
                                                                ctx.writeAndFlush(buf.duplicate())
                                                                  .addListener(
                                                                          ChannelFutureListener.CLOSE);
                                                      }
                                                  });
                                      }
                                  }
                    );
            
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

