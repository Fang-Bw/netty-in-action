package nia.chapter5;

import io.netty.buffer.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ByteProcessor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Random;

import static io.netty.channel.DummyChannelHandlerContext.DUMMY_INSTANCE;

/**
 * ByteBuf操作示例集合
 * 
 * 这个类包含了Netty中ByteBuf的各种使用示例，演示了：
 * 1. 堆缓冲区（Heap Buffer）的使用
 * 2. 直接缓冲区（Direct Buffer）的使用
 * 3. 复合缓冲区（Composite Buffer）的使用
 * 4. ByteBuf的读写操作
 * 5. ByteBuf的分片和复制
 * 6. ByteBuf的引用计数机制
 * 
 * ByteBuf的核心概念：
 * - ByteBuf是Netty的字节容器，比JDK的ByteBuffer更强大
 * - 支持动态扩容，读写指针分离
 * - 提供引用计数机制，避免内存泄漏
 * - 支持零拷贝操作，提高性能
 * 
 * 创建者：kerr
 * 
 * 代码清单 5.1  支撑数组
 * 代码清单 5.2  直接缓冲区数据访问
 * 代码清单 5.3  使用ByteBuffer的复合缓冲区模式
 * 代码清单 5.4  使用CompositeByteBuf的复合缓冲区模式
 * 代码清单 5.5  访问CompositeByteBuf中的数据
 * 代码清单 5.6  访问数据
 * 代码清单 5.7  读取所有数据
 * 代码清单 5.8  写数据
 * 代码清单 5.9  使用ByteProcessor查找\r
 * 代码清单 5.10 对ByteBuf进行切片
 * 代码清单 5.11 复制ByteBuf
 * 代码清单 5.12 get()和set()的使用
 * 代码清单 5.13 ByteBuf上的read()和write()操作
 * 代码清单 5.14 获取ByteBufAllocator的引用
 * 代码清单 5.15 引用计数
 * 代码清单 5.16 释放引用计数的对象
 */
public class ByteBufExamples {
    // 随机数生成器，用于示例中的随机数据
    private final static Random random = new Random();
    // 示例用的ByteBuf实例
    private static final ByteBuf BYTE_BUF_FROM_SOMEWHERE = Unpooled.buffer(1024);
    // 示例用的Channel实例
    private static final Channel CHANNEL_FROM_SOMEWHERE = new NioSocketChannel();
    // 示例用的ChannelHandlerContext实例
    private static final ChannelHandlerContext CHANNEL_HANDLER_CONTEXT_FROM_SOMEWHERE = DUMMY_INSTANCE;
    
    /**
     * 堆缓冲区示例
     * 
     * 堆缓冲区将数据存储在JVM的堆空间中，特点：
     * 1. 由支撑数组提供支持，可以快速分配和释放
     * 2. 当数据没有存储在直接内存中时提供快速的数据访问
     * 3. 可以通过hasArray()方法检查是否有支撑数组
     * 4. 如果有支撑数组，可以直接访问底层数组获得更好的性能
     * 
     * 使用场景：
     * - 需要快速访问数据的场景
     * - 数据处理和转换
     * - 临时缓冲区
     * 
     * 代码清单 5.1 支撑数组
     */
    public static void heapBuffer() {
        // 获取ByteBuf引用（从某处得到）
        ByteBuf heapBuf = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        
        // 检查ByteBuf是否有一个支撑数组
        if (heapBuf.hasArray()) {
            // 如果有，则获取对该数组的直接引用
            byte[] array = heapBuf.array();
            
            // 计算第一个字节的偏移量
            // arrayOffset()返回数组中第一个字节的偏移量
            // readerIndex()返回当前读指针位置
            int offset = heapBuf.arrayOffset() + heapBuf.readerIndex();
            
            // 获得可读字节数
            int length = heapBuf.readableBytes();
            
            // 使用数组、偏移量和长度作为参数调用你的方法
            handleArray(array, offset, length);
        }
    }

    /**
     * 直接缓冲区示例
     * 
     * 直接缓冲区将数据存储在堆外内存中，特点：
     * 1. 内存分配和释放开销较大
     * 2. 在Socket I/O操作中性能更好（避免了数据复制）
     * 3. 不会有支撑数组，需要通过其他方式访问数据
     * 4. 对于网络数据传输是理想的选择
     * 
     * 使用场景：
     * - 网络传输
     * - 大数据处理
     * - 需要避免堆内存压力的场景
     * 
     * 代码清单 5.2 直接缓冲区数据访问
     */
    public static void directBuffer() {
        // 获取ByteBuf引用（从某处得到）
        ByteBuf directBuf = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        
        // 检查ByteBuf是否不是由数组支撑。如果不是，则这是一个直接缓冲区
        if (!directBuf.hasArray()) {
            // 获得可读字节数
            int length = directBuf.readableBytes();
            
            // 分配一个新的数组来保存具有该长度的字节
            byte[] array = new byte[length];
            
            // 将字节复制到该数组
            directBuf.getBytes(directBuf.readerIndex(), array);
            
            // 使用数组、偏移量和长度作为参数调用你的方法
            handleArray(array, 0, length);
        }
    }

