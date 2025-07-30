package nia.chapter10;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * 整数到字符串编码器
 * 
 * 这个类演示了如何实现一个将Java对象（Integer）编码为另一种Java对象（String）的编码器。
 * 它继承自MessageToMessageEncoder，这是Netty提供的用于消息到消息编码的基类。
 * 
 * MessageToMessageEncoder vs MessageToByteEncoder：
 * - MessageToMessageEncoder：输出Java对象，需要后续编码器将对象转换为字节
 * - MessageToByteEncoder：直接输出字节，可以直接写入网络
 * 
 * 编码器设计模式：
 * 1. 单向转换：将一种类型的消息转换为另一种类型
 * 2. 链式处理：可以与其他编码器组成处理链
 * 3. 类型安全：通过泛型确保处理正确的消息类型
 * 4. 无状态设计：避免在实例中保存状态信息
 * 
 * MessageToMessageEncoder的特点：
 * - 泛型支持：只处理指定类型的消息
 * - 自动过滤：自动跳过不匹配类型的消息
 * - 异常处理：提供统一的异常处理机制
 * - 内存管理：自动处理引用计数对象的生命周期
 * 
 * 应用场景：
 * - 协议转换：在不同协议格式之间转换
 * - 数据格式化：将内部对象转换为传输格式
 * - 序列化预处理：在序列化前进行数据转换
 * - 多层编码：作为编码链中的一环
 * 
 * 编码器链示例：
 * Object -> String -> JSON -> ByteBuf -> Network
 *         ↑ 本编码器  ↑ JSON编码器  ↑ 字节编码器
 * 
 * 代码清单 10.3 IntegerToStringEncoder
 *
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class IntegerToStringEncoder extends MessageToMessageEncoder<Integer> {

    /**
     * 编码方法 - 将Integer对象转换为String对象
     * 
     * 这个方法是编码器的核心，负责将输入的Integer对象转换为String对象。
     * MessageToMessageEncoder会确保只有Integer类型的消息会传递到这个方法。
     * 
     * 编码过程说明：
     * 1. 接收一个Integer对象作为输入
     * 2. 将Integer转换为其字符串表示形式
     * 3. 将转换后的String添加到输出列表中
     * 4. 输出的String可以被后续的编码器进一步处理
     * 
     * 类型安全性：
     * - 泛型参数<Integer>确保只有Integer类型的消息会被处理
     * - 其他类型的消息会被自动跳过，传递给下一个处理器
     * - 编译时类型检查，减少运行时错误
     * 
     * 性能考虑：
     * - String.valueOf()是高效的转换方式
     * - 避免了手动的字符串构建和格式化
     * - 对于空值的处理是安全的
     * 
     * @param ctx ChannelHandlerContext，提供对Channel和Pipeline的访问
     * @param msg 待编码的Integer消息对象
     * @param out 输出列表，用于存放编码后的消息对象
     * @throws Exception 编码过程中可能抛出的异常
     */
    @Override
    public void encode(ChannelHandlerContext ctx, Integer msg,
                      List<Object> out) throws Exception {
        // 将Integer转换为String
        // String.valueOf()的优势：
        // 1. 处理null值安全（会返回"null"字符串）
        // 2. 性能优化的实现
        // 3. 标准的转换方式，易于理解和维护
        String stringValue = String.valueOf(msg);
        
        // 将转换后的String添加到输出列表
        // 这个String将被传递给Pipeline中的下一个处理器
        out.add(stringValue);
        
        // 处理流程说明：
        // 1. 输入：Integer对象（例如：42）
        // 2. 转换：调用String.valueOf(42)
        // 3. 输出：String对象（"42"）
        // 4. 后续：String可能被进一步编码为JSON、XML或直接转换为字节
    }
    
    // 扩展示例：带格式化的编码器
    /**
     * 格式化编码示例（替代实现）
     * 
     * 这个示例展示了如何创建更复杂的编码逻辑，
     * 例如添加格式化、前缀、后缀等。
     */
    public void encodeWithFormat(ChannelHandlerContext ctx, Integer msg,
                                List<Object> out) throws Exception {
        // 创建格式化的字符串
        String formatted;
        
        if (msg == null) {
            formatted = "NULL";
        } else if (msg >= 0) {
            formatted = "POSITIVE:" + msg;
        } else {
            formatted = "NEGATIVE:" + Math.abs(msg);
        }
        
        out.add(formatted);
        
        // 这种方式适用于：
        // 1. 需要特定格式的协议
        // 2. 添加元数据或标识符
        // 3. 条件性的格式化逻辑
        // 4. 复杂的业务规则
    }
    
    // 批量编码示例
    /**
     * 批量编码示例
     * 
     * 如果需要处理List<Integer>这样的集合类型，
     * 可以创建专门的批量编码器。
     */
    public static class IntegerListToStringEncoder 
        extends MessageToMessageEncoder<List<Integer>> {
        
        @Override
        protected void encode(ChannelHandlerContext ctx, 
                            List<Integer> msg, List<Object> out) throws Exception {
            // 将整数列表转换为逗号分隔的字符串
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < msg.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(msg.get(i));
            }
            out.add(sb.toString());
        }
    }
    
    // 错误处理
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 记录编码错误
        // logger.error("Integer to String encoding failed", cause);
        
        // 编码错误的处理策略：
        // 1. 数据错误：跳过当前消息，继续处理
        // 2. 系统错误：传播异常，可能需要关闭连接
        // 3. 配置错误：记录详细信息，便于调试
        
        // 默认处理：传播异常
        super.exceptionCaught(ctx, cause);
    }
    
    // 使用建议：
    // 1. 编码器应该是无状态的，避免实例变量
    // 2. 考虑使用@Sharable注解来重用编码器实例
    // 3. 对于复杂的转换逻辑，考虑单独的转换工具类
    // 4. 注意处理null值和边界条件
    // 5. 在高性能场景下，考虑对象池化和缓存策略
}

