package nia.chapter4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Channel操作示例
 * 
 * 这个类演示了Netty中Channel的基本操作，重点展示：
 * 1. 如何向Channel写入数据
 * 2. 如何使用ChannelFuture处理异步操作结果
 * 3. 如何在多线程环境中安全地使用Channel
 * 
 * 重要概念：
 * - Channel是线程安全的，可以被多个线程同时使用
 * - 写操作是异步的，返回ChannelFuture
 * - ChannelFuture提供了操作完成时的回调机制
 * - 可以通过监听器来处理操作结果
 * 
 * 代码清单 4.5 写入Channel
 * 代码清单 4.6 从多个线程使用Channel
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class ChannelOperationExamples {
    // 示例用的Channel实例
    private static final Channel CHANNEL_FROM_SOMEWHERE = new NioSocketChannel();
    
    /**
     * 演示向Channel写入数据的基本操作
     * 
     * 这个方法展示了Netty异步写操作的核心概念：
     * 1. 创建要发送的数据（ByteBuf）
     * 2. 调用writeAndFlush()异步发送数据
     * 3. 获取ChannelFuture来处理操作结果
     * 4. 添加监听器来响应操作完成事件
     * 
     * 代码清单 4.5 写入Channel
     */
    public static void writingToChannel() {
        // 获取Channel引用（在实际应用中，这通常来自于连接建立时）
        Channel channel = CHANNEL_FROM_SOMEWHERE; // Get the channel reference from somewhere
        
        // 创建要发送的数据
        // Unpooled.copiedBuffer()创建一个包含指定数据的ByteBuf
        ByteBuf buf = Unpooled.copiedBuffer("your data", CharsetUtil.UTF_8);
        
        // 异步写入数据并刷新
        // writeAndFlush()将数据写入Channel并立即刷新输出缓冲区
        // 这是一个非阻塞操作，立即返回ChannelFuture
        ChannelFuture cf = channel.writeAndFlush(buf);
        
        // 添加监听器来处理写操作的结果
        // 当写操作完成时（成功或失败），监听器的operationComplete()方法会被调用
        cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                // 检查操作是否成功
                if (future.isSuccess()) {
                    System.out.println("Write successful");
                } else {
                    // 写操作失败，打印错误信息
                    System.err.println("Write error");
                    future.cause().printStackTrace();
                }
            }
        });
    }

    /**
     * 演示在多线程环境中使用Channel
     * 
     * 这个方法展示了Channel的线程安全特性：
     * 1. Channel可以被多个线程同时访问
     * 2. 所有的I/O操作都是线程安全的
     * 3. 不需要额外的同步机制
     * 4. Netty内部会确保写操作的顺序性
     * 
     * 重要特性：
     * - Channel的操作是线程安全的
     * - 多个线程可以同时向同一个Channel写入数据
     * - Netty会保证写入操作的顺序，即使来自不同线程
     * - 这大大简化了多线程网络编程的复杂性
     * 
     * 代码清单 4.6 从多个线程使用Channel
     */
    public static void writingToChannelFromManyThreads() {
        // 获取Channel引用
        final Channel channel = CHANNEL_FROM_SOMEWHERE; // Get the channel reference from somewhere
        
        // 创建要发送的数据
        final ByteBuf buf = Unpooled.copiedBuffer("your data",
                CharsetUtil.UTF_8);
        
        // 创建一个写操作的Runnable
        // 这个Runnable将在不同的线程中执行
        Runnable writer = new Runnable() {
            @Override
            public void run() {
                // 写入数据
                // duplicate()创建ByteBuf的副本，避免多线程并发访问同一个ByteBuf的问题
                // 注意：这里只调用write()而没有flush()，数据会在稍后被刷新
                channel.write(buf.duplicate());
            }
        };
        
        // 创建线程池
        Executor executor = Executors.newCachedThreadPool();

        // 在一个线程中写入数据
        executor.execute(writer);

        // 在另一个线程中写入数据
        executor.execute(writer);
        
        // 可以继续在更多线程中执行写操作
        // Netty保证所有写操作都是线程安全的，并且按照调用顺序执行
        //...
    }
}
