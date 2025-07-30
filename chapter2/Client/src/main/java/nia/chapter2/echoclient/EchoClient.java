package nia.chapter2.echoclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * Echo客户端主类
 * 
 * 这个类演示了Netty客户端的基本架构和启动流程。
 * Echo客户端会连接到Echo服务器，发送一条消息，接收服务器的回显响应，
 * 然后断开连接。
 * 
 * Netty客户端与服务器端的主要区别：
 * 1. 使用Bootstrap而不是ServerBootstrap
 * 2. 使用NioSocketChannel而不是NioServerSocketChannel
 * 3. 不需要childHandler，直接使用handler
 * 4. 使用connect()而不是bind()
 * 
 * 代码清单 2.4 客户端的主类
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class EchoClient {
    // 服务器主机地址
    private final String host;
    // 服务器端口号
    private final int port;

    /**
     * 构造函数，初始化服务器地址和端口
     * 
     * @param host 服务器主机地址
     * @param port 服务器端口号
     */
    public EchoClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 启动客户端
     * 
     * 这个方法展示了Netty客户端的典型启动流程：
     * 1. 创建EventLoopGroup来处理I/O操作
     * 2. 使用Bootstrap配置客户端参数
     * 3. 设置Channel类型和处理器
     * 4. 连接到服务器
     * 5. 等待连接关闭
     * 6. 优雅关闭资源
     * 
     * @throws Exception 启动过程中可能抛出的异常
     */
    public void start()
        throws Exception {
        // 创建EventLoopGroup
        // 对于客户端，一个EventLoopGroup就足够了
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // 创建Bootstrap实例
            // Bootstrap是Netty提供的客户端启动辅助类
            Bootstrap b = new Bootstrap();
            
            // 配置Bootstrap
            b.group(group)                                          // 设置EventLoopGroup
                .channel(NioSocketChannel.class)                   // 指定Channel的类型，对应于NIO的客户端Channel
                .remoteAddress(new InetSocketAddress(host, port))   // 设置服务器的InetSocketAddress
                .handler(new ChannelInitializer<SocketChannel>() { // 设置Channel的处理器
                    @Override
                    public void initChannel(SocketChannel ch)
                        throws Exception {
                        // 当Channel被创建时，这个方法会被调用
                        // 为Channel的ChannelPipeline添加EchoClientHandler
                        ch.pipeline().addLast(
                             new EchoClientHandler());
                    }
                });
            
            // 连接到远程节点，阻塞等待直到连接完成
            ChannelFuture f = b.connect().sync();
            
            // 阻塞，直到Channel关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅关闭线程组
            group.shutdownGracefully().sync();
        }
    }

    /**
     * 程序入口点
     * 
     * @param args 命令行参数，第一个参数是主机地址，第二个参数是端口号
     * @throws Exception 启动过程中可能抛出的异常
     */
    public static void main(String[] args)
            throws Exception {
        // 检查命令行参数
        if (args.length != 2) {
            System.err.println("Usage: " + EchoClient.class.getSimpleName() +
                    " <host> <port>"
            );
            return;
        }

        // 解析命令行参数
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        
        // 创建并启动客户端
        new EchoClient(host, port).start();
    }
}

