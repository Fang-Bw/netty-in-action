package nia.chapter7;

import io.netty.channel.Channel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 任务调度示例
 * 
 * 这个类演示了Netty中的任务调度机制，包括定时任务、延迟任务和周期性任务。
 * Netty的EventLoop不仅可以处理I/O事件，还可以作为高效的任务调度器使用。
 * 
 * 调度功能特点：
 * 1. 高精度：基于高精度的时间轮算法
 * 2. 低开销：与I/O事件处理共享同一个线程
 * 3. 线程安全：所有调度任务在EventLoop线程中执行
 * 4. 可取消：支持任务的取消和状态查询
 * 
 * 调度类型：
 * - 延迟执行：在指定延迟后执行一次
 * - 固定频率：以固定的时间间隔重复执行
 * - 固定延迟：每次执行完成后等待固定延迟再执行下次
 * 
 * 应用场景：
 * - 连接超时检测
 * - 心跳包发送
 * - 资源清理
 * - 性能统计
 * - 重试机制
 * 
 * 创建者：kerr
 */
public class ScheduleExamples {

    /**
     * 演示使用EventLoop进行任务调度
     * 
     * 这个方法展示了如何使用EventLoop的调度功能来执行延迟任务和周期性任务。
     * EventLoop实现了ScheduledExecutorService接口，提供了完整的调度功能。
     * 
     * 优势：
     * - 与I/O操作共享线程，避免线程切换开销
     * - 保证任务与I/O操作的执行顺序
     * - 简化并发编程，无需额外的同步机制
     * 
     * 代码清单 7.3 使用EventLoop调度任务
     */
    public static void scheduleViaEventLoop() {
        // 创建一个Channel实例
        Channel ch = new NioSocketChannel();
        
        // 创建一个延迟任务，60秒后执行
        ScheduledFuture<?> future = ch.eventLoop().schedule(
            new Runnable() {
                @Override
                public void run() {
                    // 这里执行延迟任务的逻辑
                    System.out.println("60 seconds later: " + 
                        Thread.currentThread().getName());
                    // 例如：检查连接状态、清理资源、发送心跳等
                }
            }, 
            60,                    // 延迟时间
            TimeUnit.SECONDS       // 时间单位
        );
        
        // 可以通过ScheduledFuture控制任务
        // future.cancel(false);  // 取消任务
        // future.isDone();       // 检查任务是否完成
        // future.isCancelled();  // 检查任务是否被取消
        
        // 创建一个周期性任务，每60秒执行一次
        ScheduledFuture<?> periodicFuture = ch.eventLoop().scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    // 这里执行周期性任务的逻辑
                    System.out.println("Periodic task executed: " + 
                        Thread.currentThread().getName());
                    // 例如：发送心跳包、统计性能数据、清理过期缓存等
                }
            },
            60,                    // 初始延迟
            60,                    // 执行间隔
            TimeUnit.SECONDS       // 时间单位
        );
        
        // 周期性任务会一直执行，直到被显式取消或EventLoop关闭
        // periodicFuture.cancel(false);  // 取消周期性任务
    }

    /**
     * 演示使用JDK ScheduledExecutorService进行任务调度
     * 
     * 这个方法展示了传统的Java任务调度方式，与Netty的EventLoop调度进行对比。
     * 虽然功能类似，但在Netty应用中，EventLoop调度通常是更好的选择。
     * 
     * JDK调度的特点：
     * - 独立的线程池，与I/O线程分离
     * - 适用于CPU密集型的定时任务
     * - 需要考虑线程安全和任务传递
     * 
     * 使用场景：
     * - 全局的定时任务（不依赖特定Channel）
     * - CPU密集型的周期性计算
     * - 需要独立线程池资源管理的场景
     * 
     * 代码清单 7.4 使用ScheduledExecutorService调度任务
     */
    public static void scheduleViaExecutor() {
        // 创建一个ScheduledExecutorService
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
        
        // 创建一个延迟任务
        ScheduledFuture<?> future = executor.schedule(
            new Runnable() {
                @Override
                public void run() {
                    // 这里执行延迟任务的逻辑
                    System.out.println("Scheduled task executed in thread pool: " + 
                        Thread.currentThread().getName());
                    
                    // 如果需要操作Netty的Channel，需要将结果传递回EventLoop
                    // channel.eventLoop().execute(() -> {
                    //     // 在EventLoop中执行Channel相关操作
                    // });
                }
            }, 
            60, 
            TimeUnit.SECONDS
        );
        
        // 创建一个周期性任务
        ScheduledFuture<?> periodicFuture = executor.scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    System.out.println("Periodic task in thread pool: " + 
                        Thread.currentThread().getName());
                    
                    // 执行独立于Netty的定时任务
                    // 例如：系统监控、日志归档、数据备份等
                }
            }, 
            60, 
            60, 
            TimeUnit.SECONDS
        );
        
        // 注意：使用完毕后需要关闭ExecutorService
        // executor.shutdown();
        
        // 对比总结：
        // EventLoop调度：
        // - 优点：与I/O操作线程安全集成，性能更好
        // - 缺点：与Channel生命周期绑定，不适合全局任务
        //
        // ScheduledExecutorService调度：
        // - 优点：独立的资源管理，适合全局和CPU密集型任务
        // - 缺点：需要额外的线程切换和同步开销
    }

    /**
     * 演示任务调度的最佳实践
     * 
     * 这个方法展示了在实际应用中如何合理使用任务调度，
     * 包括错误处理、资源管理和性能优化。
     */
    public static void schedulingBestPractices() {
        Channel ch = new NioSocketChannel();
        
        // 1. 带有错误处理的调度任务
        ScheduledFuture<?> robustTask = ch.eventLoop().scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        // 执行可能失败的任务逻辑
                        performPeriodicTask();
                    } catch (Exception e) {
                        // 记录异常，但不让异常终止周期性任务
                        System.err.println("Periodic task failed: " + e.getMessage());
                        // 在实际应用中，应该使用日志框架记录异常
                    }
                }
                
                private void performPeriodicTask() {
                    // 具体的任务逻辑
                    System.out.println("Performing periodic maintenance...");
                }
            },
            0,     // 立即开始
            30,    // 30秒间隔
            TimeUnit.SECONDS
        );
        
        // 2. 任务的优雅取消
        // 在应用关闭时或不再需要时，应该取消调度任务
        // robustTask.cancel(false);  // false表示不中断正在执行的任务
        
        // 3. 条件性调度 - 根据应用状态动态调度
        if (shouldEnableHeartbeat()) {
            ScheduledFuture<?> heartbeat = ch.eventLoop().scheduleAtFixedRate(
                () -> sendHeartbeat(),
                0, 30, TimeUnit.SECONDS
            );
            
            // 保存引用以便后续取消
            // this.heartbeatFuture = heartbeat;
        }
    }
    
    // 辅助方法示例
    private static boolean shouldEnableHeartbeat() {
        // 根据配置或应用状态决定是否启用心跳
        return true;
    }
    
    private static void sendHeartbeat() {
        // 发送心跳包的逻辑
        System.out.println("Sending heartbeat...");
    }
}
