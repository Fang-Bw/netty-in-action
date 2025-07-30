package nia.chapter7;

import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.Executors;

/**
 * EventLoop使用示例
 * 
 * 这个类演示了Netty中EventLoop的基本概念和使用方法。
 * EventLoop是Netty异步编程模型的核心，它负责处理Channel的所有I/O操作和事件。
 * 
 * EventLoop的核心概念：
 * 1. 事件循环：在一个循环中不断处理I/O事件和任务
 * 2. 线程绑定：每个EventLoop绑定到一个特定的线程
 * 3. Channel绑定：每个Channel在其生命周期内只与一个EventLoop绑定
 * 4. 任务执行：除了I/O事件，还可以执行普通的Runnable任务
 * 
 * EventLoop的优势：
 * - 线程安全：同一个Channel的所有操作都在同一个线程中执行
 * - 高性能：避免了线程切换和锁竞争的开销
 * - 简化编程：无需考虑并发同步问题
 * - 可扩展性：可以用少量线程处理大量连接
 * 
 * 使用场景：
 * - I/O事件处理（读、写、连接、关闭等）
 * - 定时任务执行
 * - 用户自定义任务执行
 * - 线程间任务传递
 * 
 * 创建者：kerr
 */
public class EventLoopExamples {
    
    /**
     * 演示在EventLoop中执行任务
     * 
     * 这个方法展示了如何在Channel的EventLoop中执行自定义任务。
     * 当需要在Channel的I/O线程中执行某些操作时，可以使用这种方式。
     * 
     * 关键概念：
     * - 线程安全：任务会在Channel的EventLoop线程中执行
     * - 异步执行：execute()方法立即返回，任务异步执行
     * - 顺序保证：任务按提交顺序执行
     * 
     * 使用场景：
     * - 需要在I/O线程中修改Channel状态
     * - 执行与Channel相关的业务逻辑
     * - 确保操作的线程安全性
     * 
     * 代码清单 7.1 在EventLoop中执行任务
     */
    public static void executeTaskInEventLoop() {
        // 创建一个Channel实例（这里使用NioSocketChannel作为示例）
        Channel channel = new NioSocketChannel();
        
        // 创建一个Runnable任务
        Runnable task = new Runnable() {
            @Override
            public void run() {
                // 这里执行需要在EventLoop中运行的任务
                // 例如：修改Channel的状态、执行业务逻辑等
                System.out.println("Task executed in EventLoop thread: " + 
                    Thread.currentThread().getName());
            }
        };
        
        // 将任务提交到Channel的EventLoop中执行
        // 这确保了任务在正确的线程中运行，保证线程安全
        channel.eventLoop().execute(task);
        
        // 重要提醒：
        // 1. execute()方法是异步的，立即返回
        // 2. 任务会在EventLoop的线程中按顺序执行
        // 3. 如果当前线程就是EventLoop线程，任务会被立即执行
        // 4. 如果当前线程不是EventLoop线程，任务会被加入队列等待执行
    }

    /**
     * 演示EventLoop线程检查
     * 
     * 这个方法展示了如何检查当前线程是否是EventLoop线程。
     * 这在编写线程安全的代码时非常有用，可以根据线程情况选择不同的执行策略。
     * 
     * 应用场景：
     * - 优化性能：如果已经在EventLoop线程中，可以直接执行操作
     * - 避免死锁：防止在EventLoop线程中调用阻塞操作
     * - 调试和监控：了解代码运行的线程环境
     * 
     * 代码清单 7.2 检查当前线程是否是EventLoop线程
     */
    public static void checkEventLoopThread() {
        // 创建一个Channel实例
        Channel channel = new NioSocketChannel();
        
        // 检查当前线程是否是这个Channel的EventLoop线程
        if (channel.eventLoop().inEventLoop()) {
            // 如果当前线程就是EventLoop线程，可以直接执行操作
            System.out.println("Already in EventLoop thread, executing directly");
            // 直接执行需要的操作...
        } else {
            // 如果当前线程不是EventLoop线程，需要提交任务到EventLoop
            System.out.println("Not in EventLoop thread, submitting task");
            channel.eventLoop().execute(new Runnable() {
                @Override
                public void run() {
                    // 在EventLoop线程中执行操作
                    System.out.println("Executing in EventLoop thread");
                }
            });
        }
        
        // 这种模式在Netty内部被广泛使用，用于确保线程安全
    }

    /**
     * 演示EventLoop与其他线程池的集成
     * 
     * 这个方法展示了EventLoop如何与传统的Java线程池协作。
     * 虽然EventLoop本身就是一个高效的任务执行器，但有时需要与其他组件集成。
     * 
     * 集成模式：
     * - EventLoop处理I/O密集型任务
     * - 传统线程池处理CPU密集型任务
     * - 通过任务传递实现协作
     * 
     * 注意事项：
     * - 避免在EventLoop中执行阻塞操作
     * - CPU密集型任务应该委托给专门的线程池
     * - 结果需要通过EventLoop返回给Channel
     */
    public static void eventLoopWithThreadPool() {
        // 创建一个Channel实例
        Channel channel = new NioSocketChannel();
        
        // 创建一个传统的Java线程池（用于CPU密集型任务）
        final var executor = Executors.newCachedThreadPool();
        
        // 在EventLoop中提交一个任务，该任务会将CPU密集型工作委托给线程池
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("Received task in EventLoop: " + 
                    Thread.currentThread().getName());
                
                // 将CPU密集型任务提交给线程池
                executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        // 执行CPU密集型操作（如复杂计算、加密等）
                        System.out.println("Executing CPU-intensive task in thread pool: " + 
                            Thread.currentThread().getName());
                        
                        // 模拟耗时操作
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                        // 任务完成后，将结果返回给EventLoop
                        channel.eventLoop().execute(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("CPU task completed, back to EventLoop: " + 
                                    Thread.currentThread().getName());
                                // 这里可以将结果写入Channel或执行其他I/O操作
                            }
                        });
                    }
                });
            }
        });
        
        // 这种模式确保了：
        // 1. I/O操作在EventLoop中执行，保证性能
        // 2. CPU密集型任务不会阻塞EventLoop
        // 3. 结果正确返回到EventLoop进行后续处理
    }
}
