package nia.chapter4;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * 传统NIO服务器
 * 
 * 这个类演示了不使用Netty的传统Java NIO服务器实现。
 * NIO（Non-blocking I/O）的特点：
 * 1. 使用Selector实现单线程处理多个连接
 * 2. 基于事件驱动的I/O模型
 * 3. 可以用较少的线程处理大量连接
 * 4. 但编程复杂度较高，容易出错
 * 
 * 传统NIO的问题：
 * - 代码复杂，难以理解和维护
 * - 需要处理很多底层细节
 * - 容易出现内存泄漏和资源管理问题
 * - 异常处理复杂
 * - 平台相关的Bug（如epoll bug）
 * 
 * 代码清单 4.2 不使用Netty的异步网络编程
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class PlainNioServer {
    
    /**
     * 启动NIO服务器
     * 
     * 这个方法展示了传统NIO服务器的复杂实现：
     * 1. 创建ServerSocketChannel并配置为非阻塞模式
     * 2. 创建Selector用于事件多路复用
     * 3. 注册ServerSocketChannel到Selector
     * 4. 在事件循环中处理各种I/O事件
     * 
     * @param port 服务器监听的端口号
     * @throws IOException 当I/O操作发生错误时抛出
     */
    public void serve(int port) throws IOException {
        // 创建ServerSocketChannel
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        
        // 配置为非阻塞模式，这是NIO的关键特性
        serverChannel.configureBlocking(false);
        
        // 获取关联的ServerSocket并绑定地址
        ServerSocket ss = serverChannel.socket();
        InetSocketAddress address = new InetSocketAddress(port);
        ss.bind(address);
        
        // 创建Selector，用于管理多个Channel的I/O事件
        Selector selector = Selector.open();
        
        // 将ServerSocketChannel注册到Selector，监听ACCEPT事件
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        // 预先准备响应消息的ByteBuffer
        final ByteBuffer msg = ByteBuffer.wrap("Hi!\r\n".getBytes());
        
        // 事件循环：NIO服务器的核心
        for (;;){
            try {
                // 阻塞等待事件发生
                // 这是唯一的阻塞调用，但可以同时监听多个Channel
                selector.select();
            } catch (IOException ex) {
                ex.printStackTrace();
                // 处理异常并退出循环
                break;
            }
            
            // 获取已准备好的SelectionKey集合
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            
            // 遍历处理每个就绪的事件
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // 必须手动移除，否则会重复处理
                iterator.remove();
                
                try {
                    // 检查事件类型并处理
                    
                    // 处理新的连接请求
                    if (key.isAcceptable()) {
                        ServerSocketChannel server =
                                (ServerSocketChannel) key.channel();
                        
                        // 接受新连接
                        SocketChannel client = server.accept();
                        
                        // 将客户端Channel配置为非阻塞模式
                        client.configureBlocking(false);
                        
                        // 注册客户端Channel到Selector，监听读写事件
                        // 并附加响应消息的副本
                        client.register(selector, SelectionKey.OP_WRITE |
                                SelectionKey.OP_READ, msg.duplicate());
                        System.out.println(
                                "Accepted connection from " + client);
                    }
                    
                    // 处理写事件
                    if (key.isWritable()) {
                        SocketChannel client =
                                (SocketChannel) key.channel();
                        
                        // 获取附加的ByteBuffer
                        ByteBuffer buffer =
                                (ByteBuffer) key.attachment();
                        
                        // 写入数据，可能需要多次写入才能完成
                        while (buffer.hasRemaining()) {
                            // 非阻塞写入，返回实际写入的字节数
                            if (client.write(buffer) == 0) {
                                // 如果写入0字节，说明TCP缓冲区已满，需要等待下次写事件
                                break;
                            }
                        }
                        
                        // 数据发送完毕，关闭连接
                        client.close();
                    }
                } catch (IOException ex) {
                    // 处理异常：取消SelectionKey并关闭Channel
                    key.cancel();
                    try {
                        key.channel().close();
                    } catch (IOException cex) {
                        // 忽略关闭时的异常
                        // ignore on close
                    }
                }
            }
        }
    }
}

