package com.example.userservice.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class RedisUtil {
    // 静态连接池（单例）
    private static final JedisPool JEDIS_POOL;

    static {
        // 连接池配置
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20); // 最大连接数
        poolConfig.setMaxIdle(10);  // 最大空闲连接
        poolConfig.setMinIdle(5);   // 最小空闲连接

        // 初始化连接池（替换为你的 Redis 地址/密码）
        JEDIS_POOL = new JedisPool(poolConfig, "localhost", 6379, Protocol.DEFAULT_TIMEOUT, null, 0);
    }

    // 获取 Jedis 连接
    public static Jedis getJedis() {
        return JEDIS_POOL.getResource();
    }

    // 关闭连接池（程序退出时调用）
    public static void closePool() {
        if (JEDIS_POOL != null) {
            JEDIS_POOL.close();
        }
    }
}
