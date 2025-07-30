package nia.chapter6;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * 可共享的ChannelHandler示例
 * 
 * 这个处理器演示了@Sharable注解的使用和意义。
 * @Sharable注解表示这个ChannelHandler可以在多个Channel或多个ChannelPipeline之间安全地共享。
 * 
 * @Sharable的重要概念：
 * 1. 线程安全：标记为@Sharable的处理器必须是线程安全的
 * 2. 无状态：通常应该是无状态的，或者状态是线程安全的
 * 3. 性能优化：可以减少对象创建，提高性能
 * 4. 资源共享：可以在多个连接之间共享昂贵的资源
 * 
 * 使用@Sharable的条件：
 * - 处理器不包含任何实例变量（无状态）
 * - 或者所有实例变量都是线程安全的
 * - 或者使用适当的同步机制保护共享状态
 * 
 * 典型应用场景：
 * - 日志记录处理器
 * - 统计计数处理器（使用原子变量）
 * - 协议编解码器（无状态）
 * - 简单的消息转发处理器
 * 
 * 代码清单 6.10 可共享的ChannelHandler
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
@Sharable  // 标记此处理器可以在多个Channel之间安全共享
public class SharableHandler extends ChannelInboundHandlerAdapter {
    
    /**
     * 处理接收到的消息
     * 
     * 这个处理器展示了一个典型的可共享处理器的实现：
     * 1. 不包含任何实例变量（无状态）
     * 2. 只执行简单的操作（日志记录）
     * 3. 将消息传递给下一个处理器
     * 
     * 线程安全性分析：
     * - 没有实例变量，因此不存在共享状态
     * - 只使用方法参数和局部变量
     * - 所有操作都是线程安全的
     * 
     * @param ctx ChannelHandlerContext实例，提供与管道交互的方法
     * @param msg 接收到的消息对象
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 记录接收到的消息
        // 这是一个线程安全的操作，因为：
        // 1. System.out.println是线程安全的
        // 2. 参数msg是方法局部变量，每个线程都有独立的副本
        System.out.println("channel read message " + msg);
        
        // 将消息传递给管道中的下一个处理器
        // ctx.fireChannelRead(msg)是线程安全的操作
        // 这确保了处理链能够继续执行
        ctx.fireChannelRead(msg);
    }
    
    // 关键要点：
    // 1. 这个处理器没有任何实例变量，因此是无状态的
    // 2. 所有操作都基于方法参数，不涉及共享状态
    // 3. 可以安全地在多个Channel之间共享同一个实例
    // 4. 这样可以节省内存，提高性能
    
    // 反例：如果有实例变量，则不应标记为@Sharable
    // private int messageCount; // 这样的字段使处理器不再线程安全
    
    // 如果确实需要共享状态，应该使用线程安全的方式：
    // private final AtomicInteger messageCount = new AtomicInteger(0);
}

