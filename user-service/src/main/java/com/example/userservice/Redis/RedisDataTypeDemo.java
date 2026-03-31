package com.example.userservice.Redis;

import redis.clients.jedis.Jedis;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis 各数据类型 Java 实现示例
 * 可直接运行 main 方法测试所有操作
 */
public class RedisDataTypeDemo {

    // Redis 连接配置（根据实际情况修改）
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
   // private static final String REDIS_PASSWORD = ""; // 如果有密码请填写
    private static final int REDIS_DB = 0; // 使用第0个数据库

    public static void main(String[] args) {
        // 创建 Jedis 连接（try-with-resources 自动关闭连接）
        try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
            // 如果 Redis 设置了密码，需要认证

            // 选择数据库
            jedis.select(REDIS_DB);
            /*
            System.out.println("===== 1. String 字符串类型 =====");
            testString(jedis);

            System.out.println("\n===== 2. Hash 哈希类型 =====");
            testHash(jedis);

            System.out.println("\n===== 3. List 列表类型 =====");
            testList(jedis);

            System.out.println("\n===== 4. Set 集合类型 =====");
            testSet(jedis);

            System.out.println("\n===== 5. Sorted Set 有序集合类型 =====");
            testSortedSet(jedis);

            System.out.println("\n===== 6. Bitmap 位图类型 =====");
            testBitmap(jedis);*/

            System.out.println("\n===== 7. HyperLogLog 基数统计 =====");
            testHyperLogLog(jedis);


        } catch (Exception e) {
            System.err.println("Redis 操作异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试 String 类型（最基础的键值对）
     */
    private static void testString(Jedis jedis) {
        // 1. 设置键值对
        jedis.set("username", "zhangsan");
        // 2. 设置带过期时间的键值对（10秒过期）
        jedis.setex("code", 1000, "123456");
        // 3. 获取值
        String username = jedis.get("username");
        String code = jedis.get("code");
        // 4. 追加字符串
        jedis.append("username", "_vip");
        String newUsername = jedis.get("username");
        // 5. 获取字符串长度
        Long len = jedis.strlen("username");

        System.out.println("username: " + username);
        System.out.println("code: " + code);
        System.out.println("追加后的username: " + newUsername);
        System.out.println("username长度: " + len);

        // 删除测试键
        //jedis.del("username", "code");
    }

    /**
     * 测试 Hash 类型（键值对的集合，适合存储对象）
     */
    private static void testHash(Jedis jedis) {
        // 1. 设置单个字段
        jedis.hset("user:100", "name", "lisi");
        jedis.hset("user:100", "age", "25");
        // 2. 批量设置字段
        jedis.hmset("user:100", Map.of("gender", "male", "email", "lisi@test.com", "phone", "18888888888"));
        // 3. 获取单个字段值
        String name = jedis.hget("user:100", "name");
        // 4. 获取多个字段值
        List<String> fields = jedis.hmget("user:100", "name", "age", "email");
        // 5. 获取所有字段和值
        Map<String, String> allFields = jedis.hgetAll("user:100");
        // 6. 获取所有字段名
        Set<String> keys = jedis.hkeys("user:100");
        // 7. 获取所有值
        List<String> values = jedis.hvals("user:100");
        // 8. 获取字段数量
        Long fieldCount = jedis.hlen("user:100");
        // 9. 判断字段是否存在
        Boolean exists = jedis.hexists("user:100", "gender");

        System.out.println("单个字段name: " + name);
        System.out.println("多个字段值: " + fields);
        System.out.println("所有字段和值: " + allFields);
        System.out.println("所有字段名: " + keys);
        System.out.println("所有值: " + values);
        System.out.println("字段数量: " + fieldCount);
        System.out.println("gender字段是否存在: " + exists);

        // 删除测试键
       // jedis.del("user:100");
    }

    /**
     * 测试 List 类型（有序可重复的列表，双向链表）
     */
    private static void testList(Jedis jedis) {
        // 1. 从左侧插入元素
        jedis.lpush("mylist", "A", "B", "C");
        // 2. 从右侧插入元素
        jedis.rpush("mylist", "D", "E");
        // 3. 获取列表长度
        Long length = jedis.llen("mylist");
        // 4. 获取指定范围的元素（0表示第一个，-1表示最后一个）
        List<String> listAll = jedis.lrange("mylist", 0, -1);
        // 5. 从左侧弹出元素
        String leftPop = jedis.lpop("mylist");
        // 6. 从右侧弹出元素
        String rightPop = jedis.rpop("mylist");
        // 7. 获取弹出后的列表
        List<String> listAfterPop = jedis.lrange("mylist", 0, -1);

        System.out.println("列表初始长度: " + length);
        System.out.println("初始列表元素: " + listAll);
        System.out.println("左侧弹出元素: " + leftPop);
        System.out.println("右侧弹出元素: " + rightPop);
        System.out.println("弹出后的列表: " + listAfterPop);

        // 删除测试键
        //jedis.del("mylist");
    }

    /**
     * 测试 Set 类型（无序不可重复的集合）
     */
    private static void testSet(Jedis jedis) {
        // 1. 添加元素
        jedis.sadd("myset", "apple", "banana", "orange", "apple","pair"); // 重复的apple会自动去重
        // 2. 获取集合所有元素
        Set<String> allMembers = jedis.smembers("myset");
        // 3. 获取集合大小
        Long size = jedis.scard("myset");
        // 4. 判断元素是否存在
        Boolean isMember = jedis.sismember("myset", "banana");
        // 5. 删除指定元素
        jedis.srem("myset", "orange");
        // 6. 随机获取一个元素
        //String randomMember = jedis.srandmember("myset");
        // 7. 随机弹出一个元素
      //  String popMember = jedis.spop("myset");

        System.out.println("集合所有元素: " + allMembers);
        System.out.println("集合大小: " + size);
        System.out.println("banana是否存在: " + isMember);
        System.out.println("删除orange后的集合: " + jedis.smembers("myset"));
      //  System.out.println("随机获取元素: " + randomMember);
       // System.out.println("随机弹出元素: " + popMember);

        // 删除测试键
        //jedis.del("myset");
    }

    /**
     * 测试 Sorted Set 类型（有序不可重复的集合，带分数排序）
     */
    private static void testSortedSet(Jedis jedis) {
        // 1. 添加元素（元素+分数）
        jedis.zadd("myzset", 95, "Tom");
        jedis.zadd("myzset", 88, "Jerry");
        jedis.zadd("myzset", 92, "Mike");
        // 2. 获取指定范围的元素（按分数升序，0第一个，-1最后一个）
      // Set<String> ascMembers = jedis.zrange("myzset", 0, -1);
        // 修复后
        List<String> ascMembers = jedis.zrange("myzset", 0, -1);
        List<String> descMembers = jedis.zrevrange("myzset", 0, -1);
        // 3. 按分数降序获取指定范围元素
       // Set<String> descMembers = jedis.zrevrange("myzset", 0, -1);
        // 4. 获取元素的分数
        Double score = jedis.zscore("myzset", "Tom");
        // 5. 获取集合大小
        Long size = jedis.zcard("myzset");
        // 6. 获取分数范围内的元素数量（80-90分）
        Long count = jedis.zcount("myzset", 80, 90);
        // 7. 按分数排名（从0开始）
        Long rank = jedis.zrank("myzset", "Jerry");

        System.out.println("升序元素: " + ascMembers);
        System.out.println("降序元素: " + descMembers);
        System.out.println("Tom的分数: " + score);
        System.out.println("集合大小: " + size);
        System.out.println("80-90分的元素数量: " + count);
        System.out.println("Jerry的排名: " + rank);

        // 删除测试键
        //jedis.del("myzset");
    }

    /**
     * 测试 Bitmap 类型（位图，按位存储，适合二值状态统计）
     */
    private static void testBitmap(Jedis jedis) {
        // 1. 设置指定位的值（0/1）
        jedis.setbit("user:login:20260316", 100, true); // 用户ID 100 登录
        jedis.setbit("user:login:20260316", 200, true); // 用户ID 200 登录
        jedis.setbit("user:login:20260316", 100, false); // 取消用户100的登录状态
        // 2. 获取指定位的值
        Boolean isLogin100 = jedis.getbit("user:login:20260316", 100);
        Boolean isLogin200 = jedis.getbit("user:login:20260316", 200);
        // 3. 统计为1的位数（登录用户数）
        Long loginCount = jedis.bitcount("user:login:20260316");

        System.out.println("用户100是否登录: " + isLogin100);
        System.out.println("用户200是否登录: " + isLogin200);
        System.out.println("登录用户总数: " + loginCount);

        // 删除测试键
        //jedis.del("user:login:20260316");
    }

    /**
     * 测试 HyperLogLog 类型（基数统计，适合海量数据去重计数）
     */
    private static void testHyperLogLog(Jedis jedis) {
        // 1. 添加元素
        jedis.pfadd("uv:20260316", "user1", "user2", "user3", "user2", "user4");
        // 2. 统计基数（去重后的数量）
        Long uvCount = jedis.pfcount("uv:20260316");
        // 3. 合并多个HyperLogLog（比如合并两天的UV）
        jedis.pfadd("uv:20260315", "user3", "user5", "user6");
        jedis.pfmerge("uv:20260315_16", "uv:20260315", "uv:20260316");
        Long mergeCount = jedis.pfcount("uv:20260315_16");

        System.out.println("20260316的UV数: " + uvCount);
        System.out.println("合并后的UV数: " + mergeCount);

        // 删除测试键
        //jedis.del("uv:20260316", "uv:20260315", "uv:20260315_16");
    }

}
