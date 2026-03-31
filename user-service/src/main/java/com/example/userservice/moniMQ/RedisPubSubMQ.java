package com.example.userservice.moniMQ;

import com.example.userservice.config.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;


/*
* 优缺点
优点：
天然支持一对多广播，适合消息通知、实时推送场景；
实现简单，发布订阅模型轻量。
缺点：
无消息持久化：消费者离线期间发布的消息会丢失；
无 ACK 机制，消息一旦发布无法确认是否被消费；
不支持消息回溯、延迟队列；
性能一般，高并发下不如 List 或 Stream。
适用场景
实时通知、广播类场景（如聊天室、系统告警推送），不要求消息可靠投递。
* */

public class RedisPubSubMQ {
    // 频道名称
    private static final String CHANNEL_NAME = "msg:channel:test";

    // 生产者：发布消息到频道
    public static void producer(String msg) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            // publish：发布消息到指定频道
            long count = jedis.publish(CHANNEL_NAME, msg);
            System.out.println("发布消息到频道 " + CHANNEL_NAME + "：" + msg + "，订阅者数量：" + count);
        } catch (JedisException e) {
            System.err.println("发布消息失败：" + e.getMessage());
        }
    }

    // 消费者：订阅频道并消费消息
    public static void consumer(String consumerName) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            // 自定义订阅回调
            JedisPubSub pubSub = new JedisPubSub() {
                // 收到消息时触发
                @Override
                public void onMessage(String channel, String message) {
                    System.out.println("消费者 " + consumerName + " 收到消息：" + message);
                }

                // 订阅成功时触发
                @Override
                public void onSubscribe(String channel, int subscribedChannels) {
                    System.out.println("消费者 " + consumerName + " 已订阅频道 " + channel);
                }
            };

            // 订阅频道（阻塞方法，会一直监听）
            jedis.subscribe(pubSub, CHANNEL_NAME);
        } catch (JedisException e) {
            System.err.println("订阅失败：" + e.getMessage());
        }
    }

    // 测试
    public static void main(String[] args) throws InterruptedException {

        // 启动 2 个消费者（模拟广播）
       new Thread(() -> consumer("consumer_1")).start();
       new Thread(() -> consumer("consumer_2")).start();

        // 等待消费者订阅完成
        Thread.sleep(1000);

        // 生产者发布 3 条消息
        for (int i = 0; i < 3; i++) {
            producer("broadcast_msg_" + i);
            Thread.sleep(500);
        }

    }


}