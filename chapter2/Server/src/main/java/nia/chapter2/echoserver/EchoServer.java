package nia.chapter2.echoserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * Echo服务器主类
 * 
 * 这是一个简单的Echo服务器，演示了Netty服务器端的基本架构和启动流程。
 * Echo服务器会将接收到的任何数据原样返回给客户端。
 * 
 * Netty服务器的核心组件：
 * 1. EventLoopGroup：事件循环组，处理I/O操作
 * 2. ServerBootstrap：服务器启动辅助类，用于配置服务器
 * 3. Channel：网络连接的抽象，代表一个打开的连接
 * 4. ChannelHandler：处理I/O事件的逻辑组件
 * 5. ChannelPipeline：ChannelHandler的容器，形成处理链
 * 
 * 代码清单 2.2 EchoServer类
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class EchoServer {
    // 服务器监听的端口号
    private final int port;

    /**
     * 构造函数，初始化服务器端口
     * 
     * @param port 服务器监听的端口号
     */
    public EchoServer(int port) {
        this.port = port;
    }

    /**
     * 程序入口点
     * 
     * @param args 命令行参数，第一个参数应该是端口号
     * @throws Exception 启动过程中可能抛出的异常
     */
    public static void main(String[] args)
        throws Exception {
        // 检查命令行参数
        if (args.length != 1) {
            System.err.println("Usage: " + EchoServer.class.getSimpleName() +
                " <port>"
            );
            return;
        }
        // 解析端口号并启动服务器
        int port = Integer.parseInt(args[0]);
        new EchoServer(port).start();
    }

    /**
     * 启动服务器
     * 
     * 这个方法展示了Netty服务器的典型启动流程：
     * 1. 创建EventLoopGroup来处理I/O操作
     * 2. 使用ServerBootstrap配置服务器参数
     * 3. 设置Channel类型和处理器
     * 4. 绑定端口并开始监听
     * 5. 等待服务器关闭
     * 6. 优雅关闭资源
     * 
     * @throws Exception 启动过程中可能抛出的异常
     */
    public void start() throws Exception {
        // 创建EchoServerHandler的实例，将在后面重用
        final EchoServerHandler serverHandler = new EchoServerHandler();
        
        // 创建EventLoopGroup
        // NioEventLoopGroup是一个多线程的EventLoopGroup，用于处理I/O操作
        // 对于服务器端，通常使用一个EventLoopGroup来处理所有的I/O操作
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // 创建ServerBootstrap实例
            // ServerBootstrap是Netty提供的服务器端启动辅助类
            ServerBootstrap b = new ServerBootstrap();
            
            // 配置ServerBootstrap
            b.group(group)                                    // 设置EventLoopGroup
                .channel(NioServerSocketChannel.class)       // 指定使用NIO传输Channel
                .localAddress(new InetSocketAddress(port))    // 设置服务器绑定的本地地址
                .childHandler(new ChannelInitializer<SocketChannel>() {  // 设置子Channel的处理器
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        // 当新连接被接受时，这个方法会被调用
                        // 为新连接的ChannelPipeline添加EchoServerHandler
                        ch.pipeline().addLast(serverHandler);
                    }
                });

            // 异步绑定服务器；调用sync()方法阻塞等待直到绑定完成
            ChannelFuture f = b.bind().sync();
            
            // 打印服务器启动信息
            System.out.println(EchoServer.class.getName() +
                " started and listening for connections on " + f.channel().localAddress());
            
            // 等待服务器的Channel关闭
            // 这行代码会阻塞，直到Channel关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅关闭EventLoopGroup，释放所有资源
            group.shutdownGracefully().sync();
        }
    }
}
