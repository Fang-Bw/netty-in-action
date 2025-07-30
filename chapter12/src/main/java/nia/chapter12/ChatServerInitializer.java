package nia.chapter12;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * 聊天服务器ChannelPipeline初始化器
 * 
 * 这个类负责为每个新连接配置ChannelPipeline，建立完整的HTTP/WebSocket处理链。
 * 处理链的设计支持HTTP和WebSocket双协议，实现从HTTP到WebSocket的无缝升级。
 * 
 * Pipeline处理流程：
 * 1. HttpServerCodec：HTTP请求/响应编解码
 * 2. ChunkedWriteHandler：支持大文件的分块传输
 * 3. HttpObjectAggregator：聚合HTTP消息片段
 * 4. HttpRequestHandler：处理HTTP请求和静态文件服务
 * 5. WebSocketServerProtocolHandler：处理WebSocket协议升级
 * 6. TextWebSocketFrameHandler：处理WebSocket文本消息
 * 
 * 关键特性：
 * - 自动协议升级：从HTTP平滑过渡到WebSocket
 * - 静态文件服务：提供聊天界面的HTML页面
 * - 消息广播：支持多客户端之间的实时通信
 * - 动态Pipeline：根据协议状态动态调整处理器
 * 
 * 代码清单 12.3 初始化ChannelPipeline
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class ChatServerInitializer extends ChannelInitializer<Channel> {
    // ChannelGroup用于管理所有连接的WebSocket客户端
    private final ChannelGroup group;

    /**
     * 构造函数
     * 
     * @param group ChannelGroup实例，用于管理所有WebSocket连接
     */
    public ChatServerInitializer(ChannelGroup group) {
        this.group = group;
    }

    /**
     * 初始化新连接的ChannelPipeline
     * 
     * 这个方法为每个新建立的连接配置完整的处理管道。
     * 处理器的添加顺序很重要，因为它决定了数据处理的流向。
     * 
     * 处理链详解：
     * 1. HttpServerCodec：处理HTTP协议的编解码
     * 2. ChunkedWriteHandler：支持分块写入，用于大文件传输
     * 3. HttpObjectAggregator：将HTTP消息片段聚合成完整消息
     * 4. HttpRequestHandler：处理HTTP请求，提供静态资源
     * 5. WebSocketServerProtocolHandler：处理WebSocket握手和协议升级
     * 6. TextWebSocketFrameHandler：处理WebSocket文本帧消息
     * 
     * @param ch 新建立的Channel
     * @throws Exception 初始化过程中可能抛出的异常
     */
    @Override
    protected void initChannel(Channel ch) throws Exception {
        // 获取Channel的Pipeline
        ChannelPipeline pipeline = ch.pipeline();
        
        // 添加HTTP服务器编解码器
        // HttpServerCodec是HttpRequestDecoder和HttpResponseEncoder的组合
        // 负责将字节流解码为HTTP请求，将HTTP响应编码为字节流
        pipeline.addLast(new HttpServerCodec());
        
        // 添加分块写入处理器
        // ChunkedWriteHandler支持异步写入大型数据流
        // 特别适用于文件传输，避免占用过多内存
        pipeline.addLast(new ChunkedWriteHandler());
        
        // 添加HTTP对象聚合器
        // HttpObjectAggregator将HttpMessage和HttpContent聚合为FullHttpRequest
        // 64*1024是最大聚合大小（64KB），防止过大的HTTP请求消耗内存
        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
        
        // 添加HTTP请求处理器
        // 处理普通HTTP请求，提供静态文件服务（如聊天页面）
        // "/ws"是WebSocket升级的URI路径
        pipeline.addLast(new HttpRequestHandler("/ws"));
        
        // 添加WebSocket服务器协议处理器
        // 自动处理WebSocket握手过程和协议升级
        // 成功升级后会触发握手完成事件
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
        
        // 添加WebSocket文本帧处理器
        // 处理WebSocket连接中的文本消息
        // 负责消息的接收、广播和连接管理
        pipeline.addLast(new TextWebSocketFrameHandler(group));
    }
}
