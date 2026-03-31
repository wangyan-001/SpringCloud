package com.example.userservice.Redis;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存击穿、穿透、雪崩 模拟与解决方案
 * 可直接运行 main 方法测试
 */
public class RedisCacheProblemDemo {

    // Redis 连接配置
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    //private static final String REDIS_PASSWORD = "";
    private static final int REDIS_DB = 0;

    // Redisson 分布式锁配置（用于解决缓存击穿）
    private static RedissonClient redissonClient;

    // 模拟数据库（实际项目中是 MySQL/DB）
    private static class MockDB {
        /**
         * 模拟从数据库查询数据
         * @param key 查询键
         * @return 数据（不存在返回 null）
         */
        public static String queryData(String key) {
            // 模拟数据库查询耗时（100ms）
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 模拟数据：只有 key 为 "hot_key" 和 "normal_key_1" 存在数据
            if ("hot_key".equals(key)) {
                return "hot_value_123456";
            } else if (key.startsWith("normal_key_")) {
                return "normal_value_" + key.split("_")[2];
            } else {
                return null; // 不存在的 key 返回 null
            }
        }
    }

    static {
        // 初始化 Redisson 客户端
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + REDIS_HOST + ":" + REDIS_PORT)
                //.setPassword(REDIS_PASSWORD.isEmpty() ? null : REDIS_PASSWORD)
                .setDatabase(REDIS_DB);
        redissonClient = Redisson.create(config);
    }

    public static void main(String[] args) throws InterruptedException {
        Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT);
        try  {
            // Redis 认证
            /*if (!REDIS_PASSWORD.isEmpty()) {
                jedis.auth(REDIS_PASSWORD);
            }*/
            jedis.select(REDIS_DB);


            // 1. 测试缓存穿透
            System.out.println("===== 1. 测试缓存穿透 =====");
            //测试缓存穿透（请求不存在的 key）解决方案：空值缓存 + 布隆过滤器(未实现)
            testCachePenetration(jedis);

            // 2. 测试缓存击穿
            System.out.println("\n===== 2. 测试缓存击穿 =====");
            //测试缓存击穿（热点 key 过期，大量并发请求） 解决方案：分布式锁
            testCacheBreakdown(jedis);

            // 3. 测试缓存雪崩
            System.out.println("\n===== 3. 测试缓存雪崩 =====");
            //测试缓存雪崩（大量 key 同时过期）解决方案：过期时间随机化
            testCacheAvalanche(jedis);


        } catch (JedisConnectionException e) {
            System.err.println("Redis 连接异常：" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Redis 操作异常：" + e.getMessage());
            e.printStackTrace();
        } finally {
            // 安全关闭 Jedis 连接
            if (jedis != null) {
                try {
                    jedis.close();
                } catch (JedisConnectionException e) {
                    System.err.println("关闭 Redis 连接失败：" + e.getMessage());
                }
            }
            // 关闭 Redisson 客户端
            if (redissonClient != null) {
                redissonClient.shutdown();
            }
        }
    }

    // ====================== 缓存穿透 ======================
    /**
     * 测试缓存穿透（请求不存在的 key）
     * 解决方案：空值缓存 + 布隆过滤器(未实现)
     */
    private static void testCachePenetration(Jedis jedis) {
        String nonExistentKey = "non_existent_key_9999";

        // ---------- 无防护的情况 ----------
        System.out.println("【无防护】查询不存在的 key: " + nonExistentKey);
        long start = System.currentTimeMillis();
        String value = getValueWithoutPenetrationProtect(jedis, nonExistentKey);
        long cost = System.currentTimeMillis() - start;
        System.out.println("无防护查询结果: " + value + "，耗时: " + cost + "ms");

        // ---------- 有防护的情况（空值缓存） ----------
        System.out.println("\n【有防护】查询不存在的 key: " + nonExistentKey);
        start = System.currentTimeMillis();
        value = getValueWithPenetrationProtect(jedis, nonExistentKey);
        cost = System.currentTimeMillis() - start;
        System.out.println("有防护第一次查询结果: " + value + "，耗时: " + cost + "ms");

        // 第二次查询（走空值缓存）
        start = System.currentTimeMillis();
        value = getValueWithPenetrationProtect(jedis, nonExistentKey);
        cost = System.currentTimeMillis() - start;
        System.out.println("有防护第二次查询结果: " + value + "，耗时: " + cost + "ms");

        // 清理测试数据
       // jedis.del(nonExistentKey);
    }

    /**
     * 无缓存穿透防护的查询方法
     */
    private static String getValueWithoutPenetrationProtect(Jedis jedis, String key) {
        // 1. 查缓存
        String value = jedis.get(key);
        if (value != null) {
            return value;
        }
        // 2. 缓存不存在，查数据库
        value = MockDB.queryData(key);
        // 3. 数据库也不存在，直接返回 null（请求直击数据库）
        return value;
    }

    /**
     * 有缓存穿透防护的查询方法（空值缓存）
     */
    private static String getValueWithPenetrationProtect(Jedis jedis, String key) {
        // 1. 查缓存
        String value = jedis.get(key);
        if (value != null) {
            // 空值标记：用 "" 表示不存在的 key
            return "".equals(value) ? null : value;
        }
        // 2. 缓存不存在，查数据库
        value = MockDB.queryData(key);
        if (value == null) {
            // 3. 数据库也不存在，设置空值缓存（过期时间 5 分钟）
            jedis.setex(key, 300, "");
            return null;
        }
        // 4. 数据库存在，设置缓存
        jedis.setex(key, 3600, value);
        return value;
    }

    // ====================== 缓存击穿 ======================
    /**
     * 测试缓存击穿（热点 key 过期，大量并发请求）
     * 解决方案：分布式锁
     */
    private static void testCacheBreakdown(Jedis jedis) throws InterruptedException {
        String hotKey = "hot_key";
        // 1. 先设置热点 key 并手动过期（模拟过期瞬间）
        jedis.setex(hotKey, 1, "hot_value_123456");
        Thread.sleep(800); // 等待缓存过期

        // 2. 模拟 100 个并发请求查询热点 key
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        long start = System.currentTimeMillis();

        System.out.println("开始模拟 " + threadCount + " 个并发请求查询过期的热点 key...");

        // ---------- 无防护的情况 ----------

        for (int i = 0; i < threadCount; i++) {
            CountDownLatch finalLatch = latch;
            executor.submit(() -> {
                try {
                    getValueWithoutBreakdownProtect(jedis, hotKey);
                } finally {
                    finalLatch.countDown();
                }
            });
        }
        latch.await();
        long cost = System.currentTimeMillis() - start;
        System.out.println("无防护并发查询耗时: " + cost + "ms（大量请求直击数据库）");




        // ---------- 有防护的情况（分布式锁） ----------
        latch = new CountDownLatch(threadCount);
        start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            CountDownLatch finalLatch1 = latch;
            executor.submit(() -> {
                try {
                    getValueWithBreakdownProtect(jedis, hotKey);
                } finally {
                    finalLatch1.countDown();
                }
            });
        }
        latch.await();
        long costY = System.currentTimeMillis() - start;
        System.out.println("有防护并发查询耗时: " + costY + "ms（只有 1 个请求查数据库）");

        // 清理资源
        //executor.shutdown();
        //jedis.del(hotKey);
    }

    /**
     * 无缓存击穿防护的查询方法
     */
    private static String getValueWithoutBreakdownProtect(Jedis jedis, String key) {
        // 1. 查缓存
        String value = jedis.get(key);
        if (value != null) {
            return value;
        }
        // 2. 缓存不存在，查数据库（大量并发会同时查数据库）
        value = MockDB.queryData(key);
        if (value != null) {
            jedis.setex(key, 3600, value);
        }
        return value;
    }

    /**
     * 有缓存击穿防护的查询方法（分布式锁）
     */
    private static String getValueWithBreakdownProtect(Jedis jedis, String key) {
        // 1. 查缓存
        String value = jedis.get(key);
        if (value != null) {
            return value;
        }

        RLock lock = null;
        try {
            System.out.println("【有防护】查询 key: " + key);
            // 2. 获取分布式锁（锁超时 30 秒）
            lock = redissonClient.getLock("lock:" + key);
            boolean locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (locked) {
                // 3. 拿到锁后，再次检查缓存（防止其他线程已更新）
                value = jedis.get(key);
                if (value != null) {
                    return value;
                }
                // 4. 查数据库（只有 1 个线程会执行）
                value = MockDB.queryData(key);
                if (value != null) {
                    jedis.setex(key, 3600, value);
                }
            } else {
                // 5. 没拿到锁，等待 50ms 后重试（或直接返回缓存旧值）
                Thread.sleep(50);
                return getValueWithBreakdownProtect(jedis, key);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            // 释放锁
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return value;
    }

    // ====================== 缓存雪崩 ======================
    /**
     * 测试缓存雪崩（大量 key 同时过期）
     * 解决方案：过期时间随机化
     */
    private static void testCacheAvalanche(Jedis jedis) throws InterruptedException {
        Random random = new Random();
        int keyCount = 10;

        // ---------- 无防护的情况（所有 key 同时过期） ----------
        System.out.println("【无防护】设置 " + keyCount + " 个 key 同时过期（10 秒后）");
        for (int i = 1; i <= keyCount; i++) {
            String key = "normal_key_" + i;
            // 所有 key 都设置 10 秒过期
            jedis.setex(key, 10, "normal_value_" + i);
        }
        Thread.sleep(10500); // 等待所有 key 过期

        // 模拟 50 个并发请求查询这些 key
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(keyCount);
        long start = System.currentTimeMillis();

        for (int i = 1; i <= keyCount; i++) {
            int finalI = i;
            executor.submit(() -> {
                try {
                    getValueWithoutAvalancheProtect(jedis, "normal_key_" + finalI);
                } finally {
                   // latch.countDown();
                }
            });
        }
        latch.await();
        long cost = System.currentTimeMillis() - start;
        System.out.println("无防护雪崩查询耗时: " + cost + "ms（大量请求同时查数据库）");

        // ---------- 有防护的情况（过期时间随机化） ----------
        System.out.println("\n【有防护】设置 " + keyCount + " 个 key 过期时间随机化（10-20 秒）");
        for (int i = 1; i <= keyCount; i++) {
            String key = "normal_key_" + i;
            // 过期时间随机化：10 + 随机 0-10 秒
            int expireTime = 10 + random.nextInt(10);
            jedis.setex(key, expireTime, "normal_value_" + i);
        }
        Thread.sleep(20500); // 等待所有 key 过期

        latch = new CountDownLatch(keyCount);
        start = System.currentTimeMillis();
        for (int i = 1; i <= keyCount; i++) {
            int finalI = i;
            CountDownLatch finalLatch = latch;
            executor.submit(() -> {
                try {
                    getValueWithoutAvalancheProtect(jedis, "normal_key_" + finalI);
                } finally {
                    finalLatch.countDown();
                }
            });
        }
        latch.await();
        cost = System.currentTimeMillis() - start;
        System.out.println("有防护雪崩查询耗时: " + cost + "ms（请求分散到不同时间）");

        // 清理资源
        executor.shutdown();
        // 删除所有测试 key
        for (int i = 1; i <= keyCount; i++) {
            jedis.del("normal_key_" + i);
        }
    }

    /**
     * 通用查询方法（用于雪崩测试）
     */
    private static String getValueWithoutAvalancheProtect(Jedis jedis, String key) {
        String value = jedis.get(key);
        if (value != null) {
            return value;
        }
        value = MockDB.queryData(key);
        if (value != null) {
            jedis.setex(key, 3600, value);
        }
        return value;
    }
}
