package nia.chapter1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * Netty异步连接示例
 * 
 * 这个类演示了Netty框架中的异步编程模型，展示了与传统阻塞I/O的不同之处：
 * 1. 异步非阻塞：连接操作不会阻塞调用线程
 * 2. 基于回调：通过ChannelFutureListener处理连接结果
 * 3. 事件驱动：操作完成时触发相应的事件处理
 * 
 * 创建者：kerr
 * 
 * 代码清单 1.3 异步连接
 * 代码清单 1.4 回调实战
 */
public class ConnectExample {
    // 示例中使用的Channel实例（实际应用中应该通过正确的方式获取）
    private static final Channel CHANNEL_FROM_SOMEWHERE = new NioSocketChannel();

    /**
     * 演示Netty异步连接和回调机制
     * 
     * 这个方法展示了Netty异步编程的核心概念：
     * 1. 非阻塞连接：connect()方法立即返回，不等待连接完成
     * 2. Future模式：返回ChannelFuture对象，代表一个可能尚未完成的操作
     * 3. 监听器模式：通过ChannelFutureListener监听操作完成事件
     * 4. 事件驱动：连接成功或失败时，自动调用相应的处理逻辑
     * 
     * 代码清单 1.3 异步连接
     * 代码清单 1.4 回调实战
     */
    public static void connect() {
        // 获取Channel引用（这里是示例，实际应用中需要通过Bootstrap创建）
        Channel channel = CHANNEL_FROM_SOMEWHERE; // 从某处获得的Channel引用
        
        // 异步连接到远程地址
        // 这个方法不会阻塞，立即返回一个ChannelFuture对象
        ChannelFuture future = channel.connect(
                new InetSocketAddress("192.168.0.1", 25));
        
        // 添加监听器，当连接操作完成时会被调用
        // 这是Netty异步编程的核心：基于回调的事件处理
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                // 检查连接是否成功
                if (future.isSuccess()) {
                    // 连接成功，创建要发送的数据
                    // ByteBuf是Netty的字节容器，类似于NIO的ByteBuffer但功能更强大
                    ByteBuf buffer = Unpooled.copiedBuffer(
                            "Hello", Charset.defaultCharset());
                    
                    // 异步写入数据并刷新
                    // writeAndFlush也是异步操作，立即返回ChannelFuture
                    ChannelFuture wf = future.channel()
                            .writeAndFlush(buffer);
                    
                    // 这里可以继续添加写操作完成的监听器
                    // ...
                } else {
                    // 连接失败，处理异常
                    Throwable cause = future.cause();
                    cause.printStackTrace();
                }
            }
        });

        // 注意：由于是异步操作，方法执行到这里时连接可能还没有建立
        // 具体的连接结果处理在上面的监听器中进行
    }
}