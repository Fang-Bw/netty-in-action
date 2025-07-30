package nia.chapter4;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;

/**
 * 传统阻塞I/O服务器
 * 
 * 这个类演示了不使用Netty的传统阻塞I/O服务器实现。
 * 该服务器的特点：
 * 1. 为每个连接创建一个新的线程
 * 2. 每个线程在I/O操作时都会阻塞
 * 3. 线程资源消耗大，扩展性差
 * 4. 当连接数增加时，性能会急剧下降
 * 
 * 这种模式的主要问题：
 * - 线程数量与连接数成正比，资源消耗大
 * - 线程上下文切换开销高
 * - 大量线程可能导致OutOfMemoryError
 * - 难以处理大量并发连接
 * 
 * 代码清单 4.1 不使用Netty的阻塞网络编程
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class PlainOioServer {
    
    /**
     * 启动服务器
     * 
     * 这个方法展示了传统阻塞I/O服务器的典型实现：
     * 1. 创建ServerSocket并绑定端口
     * 2. 在无限循环中等待连接
     * 3. 为每个新连接创建一个新线程
     * 4. 在新线程中处理客户端请求
     * 
     * @param port 服务器监听的端口号
     * @throws IOException 当I/O操作发生错误时抛出
     */
    public void serve(int port) throws IOException {
        // 创建ServerSocket并绑定到指定端口
        final ServerSocket socket = new ServerSocket(port);
        try {
            // 无限循环，持续接受新连接
            for(;;) {
                // 阻塞等待客户端连接
                // 这是一个阻塞调用，线程会在这里等待
                final Socket clientSocket = socket.accept();
                System.out.println(
                        "Accepted connection from " + clientSocket);
                
                // 为每个新连接创建一个新线程
                // 这是传统阻塞I/O模型的典型做法，但会消耗大量线程资源
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        OutputStream out;
                        try {
                            // 获取客户端Socket的输出流
                            out = clientSocket.getOutputStream();
                            
                            // 向客户端发送响应消息
                            // write()是阻塞操作，会等待数据完全写入
                            out.write("Hi!\r\n".getBytes(
                                    Charset.forName("UTF-8")));
                            
                            // 刷新输出流，确保数据被发送
                            out.flush();
                            
                            // 关闭客户端连接
                            clientSocket.close();
                        } catch (IOException e) {
                            // 处理I/O异常
                            e.printStackTrace();
                        } finally {
                            try {
                                // 确保客户端Socket被关闭
                                clientSocket.close();
                            } catch (IOException ex) {
                                // 忽略关闭时的异常
                                // ignore on close
                            }
                        }
                    }
                }).start(); // 启动新线程
            }
        } catch (IOException e) {
            // 处理服务器异常
            e.printStackTrace();
        }
    }
}
