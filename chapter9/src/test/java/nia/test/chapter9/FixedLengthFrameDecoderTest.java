package nia.test.chapter9;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import nia.chapter9.FixedLengthFrameDecoder;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 固定长度帧解码器测试类
 * 
 * 这个测试类演示了如何使用Netty的测试框架来测试自定义的编解码器。
 * Netty提供了EmbeddedChannel类，专门用于测试ChannelHandler的功能。
 * 
 * EmbeddedChannel的特点：
 * 1. 嵌入式Channel：无需真实的网络连接
 * 2. 同步操作：所有操作都是同步的，便于测试
 * 3. 完整的Pipeline：支持完整的ChannelHandler链
 * 4. 状态可见：可以检查Channel的状态和数据
 * 5. 异常捕获：能够捕获和验证异常情况
 * 
 * 测试策略：
 * 1. 正常情况测试：验证基本功能是否正确
 * 2. 边界条件测试：测试各种边界情况
 * 3. 异常情况测试：验证错误处理逻辑
 * 4. 性能测试：检查性能指标
 * 5. 兼容性测试：确保与其他组件的兼容性
 * 
 * 单元测试的重要性：
 * - 确保编解码器的正确性
 * - 防止回归错误
 * - 文档化预期行为
 * - 提高代码质量和可维护性
 * 
 * 代码清单 9.3 测试FixedLengthFrameDecoder
 * 
 * @author <a href="mailto:norman.maurer@gmail.com">Norman Maurer</a>
 */
public class FixedLengthFrameDecoderTest {

    /**
     * 测试解码器的基本功能
     * 
     * 这个测试验证FixedLengthFrameDecoder能够正确地将字节流
     * 分割成固定长度的帧。测试覆盖了最基本的使用场景。
     * 
     * 测试场景：
     * 1. 创建固定长度为3字节的解码器
     * 2. 写入9字节的数据（正好3个完整帧）
     * 3. 验证输出了3个长度为3的帧
     * 4. 验证每个帧的内容正确
     * 
     * 代码清单 9.3 测试FixedLengthFrameDecoder
     */
    @Test
    public void testFramesDecoded() {
        // 创建测试数据
        // 这里创建了9个字节的数据：0, 1, 2, 3, 4, 5, 6, 7, 8
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }
        
        // 复制一份输入数据用于后续验证
        ByteBuf input = buf.duplicate();
        
        // 创建EmbeddedChannel并添加FixedLengthFrameDecoder
        // 帧长度设置为3字节，所以9字节数据应该被分成3帧
        EmbeddedChannel channel = new EmbeddedChannel(
            new FixedLengthFrameDecoder(3));
        
        // 模拟数据写入
        // writeInbound()模拟从网络接收数据的过程
        // 返回值表示是否有数据可以从inbound读取
        assertTrue(channel.writeInbound(input.retain()));
        
        // 标记Channel完成，触发最终的数据处理
        // 这确保所有缓冲的数据都被处理
        assertTrue(channel.finish());

        // 验证解码结果
        // readInbound()读取解码器输出的帧
        
        // 读取第一帧 (字节 0, 1, 2)
        ByteBuf read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);  // 验证内容正确
        read.release();  // 释放资源

        // 读取第二帧 (字节 3, 4, 5)
        read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);  // 验证内容正确
        read.release();  // 释放资源

        // 读取第三帧 (字节 6, 7, 8)
        read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);  // 验证内容正确
        read.release();  // 释放资源

        // 验证没有更多的帧
        assertNull(channel.readInbound());
        
        // 清理资源
        buf.release();
    }

    /**
     * 测试数据不足一个完整帧的情况
     * 
     * 这个测试验证当输入数据不足以组成一个完整帧时，
     * 解码器能够正确处理（即不输出任何帧，等待更多数据）。
     * 
     * 测试场景：
     * 1. 创建固定长度为3字节的解码器
     * 2. 写入2字节的数据（不足一个完整帧）
     * 3. 验证没有输出任何帧
     * 4. 再写入1字节数据（凑够一个完整帧）
     * 5. 验证输出了1个完整帧
     */
    @Test
    public void testFramesDecoded2() {
        // 创建测试数据：2个字节
        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 9; i++) {
            buf.writeByte(i);
        }
        ByteBuf input = buf.duplicate();

        // 创建EmbeddedChannel，帧长度为3字节
        EmbeddedChannel channel = new EmbeddedChannel(
            new FixedLengthFrameDecoder(3));

        // 第一次写入：只写入2字节（不足一个完整帧）
        // writeInbound()返回false，表示没有数据可读
        assertFalse(channel.writeInbound(input.readRetainedSlice(2)));
        
        // 验证确实没有数据可读
        assertNull(channel.readInbound());

        // 第二次写入：写入剩余的7字节
        // 这样总共有9字节，可以组成3个完整帧
        assertTrue(channel.writeInbound(input.readRetainedSlice(7)));

        // 完成Channel操作
        assertTrue(channel.finish());

        // 验证解码结果：应该有3个帧
        ByteBuf read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);
        read.release();

        read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);
        read.release();

        read = channel.readInbound();
        assertEquals(buf.readSlice(3), read);
        read.release();

        // 验证没有更多帧
        assertNull(channel.readInbound());
        buf.release();
    }
    
    // 扩展测试示例（实际项目中可能需要的其他测试）：
    
    /**
     * 测试边界条件：零长度帧
     * 
     * 虽然在构造函数中已经验证了帧长度必须大于0，
     * 但这里展示了如何测试异常情况。
     */
    @Test(expected = IllegalArgumentException.class)
    public void testZeroLengthFrame() {
        // 期望抛出IllegalArgumentException
        new FixedLengthFrameDecoder(0);
    }
    
    /**
     * 测试大量数据的处理
     * 
     * 验证解码器能够处理大量数据而不出现内存泄漏或性能问题。
     */
    @Test
    public void testLargeDataProcessing() {
        final int frameLength = 1024;
        final int frameCount = 1000;
        
        // 创建大量测试数据
        ByteBuf largeBuffer = Unpooled.buffer(frameLength * frameCount);
        for (int i = 0; i < frameLength * frameCount; i++) {
            largeBuffer.writeByte(i % 256);
        }
        
        EmbeddedChannel channel = new EmbeddedChannel(
            new FixedLengthFrameDecoder(frameLength));
        
        // 写入大量数据
        assertTrue(channel.writeInbound(largeBuffer.duplicate()));
        assertTrue(channel.finish());
        
        // 验证解码结果
        for (int i = 0; i < frameCount; i++) {
            ByteBuf frame = channel.readInbound();
            assertNotNull("Frame " + i + " should not be null", frame);
            assertEquals("Frame " + i + " should have correct length", 
                        frameLength, frame.readableBytes());
            frame.release();
        }
        
        // 验证没有更多帧
        assertNull(channel.readInbound());
        largeBuffer.release();
    }
    
    // 测试最佳实践：
    // 1. 总是测试正常情况和异常情况
    // 2. 验证内存管理（释放ByteBuf）
    // 3. 测试边界条件和极端情况
    // 4. 使用描述性的测试方法名
    // 5. 添加必要的注释说明测试意图
    // 6. 确保测试的独立性和可重复性
}
