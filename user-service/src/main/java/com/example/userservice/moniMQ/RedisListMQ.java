package com.example.userservice.moniMQ;

import com.example.userservice.config.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import java.util.List;


/* Redis 队列 MQ 用List实现
* 优缺点
优点：
实现简单，Redis 原生支持，无需额外依赖；
阻塞读取（brpop/blpop）避免空轮询，性能高；
天然支持 FIFO，符合基本队列特性。
缺点：
不支持消息持久化配置外的可靠性保障（如消费失败无重试、消息丢失）；
不支持广播 / 订阅模式，一条消息只能被一个消费者消费；
无消息确认机制（ACK），消费端崩溃会导致消息丢失；
无法做消息过滤、延迟队列。
适用场景
轻量级、低可靠性要求的场景（如日志收集、临时任务分发）。
* */


public class RedisListMQ {
    // 队列 KEY
    private static final String QUEUE_KEY = "msg:queue:list";

    // 生产者：往队列尾部添加消息
    public static void producer(String msg) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            // rpush：从右侧（尾部）入队
            jedis.rpush(QUEUE_KEY, msg);
            System.out.println("生产消息：" + msg);
        } catch (JedisException e) {
            System.err.println("生产消息失败：" + e.getMessage());
        }
    }

    // 消费者：阻塞式从队列头部取消息
    public static void consumer() {
        try (Jedis jedis = RedisUtil.getJedis()) {
            while (true) {
                // blpop(超时时间, 队列名)：阻塞式左弹出，超时时间 0 表示永久阻塞
                // 返回值：List[0] = 队列名，List[1] = 消息内容
                List<String> result = jedis.blpop(0, QUEUE_KEY);
                if (result != null && result.size() >= 2) {
                    String msg = result.get(1);
                    System.out.println("消费消息：" + msg);
                    // 模拟业务处理
                    Thread.sleep(1000);
                }
            }
        } catch (JedisException | InterruptedException e) {
            System.err.println("消费消息失败：" + e.getMessage());
        }
    }

    // 测试
    public static void main(String[] args) {
        // 启动生产者（生产 5 条消息）
        new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                producer("test_msg_" + i);
            }
        }).start();

        // 启动消费者
        new Thread(RedisListMQ::consumer).start();
    }
}
