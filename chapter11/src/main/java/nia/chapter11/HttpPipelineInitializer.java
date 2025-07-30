package nia.chapter11;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * HTTP管道初始化器
 * 
 * 这个类演示了如何为客户端和服务器端添加HTTP协议支持。
 * HTTP是应用层协议，需要在TCP连接之上进行HTTP消息的编解码。
 * 
 * HTTP协议的特点：
 * 1. 基于请求-响应模式
 * 2. 无状态协议
 * 3. 文本协议（虽然现在也支持二进制）
 * 4. 客户端发送请求，服务器返回响应
 * 
 * Netty HTTP编解码器：
 * - HttpRequestDecoder：将字节流解码为HttpRequest对象
 * - HttpRequestEncoder：将HttpRequest对象编码为字节流
 * - HttpResponseDecoder：将字节流解码为HttpResponse对象
 * - HttpResponseEncoder：将HttpResponse对象编码为字节流
 * 
 * 客户端vs服务器端的区别：
 * - 客户端：接收响应（需要ResponseDecoder），发送请求（需要RequestEncoder）
 * - 服务器端：接收请求（需要RequestDecoder），发送响应（需要ResponseEncoder）
 * 
 * 使用场景：
 * - HTTP客户端开发
 * - HTTP服务器开发
 * - REST API客户端/服务器
 * - Web代理服务器
 * 
 * 代码清单 11.2 添加HTTP支持
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class HttpPipelineInitializer extends ChannelInitializer<Channel> {
    // 标识是否为客户端模式
    private final boolean client;

    /**
     * 构造函数
     * 
     * @param client true表示客户端模式，false表示服务器模式
     *               不同模式需要配置不同的编解码器
     */
    public HttpPipelineInitializer(boolean client) {
        this.client = client;
    }

    /**
     * 初始化Channel的HTTP处理管道
     * 
     * 根据客户端或服务器模式配置相应的HTTP编解码器。
     * 配置的顺序很重要：解码器处理入站数据，编码器处理出站数据。
     * 
     * 客户端配置：
     * - HttpResponseDecoder：解码来自服务器的HTTP响应
     * - HttpRequestEncoder：编码要发送到服务器的HTTP请求
     * 
     * 服务器端配置：
     * - HttpRequestDecoder：解码来自客户端的HTTP请求
     * - HttpResponseEncoder：编码要发送到客户端的HTTP响应
     * 
     * 重要提醒：
     * - 解码器和编码器的顺序不能颠倒
     * - 客户端和服务器端的配置是对称的
     * - 这些编解码器会自动处理HTTP协议的细节
     * 
     * @param ch 要初始化的Channel
     * @throws Exception 初始化过程中可能抛出的异常
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {
        // 获取Channel的Pipeline
        ChannelPipeline pipeline = ch.pipeline();
        
        if (client) {
            // 客户端配置
            // 添加HTTP响应解码器，用于解码服务器返回的响应
            pipeline.addLast("decoder", new HttpResponseDecoder());
            // 添加HTTP请求编码器，用于编码发送到服务器的请求
            pipeline.addLast("encoder", new HttpRequestEncoder());
        } else {
            // 服务器端配置
            // 添加HTTP请求解码器，用于解码客户端发送的请求
            pipeline.addLast("decoder", new HttpRequestDecoder());
            // 添加HTTP响应编码器，用于编码发送到客户端的响应
            pipeline.addLast("encoder", new HttpResponseEncoder());
        }
        
        // 配置完成后，可以在Pipeline中添加更多的处理器：
        // pipeline.addLast("aggregator", new HttpObjectAggregator(65536));  // HTTP消息聚合
        // pipeline.addLast("compressor", new HttpContentCompressor());      // 响应压缩
        // pipeline.addLast("handler", new MyHttpHandler());                 // 业务逻辑处理器
        
        // 注意：
        // 1. 编解码器的名称（"decoder", "encoder"）是可选的，但建议使用有意义的名称
        // 2. 后续可以通过名称来查找、替换或移除特定的处理器
        // 3. HTTP编解码器会产生HttpRequest/HttpResponse对象，而不是原始的ByteBuf
        // 4. 如果需要处理HTTP内容，可能还需要添加HttpObjectAggregator
    }
}
