package nia.chapter6;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelPipeline;

import static io.netty.channel.DummyChannelPipeline.DUMMY_INSTANCE;

/**
 * ChannelPipeline修改操作示例
 * 
 * 这个类演示了如何在运行时动态修改ChannelPipeline的结构。
 * ChannelPipeline是一个处理器链，可以在运行时动态地添加、删除和替换处理器。
 * 
 * ChannelPipeline的核心概念：
 * 1. 处理器链：由多个ChannelHandler组成的有序链表
 * 2. 双向传播：支持入站和出站事件的传播
 * 3. 动态修改：可以在运行时添加、删除、替换处理器
 * 4. 线程安全：所有修改操作都是线程安全的
 * 
 * 常用操作：
 * - addFirst/addLast：在管道的开头/结尾添加处理器
 * - addBefore/addAfter：在指定处理器前/后添加处理器
 * - remove：移除指定的处理器
 * - replace：替换指定的处理器
 * 
 * 应用场景：
 * - 协议切换（如HTTP升级到WebSocket）
 * - 动态压缩/解压缩
 * - 条件性处理器（根据配置动态添加）
 * - 调试和监控（临时添加日志处理器）
 * 
 * 代码清单 6.5 修改ChannelPipeline
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class ModifyChannelPipeline {
    // 示例用的ChannelPipeline实例
    private static final ChannelPipeline CHANNEL_PIPELINE_FROM_SOMEWHERE = DUMMY_INSTANCE;

    /**
     * 演示ChannelPipeline的各种修改操作
     * 
     * 这个方法展示了ChannelPipeline支持的主要修改操作：
     * 1. 添加处理器：addFirst、addLast
     * 2. 移除处理器：remove（通过名称或引用）
     * 3. 替换处理器：replace
     * 
     * 重要提醒：
     * - 每个ChannelHandler都可以有一个名称，用于标识和查找
     * - 如果不指定名称，Netty会自动生成一个
     * - 处理器的顺序很重要，影响事件的处理流程
     * - 修改操作是线程安全的，可以在任何时候进行
     * 
     * 代码清单 6.5 修改ChannelPipeline
     */
    public static void modifyPipeline() {
        // 获取ChannelPipeline引用（从某处得到）
        ChannelPipeline pipeline = CHANNEL_PIPELINE_FROM_SOMEWHERE; // get reference to pipeline;
        
        // 创建第一个处理器实例，保留引用以便后续操作
        FirstHandler firstHandler = new FirstHandler();
        
        // 在管道末尾添加处理器，指定名称为"handler1"
        pipeline.addLast("handler1", firstHandler);
        
        // 在管道开头添加处理器，指定名称为"handler2"
        // 现在的顺序是：handler2 -> handler1
        pipeline.addFirst("handler2", new SecondHandler());
        
        // 在管道末尾再添加一个处理器，指定名称为"handler3"
        // 现在的顺序是：handler2 -> handler1 -> handler3
        pipeline.addLast("handler3", new ThirdHandler());
        
        //...执行其他业务逻辑
        
        // 通过名称移除处理器
        // 移除"handler3"，现在的顺序是：handler2 -> handler1
        pipeline.remove("handler3");
        
        // 通过处理器实例引用移除处理器
        // 移除firstHandler，现在的顺序是：handler2
        pipeline.remove(firstHandler);
        
        // 替换处理器：将"handler2"替换为"handler4"
        // 新处理器使用FourthHandler实例，名称改为"handler4"
        // 现在的顺序是：handler4
        pipeline.replace("handler2", "handler4", new FourthHandler());
        
        // 其他可用的操作（未在此示例中展示）：
        // - addBefore(baseName, name, handler)：在指定处理器前添加
        // - addAfter(baseName, name, handler)：在指定处理器后添加
        // - get(name)：获取指定名称的处理器
        // - context(name)：获取指定名称处理器的上下文
        // - names()：获取所有处理器的名称列表
    }

    /**
     * 第一个示例处理器
     * 
     * 继承自ChannelHandlerAdapter，这是一个适配器类，
     * 提供了ChannelHandler接口的默认实现。
     */
    private static final class FirstHandler
        extends ChannelHandlerAdapter {
        // 这里可以重写需要的方法来实现具体的处理逻辑
    }

    /**
     * 第二个示例处理器
     */
    private static final class SecondHandler
        extends ChannelHandlerAdapter {
        // 这里可以重写需要的方法来实现具体的处理逻辑
    }

    /**
     * 第三个示例处理器
     */
    private static final class ThirdHandler
        extends ChannelHandlerAdapter {
        // 这里可以重写需要的方法来实现具体的处理逻辑
    }

    /**
     * 第四个示例处理器
     */
    private static final class FourthHandler
        extends ChannelHandlerAdapter {
        // 这里可以重写需要的方法来实现具体的处理逻辑
    }
}
