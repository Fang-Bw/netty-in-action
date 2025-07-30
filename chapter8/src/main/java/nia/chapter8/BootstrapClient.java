package nia.chapter8;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * 客户端Bootstrap示例
 * 
 * 这个类演示了如何使用Netty的Bootstrap类来配置和启动TCP客户端。
 * Bootstrap是Netty提供的客户端启动辅助类，简化了客户端的配置过程。
 * 
 * Bootstrap的核心功能：
 * 1. EventLoopGroup管理：配置客户端的事件循环组
 * 2. Channel类型选择：指定客户端使用的Channel实现
 * 3. 处理器配置：设置消息处理的ChannelHandler
 * 4. 连接建立：异步连接到远程服务器
 * 5. 选项配置：设置Socket选项和Channel属性
 * 
 * 与ServerBootstrap的区别：
 * - Bootstrap用于客户端，ServerBootstrap用于服务器
 * - Bootstrap只需要一个EventLoopGroup，ServerBootstrap需要两个
 * - Bootstrap使用connect()连接，ServerBootstrap使用bind()绑定
 * - Bootstrap没有childHandler概念，直接使用handler()
 * 
 * 典型应用场景：
 * - HTTP客户端
 * - RPC客户端
 * - 数据库客户端
 * - 消息队列客户端
 * 
 * 代码清单 8.1 引导客户端
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 * @author <a href="mailto:mawolfthal@gmail.com">Marvin Wolfthal</a>
 */
public class BootstrapClient {
    
    /**
     * 程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String args[]) {
        BootstrapClient client = new BootstrapClient();
        client.bootstrap();
    }

    /**
     * 引导客户端的核心方法
     * 
     * 这个方法展示了使用Bootstrap配置和启动Netty客户端的完整流程：
     * 1. 创建EventLoopGroup处理I/O操作
     * 2. 创建Bootstrap实例并进行配置
     * 3. 设置Channel类型和处理器
     * 4. 连接到远程服务器
     * 5. 添加连接监听器处理连接结果
     * 
     * 配置要点：
     * - group()：设置EventLoopGroup，客户端只需要一个
     * - channel()：指定Channel类型，这里使用NioSocketChannel
     * - handler()：设置ChannelHandler，处理接收到的数据
     * - connect()：异步连接到指定的远程地址
     * 
     * 代码清单 8.1 引导客户端
     */
    public void bootstrap() {
        // 创建EventLoopGroup
        // 对于客户端，通常只需要一个EventLoopGroup来处理所有I/O操作
        EventLoopGroup group = new NioEventLoopGroup();
        
        // 创建Bootstrap实例
        Bootstrap bootstrap = new Bootstrap();
        
        // 配置Bootstrap
        bootstrap.group(group)                          // 设置EventLoopGroup
            .channel(NioSocketChannel.class)            // 指定Channel类型为NioSocketChannel
            .handler(new SimpleChannelInboundHandler<ByteBuf>() {  // 设置ChannelHandler
                /**
                 * 处理接收到的数据
                 * 
                 * 这个方法在从服务器接收到数据时被调用。
                 * SimpleChannelInboundHandler会自动处理ByteBuf的释放，
                 * 所以我们不需要手动调用release()。
                 * 
                 * @param channelHandlerContext Channel的上下文
                 * @param byteBuf 接收到的数据
                 * @throws Exception 处理过程中可能的异常
                 */
                @Override
                protected void channelRead0(
                    ChannelHandlerContext channelHandlerContext,
                    ByteBuf byteBuf) throws Exception {
                    // 简单打印接收到数据的消息
                    // 在实际应用中，这里会包含具体的数据处理逻辑
                    System.out.println("Received data");
                    
                    // 可以在这里添加更多处理逻辑：
                    // - 解析协议消息
                    // - 处理业务逻辑
                    // - 发送响应消息
                    // - 更新客户端状态
                }
                });
        
        // 异步连接到远程服务器
        // connect()方法是异步的，立即返回ChannelFuture
        ChannelFuture future =
            bootstrap.connect(
                    new InetSocketAddress("www.manning.com", 80));
        
        // 添加连接监听器处理连接结果
        future.addListener(new ChannelFutureListener() {
            /**
             * 连接操作完成时的回调方法
             * 
             * 无论连接成功还是失败，这个方法都会被调用。
             * 通过检查ChannelFuture的状态可以确定连接结果。
             * 
             * @param channelFuture 连接操作的Future对象
             * @throws Exception 处理过程中可能的异常
             */
            @Override
            public void operationComplete(ChannelFuture channelFuture)
                throws Exception {
                // 检查连接是否成功
                if (channelFuture.isSuccess()) {
                    System.out.println("Connection established");
                    
                    // 连接成功，可以进行后续操作：
                    // - 发送初始消息
                    // - 启动心跳机制
                    // - 更新客户端状态
                    // Channel channel = channelFuture.channel();
                    // channel.writeAndFlush(someMessage);
                    
                } else {
                    // 连接失败，处理错误情况
                    System.err.println("Connection attempt failed");
                    channelFuture.cause().printStackTrace();
                    
                    // 连接失败的处理策略：
                    // - 记录错误日志
                    // - 实施重连机制
                    // - 通知应用层
                    // - 清理资源
                }
            }
        });
        
        // 重要提醒：
        // 1. 这个方法在连接建立后立即返回，连接是异步进行的
        // 2. 实际应用中应该等待连接完成或者实施适当的资源管理
        // 3. 应该在适当的时机调用group.shutdownGracefully()来释放资源
        // 4. 可以通过future.sync()来等待连接完成（阻塞方式）
    }
}