    /**
     * 使用JDK ByteBuffer的复合缓冲区模式
     * 
     * 传统的JDK ByteBuffer处理复合数据（如消息头+消息体）时的局限性：
     * 1. 需要创建数组来管理多个ByteBuffer
     * 2. 或者需要分配新的ByteBuffer并复制数据
     * 3. 这些方式要么复杂，要么低效
     * 
     * 这个示例展示了传统方式的两种处理方法：
     * 1. 使用数组管理多个ByteBuffer
     * 2. 通过复制创建新的ByteBuffer
     * 
     * 代码清单 5.3 使用ByteBuffer的复合缓冲区模式
     * 
     * @param header 消息头ByteBuffer
     * @param body 消息体ByteBuffer
     */
    public static void byteBufferComposite(ByteBuffer header, ByteBuffer body) {
        // 使用数组来保存消息的各个部分
        // 这种方式需要在发送时遍历数组
        ByteBuffer[] message =  new ByteBuffer[]{ header, body };

        // 创建一个新的ByteBuffer并使用copy来合并header和body
        // 这种方式需要分配内存并复制数据，效率较低
        ByteBuffer message2 =
                ByteBuffer.allocate(header.remaining() + body.remaining());
        message2.put(header);
        message2.put(body);
        message2.flip();
    }

    /**
     * 使用Netty CompositeByteBuf的复合缓冲区模式
     * 
     * CompositeByteBuf为多个ByteBuf提供了一个聚合视图，解决了传统方式的问题：
     * 1. 可以动态地添加或者删除ByteBuf实例
     * 2. 对外提供统一的ByteBuf接口
     * 3. 避免了数据复制，实现零拷贝
     * 4. 支持迭代器模式遍历组件
     * 
     * 优势：
     * - 零拷贝：不需要复制数据
     * - 动态组合：可以随时添加/删除组件
     * - 统一接口：对外表现为单一的ByteBuf
     * 
     * 代码清单 5.4 使用CompositeByteBuf的复合缓冲区模式
     */
    public static void byteBufComposite() {
        // 创建CompositeByteBuf实例
        CompositeByteBuf messageBuf = Unpooled.compositeBuffer();
        
        // 获取header和body的ByteBuf（可以是堆缓冲区也可以是直接缓冲区）
        ByteBuf headerBuf = BYTE_BUF_FROM_SOMEWHERE; // can be backing or direct
        ByteBuf bodyBuf = BYTE_BUF_FROM_SOMEWHERE;   // can be backing or direct
        
        // 将ByteBuf实例追加到CompositeByteBuf
        messageBuf.addComponents(headerBuf, bodyBuf);
        
        //...进行其他操作
        
        // 删除位于索引位置0的ByteBuf（移除header）
        messageBuf.removeComponent(0); // remove the header
        
        // 循环遍历所有的ByteBuf实例
        for (ByteBuf buf : messageBuf) {
            System.out.println(buf.toString());
        }
    }

    /**
     * 访问CompositeByteBuf中的数据
     * 
     * CompositeByteBuf提供了多种访问其数据的方式：
     * 1. 可以像普通ByteBuf一样使用各种get/set方法
     * 2. 可以将数据复制到字节数组中
     * 3. 内部会自动处理跨组件的访问
     * 
     * 这个示例展示了如何将CompositeByteBuf的数据复制到字节数组中。
     * 
     * 代码清单 5.5 访问CompositeByteBuf中的数据
     */
    public static void byteBufCompositeArray() {
        // 创建CompositeByteBuf
        CompositeByteBuf compBuf = Unpooled.compositeBuffer();
        
        // 获得可读字节数
        int length = compBuf.readableBytes();
        
        // 分配一个新的数组来保存这些字节
        byte[] array = new byte[length];
        
        // 将字节读到该数组中
        // getBytes()方法会自动处理跨多个组件的读取
        compBuf.getBytes(compBuf.readerIndex(), array);
        
        // 使用字节数组
        handleArray(array, 0, array.length);
    }

