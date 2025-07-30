package nia.chapter8;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * 服务器Bootstrap示例
 * 
 * 这个类演示了如何使用Netty的ServerBootstrap类来配置和启动TCP服务器。
 * ServerBootstrap是Netty提供的服务器端启动辅助类，专门用于配置服务器应用。
 * 
 * ServerBootstrap的核心功能：
 * 1. EventLoopGroup管理：配置父子EventLoopGroup
 * 2. Channel类型选择：指定服务器使用的ServerChannel实现
 * 3. 处理器配置：设置父Channel和子Channel的处理器
 * 4. 端口绑定：绑定到指定端口开始监听
 * 5. 选项配置：设置Socket选项和Channel属性
 * 
 * 与Bootstrap的区别：
 * - ServerBootstrap用于服务器，Bootstrap用于客户端
 * - ServerBootstrap可以配置两个EventLoopGroup（父子）
 * - ServerBootstrap使用bind()绑定端口，Bootstrap使用connect()连接
 * - ServerBootstrap有childHandler()用于配置子Channel处理器
 * 
 * EventLoopGroup分工：
 * - 父EventLoopGroup：处理连接接受（accept）
 * - 子EventLoopGroup：处理已建立连接的I/O操作
 * 
 * 典型应用场景：
 * - HTTP服务器
 * - RPC服务器
 * - 游戏服务器
 * - 消息代理服务器
 * 
 * 代码清单 8.4 引导服务器
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 * @author <a href="mailto:mawolfthal@gmail.com">Marvin Wolfthal</a>
 */
public class BootstrapServer {

    /**
     * 引导服务器的核心方法
     * 
     * 这个方法展示了使用ServerBootstrap配置和启动Netty服务器的完整流程：
     * 1. 创建EventLoopGroup处理I/O操作
     * 2. 创建ServerBootstrap实例并进行配置
     * 3. 设置ServerChannel类型和子Channel处理器
     * 4. 绑定到指定端口开始监听
     * 5. 添加绑定监听器处理绑定结果
     * 
     * 配置要点：
     * - group()：设置EventLoopGroup，这里使用相同的组处理所有操作
     * - channel()：指定ServerChannel类型，这里使用NioServerSocketChannel
     * - childHandler()：设置子Channel（客户端连接）的处理器
     * - bind()：绑定到指定端口并开始监听
     * 
     * 代码清单 8.4 引导服务器
     */
    public void bootstrap() {
        // 创建EventLoopGroup
        // 在这个简化示例中，使用单个EventLoopGroup处理所有操作
        // 实际应用中，通常会使用两个不同的EventLoopGroup：
        // - 一个用于接受连接（boss group）
        // - 一个用于处理I/O操作（worker group）
        NioEventLoopGroup group = new NioEventLoopGroup();
        
        // 创建ServerBootstrap实例
        ServerBootstrap bootstrap = new ServerBootstrap();
        
        // 配置ServerBootstrap
        bootstrap.group(group)                          // 设置EventLoopGroup
            .channel(NioServerSocketChannel.class)      // 指定ServerChannel类型
            .childHandler(new SimpleChannelInboundHandler<ByteBuf>() {  // 设置子Channel处理器
                /**
                 * 处理从客户端接收到的数据
                 * 
                 * 这个方法在从客户端接收到数据时被调用。
                 * 每个连接到服务器的客户端都会有自己的Channel，
                 * 这个处理器的实例会被添加到每个客户端Channel的Pipeline中。
                 * 
                 * 注意：
                 * - 这个处理器实例会被多个客户端连接共享
                 * - SimpleChannelInboundHandler自动处理ByteBuf的释放
                 * - 如果需要状态管理，应该考虑线程安全问题
                 * 
                 * @param channelHandlerContext Channel的上下文
                 * @param byteBuf 接收到的数据
                 * @throws Exception 处理过程中可能的异常
                 */
                @Override
                protected void channelRead0(ChannelHandlerContext channelHandlerContext,
                    ByteBuf byteBuf) throws Exception {
                    // 简单打印接收到数据的消息
                    // 在实际应用中，这里会包含具体的业务逻辑
                    System.out.println("Received data");
                    
                    // 典型的服务器处理逻辑：
                    // - 解析客户端请求
                    // - 执行业务逻辑
                    // - 生成响应数据
                    // - 发送响应给客户端
                    // channelHandlerContext.writeAndFlush(response);
                }
            });
        
        // 绑定到指定端口
        // bind()方法是异步的，立即返回ChannelFuture
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080));
        
        // 添加绑定监听器处理绑定结果
        future.addListener(new ChannelFutureListener() {
            /**
             * 绑定操作完成时的回调方法
             * 
             * 无论绑定成功还是失败，这个方法都会被调用。
             * 通过检查ChannelFuture的状态可以确定绑定结果。
             * 
             * @param channelFuture 绑定操作的Future对象
             * @throws Exception 处理过程中可能的异常
             */
            @Override
            public void operationComplete(ChannelFuture channelFuture)
                throws Exception {
                // 检查绑定是否成功
                if (channelFuture.isSuccess()) {
                    System.out.println("Server bound");
                    
                    // 绑定成功，服务器开始监听连接：
                    // - 记录成功启动日志
                    // - 初始化服务器状态
                    // - 启动监控和管理功能
                    // - 通知其他组件服务器已就绪
                    
                } else {
                    // 绑定失败，处理错误情况
                    System.err.println("Bind attempt failed");
                    channelFuture.cause().printStackTrace();
                    
                    // 绑定失败的处理策略：
                    // - 记录详细的错误信息
                    // - 检查端口是否被占用
                    // - 尝试绑定其他端口
                    // - 通知应用层启动失败
                    // - 清理已分配的资源
                }
            }
        });
        
        // 重要提醒：
        // 1. 这个方法在绑定完成后立即返回，绑定是异步进行的
        // 2. 实际应用中应该等待绑定完成或者实施适当的资源管理
        // 3. 应该在应用关闭时调用group.shutdownGracefully()来释放资源
        // 4. 可以通过future.sync()来等待绑定完成（阻塞方式）
        // 5. 服务器启动后会持续运行直到被显式关闭
        
        // 更完整的服务器启动示例：
        // try {
        //     future.sync(); // 等待绑定完成
        //     future.channel().closeFuture().sync(); // 等待服务器关闭
        // } finally {
        //     group.shutdownGracefully(); // 优雅关闭资源
        // }
    }
}
