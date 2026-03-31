package com.example.userservice.moniMQ;

import org.redisson.Redisson;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;


/*
* 优缺点
优点：
专门解决延迟 / 定时消息场景（如订单超时关闭、定时任务）；
实现简单，基于 Redis 原生有序集合。
缺点：
需消费者轮询，存在一定性能损耗；
无 ACK 机制，消费失败易丢失；
不支持复杂的消息过滤、重试策略。
适用场景
延迟任务、定时任务（如订单 30 分钟未支付自动关闭、优惠券到期提醒）。
* */

public class RedissonDelayQueueMain {
    private static final String DELAY_QUEUE_KEY = "msg:delay:queue";
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    //private static final String REDIS_PASSWORD = "";
    private static final int REDIS_DB = 0;

    private static RedissonClient initRedissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + REDIS_HOST + ":" + REDIS_PORT)
                //.setPassword(REDIS_PASSWORD)
                .setDatabase(REDIS_DB);
        return Redisson.create(config);
    }

    private static void addDelayMsg(RedissonClient redissonClient, String msg, long delaySeconds) {
        RScoredSortedSet<String> delayQueue = redissonClient.getScoredSortedSet(DELAY_QUEUE_KEY);
        long ts = System.currentTimeMillis() + delaySeconds * 1000L;
        delayQueue.add(ts, msg);
        System.out.println("添加：" + msg + " " + delaySeconds + "s");
    }

    private static void startConsume(RedissonClient redissonClient) {
        new Thread(() -> {
            RScoredSortedSet<String> queue = redissonClient.getScoredSortedSet(DELAY_QUEUE_KEY);
            while (!Thread.currentThread().isInterrupted()) {
                long now = System.currentTimeMillis();
                Iterator<String> it = queue.valueRange(0, false, now, true, 0, 1).iterator();
                if (it.hasNext()) {
                    String msg = it.next();
                    if (queue.remove(msg)) {
                        System.out.println("消费：" + msg);
                    }
                }
                try { TimeUnit.MILLISECONDS.sleep(500); }
                catch (InterruptedException e) { break; }
            }
            System.out.println("消费线程退出");
        }).start();
    }

    public static void main(String[] args) throws InterruptedException {
        RedissonClient redisson = initRedissonClient();
        System.out.println("Redisson 初始化完成");

        // 1. 先启动消费者
        startConsume(redisson);
        TimeUnit.SECONDS.sleep(1);

        // 2. 再生产消息
        addDelayMsg(redisson, "订单超时关闭", 3);
        addDelayMsg(redisson, "优惠券到期", 5);
        addDelayMsg(redisson, "退款处理", 8);

        System.out.println("生产完成，等待消费...");

        // 3. 延迟20秒再关闭，确保消费完
        TimeUnit.SECONDS.sleep(20);
        redisson.shutdown();
        System.out.println("关闭完成");
    }
}