    /**
     * ByteBuf随机访问示例
     * 
     * 这个方法演示了ByteBuf的随机访问功能：
     * 1. 使用get方法可以访问指定索引位置的数据
     * 2. get操作不会改变readerIndex或writerIndex
     * 3. 可以重复读取相同位置的数据
     * 4. 索引从0开始，到capacity()-1结束
     * 
     * get vs read方法的区别：
     * - get方法：不改变读写指针，可以随机访问
     * - read方法：会移动读指针，顺序访问
     * 
     * 代码清单 5.6 访问数据
     */
    public static void byteBufRelativeAccess() {
        // 获取ByteBuf引用（从某处得到）
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        
        // 循环访问ByteBuf中的每个字节
        for (int i = 0; i < buffer.capacity(); i++) {
            // 获取索引i处的字节，不会改变读写指针
            byte b = buffer.getByte(i);
            // 将字节转换为字符并打印
            System.out.println((char) b);
        }
    }

    /**
     * 读取所有数据示例
     * 
     * 这个方法演示了如何顺序读取ByteBuf中的所有可读数据：
     * 1. 使用isReadable()检查是否还有可读数据
     * 2. 使用readByte()读取数据并移动读指针
     * 3. 读操作会改变readerIndex
     * 4. 当readerIndex等于writerIndex时，isReadable()返回false
     * 
     * 重要概念：
     * - readerIndex：当前读位置
     * - writerIndex：当前写位置
     * - 可读字节数 = writerIndex - readerIndex
     * 
     * 代码清单 5.7 读取所有数据
     */
    public static void readAllData() {
        // 获取ByteBuf引用（从某处得到）
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        
        // 循环读取直到没有更多可读字节
        while (buffer.isReadable()) {
            // 读取一个字节并移动读指针
            System.out.println(buffer.readByte());
        }
    }

    /**
     * 写入数据示例
     * 
     * 这个方法演示了如何向ByteBuf写入数据：
     * 1. 使用writableBytes()检查可写空间
     * 2. 使用write方法写入数据并移动写指针
     * 3. 写操作会改变writerIndex
     * 4. 如果空间不足，某些ByteBuf实现会自动扩容
     * 
     * 重要概念：
     * - 可写字节数 = capacity - writerIndex
     * - 动态扩容：某些ByteBuf会在空间不足时自动扩容
     * - 写入类型：支持各种基本数据类型的写入
     * 
     * 代码清单 5.8 写数据
     */
    public static void write() {
        // 用随机整数填充缓冲区的可写字节
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        
        // 当有足够空间写入一个int（4字节）时继续写入
        while (buffer.writableBytes() >= 4) {
            // 写入一个随机整数，会移动写指针4个位置
            buffer.writeInt(random.nextInt());
        }
    }

    /**
     * 使用ByteProcessor查找特定字节
     * 
     * ByteProcessor提供了一种高效的方式来搜索ByteBuf中的特定模式：
     * 1. 可以避免手动循环查找
     * 2. 性能更好，内部优化
     * 3. 支持各种预定义的查找模式
     * 4. 可以自定义查找逻辑
     * 
     * 常用的ByteProcessor：
     * - FIND_CR：查找回车符(\r)
     * - FIND_LF：查找换行符(\n)
     * - FIND_CRLF：查找回车换行符(\r\n)
     * 
     * 注意：此方法适用于Netty 4.0.x版本
     * 代码清单 5.9 使用ByteProcessor查找\r
     *
     * 使用 {@link io.netty.buffer.ByteBufProcessor in Netty 4.0.x}
     */
    public static void byteProcessor() {
        // 获取ByteBuf引用（从某处得到）
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        
        // 查找第一个回车符(\r)的位置
        // 返回索引位置，如果未找到则返回-1
        int index = buffer.forEachByte(ByteProcessor.FIND_CR);
    }

    /**
     * 使用ByteBufProcessor查找特定字节（Netty 4.1.x版本）
     * 
     * 这是Netty 4.1.x版本中的ByteProcessor使用方式，
     * 功能与4.0.x版本相同，但是包路径有所变化。
     * 
     * 代码清单 5.9 使用ByteProcessor查找\r
     *
     * 使用 {@link io.netty.util.ByteProcessor in Netty 4.1.x}
     */
    public static void byteBufProcessor() {
        // 获取ByteBuf引用（从某处得到）
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        
        // 查找第一个回车符(\r)的位置
        int index = buffer.forEachByte(ByteBufProcessor.FIND_CR);
    }

