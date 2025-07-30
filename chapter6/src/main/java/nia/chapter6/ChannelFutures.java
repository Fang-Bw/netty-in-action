package nia.chapter6;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * ChannelFuture操作示例
 * 
 * 这个类演示了如何使用ChannelFuture来处理异步操作的结果。
 * ChannelFuture是Netty异步编程模型的核心，它代表一个可能尚未完成的I/O操作。
 * 
 * ChannelFuture的重要特性：
 * 1. 异步性：I/O操作立即返回ChannelFuture，不会阻塞调用线程
 * 2. 回调机制：可以添加监听器来处理操作完成事件
 * 3. 结果获取：可以检查操作是否成功，获取异常信息
 * 4. 链式操作：支持添加多个监听器
 * 
 * 使用模式：
 * - 添加监听器处理异步结果
 * - 检查操作状态和错误处理
 * - 在操作完成时执行后续逻辑
 * 
 * 创建者：kerr
 * 
 * 代码清单 6.13 为ChannelFuture添加ChannelFutureListener
 */
public class ChannelFutures {
    // 示例用的Channel实例
    private static final Channel CHANNEL_FROM_SOMEWHERE = new NioSocketChannel();
    // 示例用的ByteBuf消息
    private static final ByteBuf SOME_MSG_FROM_SOMEWHERE = Unpooled.buffer(1024);

    /**
     * 演示如何为ChannelFuture添加ChannelFutureListener
     * 
     * 这个方法展示了Netty异步编程的典型模式：
     * 1. 执行异步I/O操作（如write）
     * 2. 获取ChannelFuture对象
     * 3. 添加监听器来处理操作结果
     * 4. 在监听器中处理成功或失败的情况
     * 
     * 监听器的优势：
     * - 非阻塞：不会阻塞调用线程
     * - 及时响应：操作完成时立即被调用
     * - 错误处理：可以统一处理各种异常情况
     * - 资源管理：可以在适当时机清理资源
     * 
     * 代码清单 6.13 为ChannelFuture添加ChannelFutureListener
     */
    public static void addingChannelFutureListener(){
        // 获取Channel引用（从某处得到）
        Channel channel = CHANNEL_FROM_SOMEWHERE; // get reference to pipeline;
        
        // 获取要发送的消息（从某处得到）
        ByteBuf someMessage = SOME_MSG_FROM_SOMEWHERE; // get reference to pipeline;
        
        //...执行其他业务逻辑
        
        // 异步写入消息，立即返回ChannelFuture
        // 注意：这里只调用了write()而没有flush()，
        // 数据会被写入到outbound缓冲区，但不会立即发送
        io.netty.channel.ChannelFuture future = channel.write(someMessage);
        
        // 为ChannelFuture添加监听器
        // 当写操作完成时（成功或失败），监听器会被调用
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(io.netty.channel.ChannelFuture f) {
                // 检查操作是否失败
                if (!f.isSuccess()) {
                    // 如果写操作失败，打印异常堆栈信息
                    f.cause().printStackTrace();
                    
                    // 关闭Channel，因为发生了错误
                    // 这是一种常见的错误处理策略
                    f.channel().close();
                }
                // 注意：如果操作成功，我们在这里不做任何事情
                // 在实际应用中，你可能需要执行一些清理工作或记录日志
            }
        });
        
        // 重要提醒：
        // 1. 监听器会在I/O线程中被调用，避免在其中执行耗时操作
        // 2. 可以添加多个监听器，它们会按添加顺序依次执行
        // 3. 如果操作已经完成，监听器会立即被调用
        // 4. 可以使用预定义的监听器，如ChannelFutureListener.CLOSE
    }
}
