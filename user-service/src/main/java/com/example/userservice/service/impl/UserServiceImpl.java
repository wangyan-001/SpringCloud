package com.example.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.example.userservice.entity.User;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类：继承ServiceImpl<Mapper, Entity>，实现UserService接口
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 注入 StringRedisTemplate（Spring 自带，无需额外配置）
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Jackson 的 JSON 转换工具（Spring 自动配置，直接注入）
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    // 注入 Redisson 客户端（Spring Boot 自动配置）
    @Resource
    private RedissonClient redissonClient;

    @Autowired
    public UserMapper userMapper;


    /**
     * 增加用户积分
     * @param userId 用户ID
     * @param points 要增加的积分数
     */
    @Override
    public void addPoints(Long userId, Long points) {


        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 2. 积分累加（原来的积分 + 新增积分）
        user.setPoints(points);

        // 3. 更新数据库
        userMapper.updateById(user);

    }


    public void deductBalance(Long userId, BigDecimal amount) {
        // 扣余额
        userMapper.updateBalance(userId, amount);
    }


    public List<User> selectByCustomSql() {
        // 自定义条件SQL
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", 0).like("user_name", "张").like("user_name", "李");
        wrapper.or().like("user_name", "王");
        List<User> userList = userMapper.selectByCustomSql(wrapper);

        LambdaQueryWrapper<User> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.like(User::getUserName, "张");

        // Mapper层
        int rows = userMapper.delete(wrapper);
        // Service层
        boolean success = this.remove(wrapper);

        //DB 为静态调用方法
        Db.lambdaUpdate(User.class).eq(User::getId, 1).set(User::getUserName, "张三").update();
        return userList;
    }

    /** Redisson ***/
    /**
     * 查询用户（防缓存击穿：Redisson 分布式锁方案）
     */
    public User getUserByIdWithRedisson(Long id) {
        String cacheKey = USER_CACHE_KEY + id;
        String lockKey = LOCK_KEY_PREFIX + id;

        // 1. 先查缓存
        User cacheUser = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cacheUser != null) {
            return cacheUser;
        }

        // 2. 获取 Redisson 分布式锁
        RLock lock = redissonClient.getLock(lockKey);
        User dbUser = null;

        try {
            // 3. 尝试获取锁：最多等3秒，拿到锁后10秒自动释放（Redisson会自动续期）
            boolean lockSuccess = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (lockSuccess) {
                // 4. 双重检查缓存（防止等待期间缓存已被更新）
                cacheUser = (User) redisTemplate.opsForValue().get(cacheKey);
                if (cacheUser != null) {
                    return cacheUser;
                }

                // 5. 查数据库
                dbUser = userMapper.selectById(id);
                if (dbUser != null) {
                    // 6. 更新缓存
                    redisTemplate.opsForValue().set(cacheKey, dbUser, CACHE_EXPIRE, TimeUnit.SECONDS);
                } else {
                    // 防缓存穿透：缓存空值
                    redisTemplate.opsForValue().set(cacheKey, new User(), 300L, TimeUnit.SECONDS);
                }
            } else {
                // 7. 没拿到锁，等待50ms后重试（最多3次）
                int retryCount = 0;
                while (retryCount < 3) {
                    Thread.sleep(50);
                    cacheUser = (User) redisTemplate.opsForValue().get(cacheKey);
                    if (cacheUser != null) {
                        return cacheUser;
                    }
                    retryCount++;
                }
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 异常兜底：直接查数据库
            return userMapper.selectById(id);
        } finally {
            // 8. 释放锁（必须在finally中释放，防止死锁）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                System.out.println("释放锁成功：" + lockKey);
            }
        }
        return dbUser;
    }

    // 存 JSON 到 Redis
    public void saveUserToRedis(User user) {
        try {
            // 1. 将 User 对象转成 JSON 字符串
            String jsonStr = objectMapper.writeValueAsString(user);
            // 2. 存入 Redis（key 自定义，value 是纯 JSON 字符串）
            stringRedisTemplate.opsForValue().set("user:" + user.getId(), jsonStr);
            System.out.println("JSON 已存入 Redis：" + jsonStr);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    // 从 Redis 读取 JSON 并转成对象
    public User getUserFromRedis(Long userId) {
        // 1. 读取 Redis 中的 JSON 字符串
        String jsonStr = stringRedisTemplate.opsForValue().get("user:" + userId);
        if (jsonStr == null) {
            return null;
        }
        // 2. 将 JSON 字符串转回 User 对象
        try {
            return objectMapper.readValue(jsonStr, User.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final long CACHE_TIMEOUT = 1;
    private static final TimeUnit CACHE_TIME_UNIT = TimeUnit.HOURS;
    // 更新用户时，需要更新缓存（删除或更新）
    public User updateUser(User user) {
        try {
        // 1. 执行数据库更新（返回影响行数）
        int affectedRows = userMapper.updateById(user);

        if (affectedRows > 0) {
            // 2. 从数据库查询最新的用户数据（确保拿到更新后的值）
            User updatedUser = userMapper.selectById(user.getId());

            if (updatedUser != null) {
                // 3. 更新 Redis 缓存（用最新数据覆盖旧缓存）
                String key = USER_CACHE_KEY_PREFIX + updatedUser.getId();
                // 1. 将 User 对象转成 JSON 字符串
                String jsonStr = objectMapper.writeValueAsString(updatedUser);
                stringRedisTemplate.opsForValue().set(key, jsonStr, CACHE_TIMEOUT, CACHE_TIME_UNIT);
                return updatedUser;
            }
        }
        }catch (JsonProcessingException e)
        {
            e.printStackTrace();
        }

        // 更新失败或查询不到数据时返回 null
        return null;
    }

    private static final String USER_CACHE_KEY_PREFIX = "user:";
    // 注入Mapper（也可以直接用baseMapper，ServiceImpl已封装）


    /*Redis Start*/

    // 正常数据缓存时间（1小时）
    private static final long NORMAL_EXPIRE = 3600L;
    // 空值缓存时间（5分钟，短一点避免占用空间）
    private static final long NULL_EXPIRE = 300L;


    public User getRedisUserById(Long id) {
        String key = USER_CACHE_KEY_PREFIX + id;

        // 1. 从Redis中获取
        User user = (User) redisTemplate.opsForValue().get(key);
        if (user != null) {
            return user; // 缓存命中，直接返回
        }

        // 2. 缓存未命中，查询数据库
        user = userMapper.selectById(id);
        if (user != null) {
            // 3. 存入Redis，并设置过期时间（例如1小时）
           // redisTemplate.opsForValue().set(key, user, 1, TimeUnit.HOURS);
            saveUserToRedis(user);
        }

        // 4. 处理数据库查询结果（核心：防穿透）
        if (user == null) {
            // 数据库也查不到：缓存空值（用一个空User对象标记）
            System.out.println("缓存空值：" + key);
            User nullUser = new User();
            nullUser.setId(null); // 用id=null标记空值
            redisTemplate.opsForValue().set(key, nullUser, NULL_EXPIRE, TimeUnit.SECONDS);
            return null;
        } else {
            // 数据库查到：缓存正常数据
            //redisTemplate.opsForValue().set(key, user, NORMAL_EXPIRE, TimeUnit.SECONDS);
            getUserFromRedis(user.getId());
            return user;
        }
      //  return getUserFromRedis(user.getId());//user;
    }

    // 缓存前缀
    private static final String USER_CACHE_KEY = "user:info:";
    // 分布式锁前缀
    private static final String LOCK_KEY_PREFIX = "lock:user:";
    // 正常缓存过期时间（1小时）
    private static final long CACHE_EXPIRE = 3600L;
    // 锁过期时间（5秒，防止死锁）
    private static final long LOCK_EXPIRE = 5L;

    /**
     * 查询用户（防缓存击穿：互斥锁方案）
     */
    public User getUserByIdWithLock(Long id) {
        String cacheKey = USER_CACHE_KEY + id;
        String lockKey = LOCK_KEY_PREFIX + id;

        // 1. 先查缓存
        User cacheUser = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cacheUser != null) {
            return cacheUser;
        }

        User dbUser = null;
        try {
            // 2. 获取分布式锁（只有一个请求能拿到）
            boolean lockSuccess = tryLock(lockKey, LOCK_EXPIRE);
            if (lockSuccess) {
                // 3. 拿到锁后，再次检查缓存（防止锁等待期间缓存已被更新）
                cacheUser = (User) redisTemplate.opsForValue().get(cacheKey);
                if (cacheUser != null) {
                    return cacheUser;
                }

                // 4. 缓存仍为空，查数据库
                dbUser = userMapper.selectById(id);
                if (dbUser != null) {
                    // 5. 更新缓存
                    redisTemplate.opsForValue().set(cacheKey, dbUser, CACHE_EXPIRE, TimeUnit.SECONDS);
                } else {
                    // 防穿透：缓存空值
                    redisTemplate.opsForValue().set(cacheKey, new User(), 300L, TimeUnit.SECONDS);
                }
            } else {
                // 6. 没拿到锁，等待50ms后重试（最多重试3次）
                int retryCount = 0;
                while (retryCount < 3) {
                    Thread.sleep(50);
                    cacheUser = (User) redisTemplate.opsForValue().get(cacheKey);
                    if (cacheUser != null) {
                        return cacheUser;
                    }
                    retryCount++;
                }
                // 重试失败，返回空或兜底数据
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 异常时直接查数据库（兜底）
            return userMapper.selectById(id);
        } finally {
            // 7. 释放锁
            releaseLock(lockKey);
        }
        return dbUser;
    }


    /**
     * 获取Redis分布式锁（原子操作，防止并发）
     */
    private boolean tryLock(String lockKey, long expireSeconds) {
        // 使用UUID作为value，防止误删别人的锁
        String lockValue = java.util.UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    /**
     * 释放Redis分布式锁（Lua脚本保证原子性）
     */
    private void releaseLock(String lockKey) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        // 获取锁时的value（这里简化，实际应存到ThreadLocal）
        String lockValue = redisTemplate.opsForValue().get(lockKey).toString();
        redisTemplate.execute(redisScript, Collections.singletonList(lockKey), lockValue);
    }



    // ========== 实现自定义业务方法 ==========
    @Override
    public User getByPhone(String phone) {
        // 调用Mapper的自定义方法
        return userMapper.selectByPhone(phone);
    }

    @Override
    public boolean updateUserNameBatch(List<Long> ids, String userName) {
        // 调用Mapper的批量更新方法，返回受影响行数
        int affectedRows = userMapper.batchUpdateUserName(ids, userName);
        // 受影响行数>0则返回true，否则false
        return affectedRows > 0;
    }

    @Override
    public IPage<User> getUserPage(IPage<User> page, QueryWrapper<User> queryWrapper) {
        // 调用MyBatis-Plus的分页查询方法
        return userMapper.selectPage(page, queryWrapper);
    }
}