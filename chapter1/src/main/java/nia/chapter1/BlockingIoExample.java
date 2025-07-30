package nia.chapter1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 传统阻塞I/O示例
 * 
 * 这个类演示了传统Java网络编程中的阻塞I/O模型。
 * 在这种模型中，每个连接都会阻塞一个线程，直到有数据可读或可写。
 * 这种方式的缺点是：
 * 1. 一个连接对应一个线程，资源消耗大
 * 2. 线程上下文切换开销大
 * 3. 连接数受限于系统线程数
 * 
 * 创建者：kerr
 * 
 * 代码清单 1.1 阻塞I/O示例
 */
public class BlockingIoExample {

    /**
     * 启动服务器并处理客户端连接
     * 
     * 此方法演示了传统的阻塞I/O服务器实现：
     * 1. 创建ServerSocket监听指定端口
     * 2. 等待客户端连接（阻塞调用）
     * 3. 为每个连接创建输入输出流
     * 4. 在循环中读取客户端数据并响应
     * 
     * 注意：这个实现一次只能处理一个客户端连接
     * 
     * @param portNumber 服务器监听的端口号
     * @throws IOException 当I/O操作发生错误时抛出
     * 
     * 代码清单 1.1 阻塞I/O示例
     */
    public void serve(int portNumber) throws IOException {
        // 创建ServerSocket并绑定到指定端口
        ServerSocket serverSocket = new ServerSocket(portNumber);
        
        // 等待客户端连接，这是一个阻塞调用
        // 线程会在这里等待，直到有客户端连接进来
        Socket clientSocket = serverSocket.accept();
        
        // 创建输入流，用于读取客户端发送的数据
        BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
        
        // 创建输出流，用于向客户端发送响应数据
        // 第二个参数true表示自动刷新缓冲区
        PrintWriter out =
                new PrintWriter(clientSocket.getOutputStream(), true);
        
        String request, response;
        
        // 循环读取客户端请求并处理
        // readLine()是阻塞调用，会等待客户端发送数据
        while ((request = in.readLine()) != null) {
            // 如果收到"Done"消息，退出循环
            if ("Done".equals(request)) {
                break;
            }
            
            // 处理客户端请求
            response = processRequest(request);
            
            // 将响应发送给客户端
            out.println(response);
        }
    }

    /**
     * 处理客户端请求的方法
     * 
     * 这是一个简单的请求处理方法，实际应用中可以根据需要
     * 实现复杂的业务逻辑
     * 
     * @param request 客户端发送的请求字符串
     * @return 处理后的响应字符串
     */
    private String processRequest(String request){
        // 简单返回"Processed"，实际应用中可以实现具体的业务逻辑
        return "Processed";
    }
}
