package com.example.userservice.moniMQ;

import org.redisson.Redisson;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/*
*Stream 实现消息队列（推荐方案）
原理
Redis 5.0 新增的 Stream 类型，是专门为消息队列设计的结构，支持：
消息持久化、ACK 确认机制；
消费者组（Consumer Group），实现消息分片消费（避免重复消费）；
消息回溯、未确认消息重试；
阻塞读取、消息 ID 自增。
*
* 如果你的场景对消息可靠性要求高，优先选择 Stream；
* 如果只是简单通知 / 广播，用 Pub/Sub；延迟任务用 Sorted Set；极简队列用 List。
*
* 、总结：Stream 消费者会丢消息吗？
默认情况（仅基础消费）：可能丢（未开启持久化、未确认、无重试）；
正确使用机制：几乎不会丢（开启持久化 + 消费确认 + 重试 + 认领死信），能满足生产环境「高可靠性」要求；
核心对比：
List 队列：无确认机制，消费崩溃必丢；
Pub/Sub：无持久化，离线必丢；
Stream：多层保障，是 Redis 所有消息队列方案中可靠性最高的。
*
* XRANGE my-stream - + 查询所有的消息
* Del my-stream 删除
* */


public class RedisStreamFullDemo {
    // Redis 配置（根据你的实际环境修改）
   // private static final String REDIS_ADDRESS = "redis://127.0.0.1:6379";
    private static final String STREAM_KEY = "my-stream";
    private static final String GROUP_NAME = "group-1";
    private static final String CONSUMER_NAME = "consumer-1";

    public static void main(String[] args) throws InterruptedException {
        // 1. 初始化 Redisson 客户端
        RedissonClient redissonClient = initRedisson();

        try {
            // 2. 创建 Stream 和消费组（确保消费组存在）
            RStream<String, String> stream = redissonClient.getStream(STREAM_KEY);
            //创建消费组（不存在则创建）
            createConsumerGroupIfNotExist(stream);

            // 3. 启动生产者线程（持续生产消息）
            Thread producerThread = startProducer(stream);

            // 4. 启动消费者线程（持续消费消息）
            Thread consumerThread = startConsumer(stream);

            // 5. 主线程运行 20 秒后停止
            System.out.println("程序启动，将运行20秒后自动退出...");
            TimeUnit.SECONDS.sleep(20);

            // 6. 优雅关闭：先停生产者 → 再停消费者 → 最后关客户端
            System.out.println("\n开始优雅关闭程序...");
            stopProducer(producerThread);
            stopConsumer(consumerThread);
        } finally {
            // 确保 Redisson 最终关闭
            redissonClient.shutdown();
            System.out.println("Redisson 客户端已关闭");

            // 可选：清空 Stream 所有消息（测试完清理）
            // clearStream(redissonClient);
        }
    }

    /**
     * 初始化 Redisson 客户端
     */
    private static RedissonClient initRedisson() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setConnectionPoolSize(10)
                .setConnectionMinimumIdleSize(5)
                .setConnectTimeout(3000)
                .setRetryAttempts(3);
        return Redisson.create(config);
    }
    /**
     * 创建消费组（不存在则创建）
     */
    private static void createConsumerGroupIfNotExist(RStream<String, String> stream) {
        try {
            stream.createGroup(GROUP_NAME);
            System.out.println("消费组 " + GROUP_NAME + " 创建成功（或已存在）");
        } catch (Exception e) {
            System.out.println("消费组已存在，无需重复创建");
        }
    }

    /**
     * 启动生产者线程（每秒生产1条消息）
     */
    private static Thread startProducer(RStream<String, String> stream) {
        Thread producerThread = new Thread(() -> {
            int msgCount = 1;
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // 构建消息内容
                    Map<String, String> msg = new HashMap<>();
                    msg.put("content", "测试消息-" + msgCount);
                    msg.put("timestamp", String.valueOf(System.currentTimeMillis()));

                    // 发送消息到 Stream
                   // StreamMessageId msgId = stream.add(msg);

                    // 正确写法：3.23.2 必须用 StreamAddArgs
                    StreamMessageId msgId = stream.add(
                            StreamAddArgs.entries(msg)
                    );

                    System.out.println("生产者发送消息：ID=" + msgId + "，内容=" + msg);

                    msgCount++;
                    TimeUnit.SECONDS.sleep(1); // 每秒1条
                }
            } catch (InterruptedException e) {
                System.out.println("生产者线程被中断，停止生产");
            } catch (Exception e) {
                System.err.println("生产者异常：" + e.getMessage());
            }
        }, "stream-producer");

        producerThread.start();
        return producerThread;
    }

    /**
     * 启动消费者线程（持续消费消息）
     */
    private static Thread startConsumer(RStream<String, String> stream) {
        Thread consumerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // 读取消费组消息（阻塞1秒，避免空轮询）
                    Map<StreamMessageId, Map<String, String>> messages = stream.readGroup(
                            GROUP_NAME, CONSUMER_NAME, 10, // 最多读10条
                            1000, // 阻塞1秒
                            TimeUnit.MILLISECONDS
                    );

                    // 处理消息
                    for (Map.Entry<StreamMessageId, Map<String, String>> entry : messages.entrySet()) {
                        StreamMessageId msgId = entry.getKey();
                        Map<String, String> msg = entry.getValue();
                        System.out.println("消费者消费消息：ID=" + msgId + "，内容=" + msg);

                        // 确认消息消费完成（关键：避免重复消费）
                        stream.ack(GROUP_NAME, msgId);
                    }
                }
            } catch (Exception e) {
                if (!(e instanceof InterruptedException)) {
                    System.err.println("消费者异常：" + e.getMessage());
                } else {
                    System.out.println("消费者线程被中断，停止消费");
                }
            }
        }, "stream-consumer");

        consumerThread.start();
        return consumerThread;
    }

    /**
     * 停止生产者线程
     */
    private static void stopProducer(Thread producerThread) throws InterruptedException {
        producerThread.interrupt();
        producerThread.join(2000); // 等待2秒确保停止
        System.out.println("生产者已停止");
    }

    /**
     * 停止消费者线程
     */
    private static void stopConsumer(Thread consumerThread) throws InterruptedException {
        consumerThread.interrupt();
        consumerThread.join(2000); // 等待2秒确保停止
        System.out.println("消费者已停止");
    }

    /**
     * 清空 Stream 所有消息（测试完清理用）
     */
    private static void clearStream(RedissonClient redissonClient) {
        RStream<String, String> stream = redissonClient.getStream(STREAM_KEY);
        stream.delete();
        System.out.println("Stream " + STREAM_KEY + " 已清空");
    }
}