    /**
     * ByteBuf分片操作示例
     * 
     * 分片(slice)操作创建原ByteBuf的一个子区域视图：
     * 1. 与原ByteBuf共享相同的存储区域
     * 2. 拥有独立的读写指针
     * 3. 对分片的修改会影响到原ByteBuf
     * 4. 零拷贝操作，不复制数据
     * 
     * 分片的特点：
     * - 共享数据：分片和原ByteBuf共享底层数据
     * - 独立索引：有自己的readerIndex和writerIndex
     * - 引用计数：分片会增加原ByteBuf的引用计数
     * - 范围限制：只能访问指定范围内的数据
     * 
     * 代码清单 5.10 对ByteBuf进行切片
     */
    public static void byteBufSlice() {
        // 创建UTF-8字符集
        Charset utf8 = Charset.forName("UTF-8");
        
        // 创建一个包含字符串的ByteBuf
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        
        // 创建一个从索引0开始长度为15的分片
        // 这个分片与原ByteBuf共享数据
        ByteBuf sliced = buf.slice(0, 15);
        
        // 打印分片内容："Netty in Action"
        System.out.println(sliced.toString(utf8));
        
        // 修改原ByteBuf的第一个字节
        buf.setByte(0, (byte)'J');
        
        // 验证分片也被修改了，因为它们共享底层数据
        assert buf.getByte(0) == sliced.getByte(0);
    }

    /**
     * ByteBuf复制操作示例
     * 
     * 复制(copy)操作创建原ByteBuf的一个独立副本：
     * 1. 拥有独立的存储区域
     * 2. 拥有独立的读写指针
     * 3. 对副本的修改不会影响到原ByteBuf
     * 4. 需要分配新内存并复制数据
     * 
     * copy vs slice的区别：
     * - copy：创建独立的数据副本，互不影响
     * - slice：共享底层数据，修改会相互影响
     * - copy：消耗更多内存，但更安全
     * - slice：零拷贝，但需要注意共享数据的风险
     * 
     * 代码清单 5.11 复制ByteBuf
     */
    public static void byteBufCopy() {
        // 创建UTF-8字符集
        Charset utf8 = Charset.forName("UTF-8");
        
        // 创建一个包含字符串的ByteBuf
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        
        // 创建一个从索引0开始长度为15的副本
        // 这个副本有自己独立的数据存储
        ByteBuf copy = buf.copy(0, 15);
        
        // 打印副本内容："Netty in Action"
        System.out.println(copy.toString(utf8));
        
        // 修改原ByteBuf的第一个字节
        buf.setByte(0, (byte)'J');
        
        // 验证副本没有被修改，因为它们有独立的数据存储
        assert buf.getByte(0) != copy.getByte(0);
    }

    /**
     * get()和set()方法使用示例
     * 
     * 这个方法演示了ByteBuf的get()和set()操作特点：
     * 1. get()和set()操作不会改变读写指针位置
     * 2. 可以在任意位置读取和修改数据
     * 3. 支持随机访问模式
     * 4. 适合需要多次访问相同位置数据的场景
     * 
     * get/set vs read/write的区别：
     * - get/set：不移动指针，支持随机访问
     * - read/write：移动指针，支持顺序访问
     * - get/set：适合索引访问和就地修改
     * - read/write：适合流式数据处理
     * 
     * 代码清单 5.12 get()和set()的使用
     */
    public static void byteBufSetGet() {
        // 创建UTF-8字符集
        Charset utf8 = Charset.forName("UTF-8");
        
        // 创建包含文本的ByteBuf
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        
        // 获取索引0位置的字节并打印字符：'N'
        System.out.println((char)buf.getByte(0));
        
        // 记录当前的读写指针位置
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();
        
        // 在索引0位置设置新值
        buf.setByte(0, (byte)'B');
        
        // 获取修改后的值并打印字符：'B'
        System.out.println((char)buf.getByte(0));
        
        // 验证读写指针位置没有改变
        assert readerIndex == buf.readerIndex();
        assert writerIndex == buf.writerIndex();
    }

    /**
     * read()和write()操作示例
     * 
     * 这个方法演示了ByteBuf的read()和write()操作特点：
     * 1. read()操作会移动readerIndex
     * 2. write()操作会移动writerIndex
     * 3. 支持顺序访问模式
     * 4. 适合流式数据处理
     * 
     * 指针移动规则：
     * - readByte()：readerIndex + 1
     * - writeByte()：writerIndex + 1
     * - 类似地，readInt()会移动4个位置，writeInt()也会移动4个位置
     * 
     * 代码清单 5.13 ByteBuf上的read()和write()操作
     */
    public static void byteBufWriteRead() {
        // 创建UTF-8字符集
        Charset utf8 = Charset.forName("UTF-8");
        
        // 创建包含文本的ByteBuf
        ByteBuf buf = Unpooled.copiedBuffer("Netty in Action rocks!", utf8);
        
        // 读取第一个字节并移动读指针，打印字符：'N'
        System.out.println((char)buf.readByte());
        
        // 记录当前的读写指针位置（此时readerIndex已经移动了）
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();
        
        // 写入一个字节并移动写指针
        buf.writeByte((byte)'?');
        
        // 验证读指针没有改变（因为我们只进行了写操作）
        assert readerIndex == buf.readerIndex();
        // 验证写指针已经改变（因为我们进行了写操作）
        assert writerIndex != buf.writerIndex();
    }

