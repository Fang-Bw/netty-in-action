package nia.chapter6;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

/**
 * 丢弃处理器 - 演示消息资源的释放
 * 
 * 这个处理器演示了如何正确释放接收到的消息资源，这是一个重要的内存管理概念。
 * 在Netty中，所有的消息都需要被显式释放，否则会导致内存泄漏。
 * 
 * 关键概念：
 * 1. 资源管理：Netty使用引用计数来管理内存
 * 2. 责任传递：如果不传递消息给下一个处理器，必须手动释放
 * 3. @Sharable注解：表示这个处理器可以在多个Channel之间安全共享
 * 4. 最佳实践：总是确保消息被正确释放
 * 
 * 使用场景：
 * - 需要丢弃某些消息的过滤器
 * - 简单的消息计数器
 * - 调试和监控工具
 * 
 * 代码清单 6.1 释放消息资源
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
@Sharable  // 标记此处理器可以在多个Channel之间安全共享
public class DiscardHandler extends ChannelInboundHandlerAdapter {

    /**
     * 处理接收到的消息
     * 
     * 在这个简单的实现中，我们选择丢弃所有接收到的消息。
     * 由于我们不会将消息传递给ChannelPipeline中的下一个处理器，
     * 因此我们有责任显式释放消息资源。
     * 
     * 重要提醒：
     * - 如果消息没有被传递给下一个处理器（通过ctx.fireChannelRead()），
     *   则当前处理器必须负责释放消息
     * - ReferenceCountUtil.release()是释放资源的便捷方法
     * - 释放已经被释放的对象是安全的（但访问已释放的对象会抛出异常）
     * 
     * @param ctx ChannelHandlerContext实例，提供处理器和管道之间的交互
     * @param msg 接收到的消息对象，通常是ByteBuf或其他引用计数对象
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // 释放接收到的消息
        // ReferenceCountUtil.release()会安全地释放引用计数对象
        // 如果msg不是引用计数对象，这个调用是无害的
        ReferenceCountUtil.release(msg);
        
        // 注意：我们没有调用ctx.fireChannelRead(msg)，
        // 这意味着消息不会传递给管道中的下一个处理器
        // 这就是为什么我们需要手动释放消息的原因
    }
}

