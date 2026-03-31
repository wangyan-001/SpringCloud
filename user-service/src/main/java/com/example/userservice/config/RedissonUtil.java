package com.example.userservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

/**
 * Redisson 客户端单例工具类
 */
public class RedissonUtil {
    // 单例 Redisson 客户端
    private static volatile RedissonClient redissonClient;

    // 私有化构造方法，禁止外部实例化
    private RedissonUtil() {}

    /**
     * 获取 Redisson 客户端（懒加载 + 双重校验锁）
     */
    public static RedissonClient getRedissonClient() {
        if (redissonClient == null) {
            synchronized (RedissonUtil.class) {
                if (redissonClient == null) {
                    // 1. 创建配置（单机模式，根据实际环境调整）
                    Config config = new Config();
                    config.useSingleServer()
                            // Redis 地址（替换为你的 Redis 地址）
                            .setAddress("redis://localhost:6379")
                            // Redis 数据库（默认 0）
                            .setDatabase(0)
                            // Redis 密码（无密码则注释此行）
                            // .setPassword("your_redis_password")
                            // 连接池大小
                            .setConnectionPoolSize(20)
                            // 连接超时时间（毫秒）
                            .setConnectTimeout(5000);

                    // 2. 创建 Redisson 客户端
                    redissonClient = Redisson.create(config);
                }
            }
        }
        return redissonClient;
    }

    /**
     * 关闭 Redisson 客户端（程序退出时调用）
     */
    public static void closeRedissonClient() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
            System.out.println("Redisson 客户端已关闭");
        }
    }
}
