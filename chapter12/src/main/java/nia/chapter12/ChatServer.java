package nia.chapter12;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;

import java.net.InetSocketAddress;

/**
 * WebSocket聊天服务器
 * 
 * 这是一个完整的WebSocket聊天服务器实现，演示了：
 * 1. HTTP到WebSocket的协议升级
 * 2. 多客户端连接管理
 * 3. 消息广播机制
 * 4. 资源生命周期管理
 * 
 * 服务器架构特点：
 * - 支持HTTP和WebSocket双协议
 * - 使用ChannelGroup管理所有连接
 * - 自动处理客户端连接和断开
 * - 实现消息的实时广播
 * 
 * 技术亮点：
 * 1. 协议切换：从HTTP升级到WebSocket
 * 2. 群组管理：使用ChannelGroup统一管理连接
 * 3. 事件驱动：基于Netty的异步事件模型
 * 4. 优雅关闭：正确处理资源清理
 * 
 * 代码清单 12.4 引导服务器
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class ChatServer {
    // ChannelGroup用于管理所有的客户端连接
    // DefaultChannelGroup会自动跟踪添加到其中的Channel
    // 当ChannelGroup被关闭时，所有包含的Channel都会被关闭
    private final ChannelGroup channelGroup =
        new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    
    // EventLoopGroup用于处理I/O操作
    private final EventLoopGroup group = new NioEventLoopGroup();
    
    // 服务器Channel，用于接受新连接
    private Channel channel;

    /**
     * 启动聊天服务器
     * 
     * 这个方法负责配置和启动WebSocket聊天服务器：
     * 1. 创建ServerBootstrap并配置基本参数
     * 2. 设置ChannelInitializer来配置每个新连接的处理管道
     * 3. 绑定到指定地址并开始监听连接
     * 4. 保存服务器Channel引用以便后续关闭
     * 
     * @param address 服务器绑定的地址和端口
     * @return ChannelFuture 代表绑定操作的异步结果
     */
    public ChannelFuture start(InetSocketAddress address) {
        // 创建ServerBootstrap实例
        ServerBootstrap bootstrap = new ServerBootstrap();
        
        // 配置ServerBootstrap
        bootstrap.group(group)                              // 设置EventLoopGroup
             .channel(NioServerSocketChannel.class)         // 指定Channel类型
             .childHandler(createInitializer(channelGroup)); // 设置子Channel的初始化器
        
        // 绑定到指定地址
        ChannelFuture future = bootstrap.bind(address);
        
        // 同步等待绑定完成，如果失败会抛出异常
        future.syncUninterruptibly();
        
        // 保存服务器Channel引用
        channel = future.channel();
        
        return future;
    }

    /**
     * 创建ChannelInitializer
     * 
     * 这个方法创建用于初始化新连接的ChannelInitializer。
     * 使用工厂方法模式，便于子类重写以定制初始化逻辑。
     * 
     * @param group 用于管理客户端连接的ChannelGroup
     * @return ChannelInitializer实例
     */
    protected ChannelInitializer<Channel> createInitializer(
        ChannelGroup group) {
        return new ChatServerInitializer(group);
    }

    /**
     * 销毁服务器并清理资源
     * 
     * 这个方法负责优雅地关闭服务器并清理所有资源：
     * 1. 关闭服务器Channel，停止接受新连接
     * 2. 关闭ChannelGroup，断开所有客户端连接
     * 3. 优雅关闭EventLoopGroup，释放线程资源
     * 
     * 关闭顺序很重要，确保资源被正确释放：
     * - 先关闭服务器Channel
     * - 再关闭所有客户端连接
     * - 最后关闭线程池
     */
    public void destroy() {
        // 关闭服务器Channel
        if (channel != null) {
            channel.close();
        }
        
        // 关闭所有客户端连接
        // ChannelGroup.close()会关闭组内所有的Channel
        channelGroup.close();
        
        // 优雅关闭EventLoopGroup
        // 这会等待正在执行的任务完成，然后关闭线程池
        group.shutdownGracefully();
    }

    /**
     * 程序入口点
     * 
     * 这个方法演示了如何启动聊天服务器并正确处理关闭：
     * 1. 解析命令行参数获取端口号
     * 2. 创建并启动服务器
     * 3. 注册关闭钩子确保优雅关闭
     * 4. 等待服务器关闭
     * 
     * @param args 命令行参数，应包含端口号
     * @throws Exception 启动过程中可能抛出的异常
     */
    public static void main(String[] args) throws Exception {
        // 检查命令行参数
        if (args.length != 1) {
            System.err.println("Please give port as argument");
            System.exit(1);
        }
        
        // 解析端口号
        int port = Integer.parseInt(args[0]);
        
        // 创建聊天服务器实例
        final ChatServer endpoint = new ChatServer();
        
        // 启动服务器
        ChannelFuture future = endpoint.start(
                new InetSocketAddress(port));
        
        // 注册JVM关闭钩子，确保服务器优雅关闭
        // 当JVM接收到关闭信号时，这个钩子会被执行
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // 销毁服务器并清理资源
                endpoint.destroy();
            }
        });
        
        // 等待服务器Channel关闭
        // 这会阻塞主线程，直到服务器被关闭
        future.channel().closeFuture().syncUninterruptibly();
    }
}