    /**
     * 辅助方法，用于处理字节数组
     * 在实际应用中，这里会包含具体的数组处理逻辑
     * 
     * @param array 字节数组
     * @param offset 偏移量
     * @param len 长度
     */
    private static void handleArray(byte[] array, int offset, int len) {}

    /**
     * 获取ByteBufAllocator引用示例
     * 
     * ByteBufAllocator是用于分配ByteBuf的工厂：
     * 1. 可以从Channel或ChannelHandlerContext获取
     * 2. 提供了池化和非池化的ByteBuf分配
     * 3. 支持堆缓冲区和直接缓冲区的分配
     * 4. Netty会根据平台和配置选择最优的分配策略
     * 
     * 分配器类型：
     * - PooledByteBufAllocator：池化分配器，重用ByteBuf实例
     * - UnpooledByteBufAllocator：非池化分配器，每次创建新实例
     * - 默认使用池化分配器以获得更好的性能
     * 
     * 代码清单 5.14 获取ByteBufAllocator的引用
     */
    public static void obtainingByteBufAllocatorReference(){
        // 从Channel获取ByteBufAllocator
        Channel channel = CHANNEL_FROM_SOMEWHERE; //get reference form somewhere
        ByteBufAllocator allocator = channel.alloc();
        //...可以使用allocator分配ByteBuf
        
        // 从ChannelHandlerContext获取ByteBufAllocator
        ChannelHandlerContext ctx = CHANNEL_HANDLER_CONTEXT_FROM_SOMEWHERE; //get reference form somewhere
        ByteBufAllocator allocator2 = ctx.alloc();
        //...可以使用allocator2分配ByteBuf
    }

    /**
     * 引用计数示例
     * 
     * Netty使用引用计数来管理ByteBuf的生命周期：
     * 1. 新创建的ByteBuf引用计数为1
     * 2. retain()方法增加引用计数
     * 3. release()方法减少引用计数
     * 4. 当引用计数为0时，ByteBuf被释放
     * 
     * 引用计数的意义：
     * - 防止内存泄漏：确保ByteBuf最终被释放
     * - 安全共享：多个组件可以安全地共享同一个ByteBuf
     * - 自动管理：不需要手动跟踪ByteBuf的使用情况
     * - 性能优化：支持对象池化和重用
     * 
     * 代码清单 5.15 引用计数
     */
    public static void referenceCounting(){
        // 获取Channel和分配器
        Channel channel = CHANNEL_FROM_SOMEWHERE; //get reference form somewhere
        ByteBufAllocator allocator = channel.alloc();
        //...
        
        // 分配一个直接缓冲区
        ByteBuf buffer = allocator.directBuffer();
        
        // 验证新分配的ByteBuf引用计数为1
        assert buffer.refCnt() == 1;
        //...使用buffer进行操作
    }

    /**
     * 释放引用计数对象示例
     * 
     * 正确释放ByteBuf是避免内存泄漏的关键：
     * 1. 每个retain()调用都应该有对应的release()调用
     * 2. release()返回true表示对象已被完全释放
     * 3. 试图访问已释放的ByteBuf会抛出异常
     * 4. Netty提供了内存泄漏检测来帮助发现问题
     * 
     * 最佳实践：
     * - 谁分配谁释放：分配ByteBuf的组件负责释放
     * - 使用try-finally：确保release()在异常情况下也会被调用
     * - 引用传递：当传递ByteBuf给其他组件时，考虑是否需要retain()
     * - 检测工具：使用Netty的内存泄漏检测工具
     * 
     * 代码清单 5.16 释放引用计数的对象
     */
    public static void releaseReferenceCountedObject(){
        // 获取ByteBuf引用（从某处得到）
        ByteBuf buffer = BYTE_BUF_FROM_SOMEWHERE; //get reference form somewhere
        
        // 释放引用计数
        // 返回true表示这是最后一个引用，对象已被完全释放
        // 返回false表示还有其他引用，对象尚未释放
        boolean released = buffer.release();
        //...
    }


}
