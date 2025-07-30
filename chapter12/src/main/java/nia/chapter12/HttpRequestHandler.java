package nia.chapter12;

import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedNioFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * HTTP请求处理器
 * 
 * 这个处理器负责处理HTTP请求，主要功能包括：
 * 1. 识别WebSocket升级请求并转发给后续处理器
 * 2. 提供静态文件服务（聊天页面的HTML文件）
 * 3. 支持HTTP/1.1的Keep-Alive连接
 * 4. 根据是否启用SSL选择合适的文件传输方式
 * 
 * 设计特点：
 * - 双重职责：既处理普通HTTP请求，又支持WebSocket升级
 * - 零拷贝：使用FileRegion实现高效的文件传输
 * - SSL兼容：自动检测SSL并选择合适的传输方式
 * - 资源管理：正确处理文件资源的生命周期
 * 
 * 工作流程：
 * 1. 检查请求URI是否为WebSocket升级路径
 * 2. 如果是WebSocket请求，转发给下一个处理器
 * 3. 如果是普通HTTP请求，提供index.html文件服务
 * 4. 根据连接类型（Keep-Alive/Close）设置响应头
 * 5. 选择合适的文件传输方式（零拷贝或分块传输）
 * 
 * 代码清单 12.1 HTTPRequestHandler
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    // WebSocket升级的URI路径
    private final String wsUri;
    
    // 静态HTML文件的引用
    private static final File INDEX;

    // 静态初始化块，用于定位index.html文件
    static {
        // 获取当前类的代码源位置
        URL location = HttpRequestHandler.class
             .getProtectionDomain()
             .getCodeSource().getLocation();
        try {
            // 构造index.html文件的路径
            String path = location.toURI() + "index.html";
            // 处理文件URI格式，移除"file:"前缀
            path = !path.contains("file:") ? path : path.substring(5);
            INDEX = new File(path);
        } catch (URISyntaxException e) {
            // 如果无法定位index.html文件，抛出运行时异常
            throw new IllegalStateException(
                 "Unable to locate index.html", e);
        }
    }

    /**
     * 构造函数
     * 
     * @param wsUri WebSocket升级的URI路径（如"/ws"）
     */
    public HttpRequestHandler(String wsUri) {
        this.wsUri = wsUri;
    }

    /**
     * 处理HTTP请求
     * 
     * 这个方法是HTTP请求处理的核心，负责：
     * 1. 区分WebSocket升级请求和普通HTTP请求
     * 2. 为普通HTTP请求提供静态文件服务
     * 3. 处理HTTP/1.1的各种特性（Keep-Alive、100-Continue等）
     * 4. 根据SSL状态选择最优的文件传输方式
     * 
     * @param ctx ChannelHandlerContext实例
     * @param request 完整的HTTP请求对象
     * @throws Exception 处理过程中可能抛出的异常
     */
    @Override
    public void channelRead0(ChannelHandlerContext ctx,
        FullHttpRequest request) throws Exception {
        
        // 检查请求URI是否匹配WebSocket升级路径
        if (wsUri.equalsIgnoreCase(request.getUri())) {
            // 如果是WebSocket升级请求，保留引用计数并传递给下一个处理器
            // retain()确保request在传递给下一个处理器时不会被释放
            ctx.fireChannelRead(request.retain());
        } else {
            // 处理普通HTTP请求，提供静态文件服务
            
            // 检查客户端是否期望100-Continue响应
            if (HttpHeaders.is100ContinueExpected(request)) {
                // 发送100 Continue响应，告知客户端可以继续发送请求体
                send100Continue(ctx);
            }
            
            // 打开index.html文件进行读取
            RandomAccessFile file = new RandomAccessFile(INDEX, "r");
            
            // 创建HTTP响应
            HttpResponse response = new DefaultHttpResponse(
                request.getProtocolVersion(), HttpResponseStatus.OK);
            
            // 设置响应头：内容类型为HTML
            response.headers().set(
                HttpHeaders.Names.CONTENT_TYPE,
                "text/html; charset=UTF-8");
            
            // 检查是否需要保持连接活跃（HTTP/1.1 Keep-Alive）
            boolean keepAlive = HttpHeaders.isKeepAlive(request);
            if (keepAlive) {
                // 如果是Keep-Alive连接，设置Content-Length和Connection头
                response.headers().set(
                    HttpHeaders.Names.CONTENT_LENGTH, file.length());
                response.headers().set( HttpHeaders.Names.CONNECTION,
                    HttpHeaders.Values.KEEP_ALIVE);
            }
            
            // 写入HTTP响应头
            ctx.write(response);
            
            // 根据是否启用SSL选择文件传输方式
            if (ctx.pipeline().get(SslHandler.class) == null) {
                // 如果没有启用SSL，使用零拷贝的FileRegion传输文件
                // DefaultFileRegion直接从文件Channel传输到Socket Channel
                // 这是最高效的文件传输方式，避免了用户空间的数据拷贝
                ctx.write(new DefaultFileRegion(
                    file.getChannel(), 0, file.length()));
            } else {
                // 如果启用了SSL，使用ChunkedNioFile进行分块传输
                // 因为SSL需要加密数据，无法直接使用零拷贝
                ctx.write(new ChunkedNioFile(file.getChannel()));
            }
            
            // 写入LastHttpContent，标示HTTP响应结束
            ChannelFuture future = ctx.writeAndFlush(
                LastHttpContent.EMPTY_LAST_CONTENT);
            
            // 如果不是Keep-Alive连接，在响应完成后关闭连接
            if (!keepAlive) {
                future.addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    /**
     * 发送100 Continue响应
     * 
     * 当客户端发送Expect: 100-continue头时，服务器应该响应100 Continue
     * 来告知客户端可以继续发送请求体。这是HTTP/1.1的一个优化特性。
     * 
     * @param ctx ChannelHandlerContext实例
     */
    private static void send100Continue(ChannelHandlerContext ctx) {
        // 创建100 Continue响应
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        
        // 立即发送响应
        ctx.writeAndFlush(response);
    }
    
    /**
     * 异常处理
     * 
     * 当处理HTTP请求时发生异常，记录异常信息并关闭连接。
     * 这确保了异常情况下资源能够被正确清理。
     * 
     * @param ctx ChannelHandlerContext实例
     * @param cause 异常对象
     * @throws Exception 重新抛出异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
        throws Exception {
        // 打印异常堆栈信息
        cause.printStackTrace();
        
        // 关闭连接
        ctx.close();
    }
}
