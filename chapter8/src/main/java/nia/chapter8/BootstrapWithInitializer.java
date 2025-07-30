package nia.chapter8;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

import java.net.InetSocketAddress;

/**
 * 使用ChannelInitializer的Bootstrap示例
 * 
 * 这个类演示了如何使用ChannelInitializer来配置复杂的ChannelPipeline。
 * ChannelInitializer是一个特殊的ChannelHandler，专门用于初始化新创建的Channel。
 * 
 * ChannelInitializer的优势：
 * 1. 简化配置：提供清晰的Pipeline配置方式
 * 2. 延迟初始化：在Channel实际创建时才配置Pipeline
 * 3. 类型安全：支持泛型，确保类型正确性
 * 4. 自动移除：完成初始化后自动从Pipeline中移除
 * 5. 复用性：可以在多个Bootstrap中重用相同的初始化逻辑
 * 
 * 使用场景：
 * - 复杂的协议栈配置（HTTP、SSL、自定义协议等）
 * - 条件性的处理器添加
 * - 需要传递参数给处理器的情况
 * - 多个处理器的组合配置
 * 
 * 工作原理：
 * 1. ChannelInitializer被添加到Bootstrap的handler或childHandler中
 * 2. 当新Channel创建时，initChannel()方法被调用
 * 3. 在initChannel()中配置完整的ChannelPipeline
 * 4. 配置完成后，ChannelInitializer自动从Pipeline中移除
 * 
 * 代码清单 8.6 引导和使用ChannelInitializer
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class BootstrapWithInitializer {

    /**
     * 演示使用ChannelInitializer配置服务器
     * 
     * 这个方法展示了如何使用ChannelInitializer来配置复杂的服务器Pipeline。
     * 与直接在Bootstrap中配置处理器相比，ChannelInitializer提供了更好的
     * 组织方式和更强的可维护性。
     * 
     * 注意事项：
     * - 这里使用了两个EventLoopGroup，这是服务器的推荐配置
     * - 第一个group处理连接接受，第二个group处理I/O操作
     * - ChannelInitializer只在Channel创建时执行一次
     * 
     * 代码清单 8.6 引导和使用ChannelInitializer
     * 
     * @throws InterruptedException 如果等待绑定过程中被中断
     */
    public void bootstrap() throws InterruptedException {
        // 创建ServerBootstrap实例
        ServerBootstrap bootstrap = new ServerBootstrap();
        
        // 配置ServerBootstrap
        bootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())  // 配置父子EventLoopGroup
            .channel(NioServerSocketChannel.class)                         // 设置ServerChannel类型
            .childHandler(new ChannelInitializerImpl());                   // 使用自定义的ChannelInitializer
        
        // 绑定到端口并同步等待绑定完成
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080));
        future.sync();  // 阻塞等待绑定完成
        
        // 在实际应用中，通常还需要：
        // future.channel().closeFuture().sync(); // 等待服务器关闭
        // 并在finally块中优雅关闭EventLoopGroup
    }

    /**
     * 自定义的ChannelInitializer实现
     * 
     * 这个内部类展示了如何扩展ChannelInitializer来配置特定的ChannelPipeline。
     * 通过继承ChannelInitializer并重写initChannel()方法，可以为每个新创建的
     * Channel配置完整的处理器链。
     * 
     * 设计要点：
     * - 继承ChannelInitializer<Channel>，指定Channel类型
     * - 在initChannel()方法中添加所需的处理器
     * - 处理器的添加顺序很重要，影响数据处理流程
     * - 可以根据条件动态添加不同的处理器
     */
    final class ChannelInitializerImpl extends ChannelInitializer<Channel> {
        
        /**
         * 初始化Channel的Pipeline
         * 
         * 这个方法在每个新Channel创建时被调用，用于配置该Channel的
         * ChannelPipeline。方法执行完毕后，ChannelInitializer会自动
         * 从Pipeline中移除。
         * 
         * Pipeline配置示例：
         * 这里配置了一个HTTP处理链，包含：
         * 1. HttpClientCodec：HTTP协议编解码器
         * 2. HttpObjectAggregator：HTTP消息聚合器
         * 
         * 实际应用中可能包含：
         * - SSL/TLS处理器（SslHandler）
         * - 压缩处理器（HttpContentCompressor）
         * - 日志处理器（LoggingHandler）
         * - 自定义业务处理器
         * - 异常处理器
         * 
         * @param ch 要初始化的Channel
         * @throws Exception 初始化过程中可能抛出的异常
         */
        @Override
        protected void initChannel(Channel ch) throws Exception {
            // 获取Channel的Pipeline
            ChannelPipeline pipeline = ch.pipeline();
            
            // 添加HTTP客户端编解码器
            // HttpClientCodec结合了HttpRequestEncoder和HttpResponseDecoder
            // 注意：这里使用HttpClientCodec可能是示例代码的笔误，
            // 服务器端通常应该使用HttpServerCodec
            pipeline.addLast(new HttpClientCodec());
            
            // 添加HTTP对象聚合器
            // HttpObjectAggregator将HTTP消息的多个部分聚合成一个完整的消息
            // Integer.MAX_VALUE设置了最大聚合大小，实际应用中应该设置合理的限制
            pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
            
            // 在实际应用中，这里还可以添加更多处理器：
            // pipeline.addLast(new SslHandler(...));                    // SSL加密
            // pipeline.addLast(new HttpContentCompressor());            // 响应压缩  
            // pipeline.addLast(new LoggingHandler(LogLevel.INFO));      // 日志记录
            // pipeline.addLast(new MyCustomHandler());                  // 自定义处理器
            // pipeline.addLast(new MyBusinessLogicHandler());           // 业务逻辑
            
            // ChannelInitializer的优势体现：
            // 1. 清晰的结构：Pipeline配置集中在一个方法中
            // 2. 参数传递：可以通过构造函数传递配置参数
            // 3. 条件配置：可以根据条件动态配置不同的处理器
            // 4. 代码复用：同一个Initializer可以用于多个Bootstrap
        }
    }
    
    // 使用ChannelInitializer的最佳实践：
    // 
    // 1. 参数化配置：
    // public class ConfigurableChannelInitializer extends ChannelInitializer<Channel> {
    //     private final boolean enableSsl;
    //     private final boolean enableCompression;
    //     
    //     public ConfigurableChannelInitializer(boolean enableSsl, boolean enableCompression) {
    //         this.enableSsl = enableSsl;
    //         this.enableCompression = enableCompression;
    //     }
    //     
    //     @Override
    //     protected void initChannel(Channel ch) {
    //         ChannelPipeline pipeline = ch.pipeline();
    //         
    //         if (enableSsl) {
    //             pipeline.addLast(new SslHandler(...));
    //         }
    //         
    //         pipeline.addLast(new HttpServerCodec());
    //         
    //         if (enableCompression) {
    //             pipeline.addLast(new HttpContentCompressor());
    //         }
    //         
    //         pipeline.addLast(new BusinessHandler());
    //     }
    // }
    //
    // 2. 模块化配置：
    // 将不同功能的处理器配置分离到不同的方法中，提高可维护性
    //
    // 3. 异常处理：
    // 在initChannel()中添加适当的异常处理逻辑
    //
    // 4. 资源管理：
    // 确保在处理器中正确管理资源，特别是引用计数对象
}
