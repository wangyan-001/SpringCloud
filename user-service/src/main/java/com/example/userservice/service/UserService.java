package com.example.userservice.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.userservice.entity.User;
import com.example.userservice.mapper.UserMapper;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 用户服务接口：继承IService<User>，获得基础的业务层方法
 */
public interface UserService extends IService<User> {

    /*MQ Str*/
    // 给用户增加积分 —— 你现在要加的方法
    void addPoints(Long userId, Long points);


    /*MQ End*/


    /**
     * 扣减用户余额
     */
    public void deductBalance(Long userId, BigDecimal amount);


    public List<User> selectByCustomSql();

    /***Redisson***/
    public User getUserByIdWithRedisson(Long id);


    // ========== 基础业务方法（IService已提供，无需手写） ==========
    // save(User user) → 新增
    // removeById(Long id) → 根据ID删除
    // updateById(User user) → 根据ID更新
    // getById(Long id) → 根据ID查询
    // list(QueryWrapper<User> queryWrapper) → 条件查询列表
    // page(IPage<User> page, QueryWrapper<User> queryWrapper) → 分页查询

    // ========== 自定义业务方法（根据业务需求扩展） ==========
    /**
     * 根据手机号查询用户
     */
    User getByPhone(String phone);

    /**
     * 批量更新用户名
     */
    boolean updateUserNameBatch(List<Long> ids, String userName);

    /**
     * 分页查询用户（带条件）
     */
    IPage<User> getUserPage(IPage<User> page, QueryWrapper<User> queryWrapper);




    /**
     * 查询用户信息（核心缓存注解）
     * @Cacheable：优先查Redis缓存，缓存命中则直接返回，未命中则执行方法并将结果存入缓存
     * value：缓存名称（Redis中key的前缀，比如value="user"，最终key是"user::1"）
     * key：缓存的具体key，#userId表示取方法参数userId作为key
     * unless：条件过滤，比如用户为null时不缓存（可选）
     */
   // @Cacheable(value = "user", key = "#userId", unless = "#result == null")
  /*  public default User getUserById(Long userId) {
        // 只有缓存未命中时，才会执行这里的数据库查询逻辑
        //return getUserFromDb(userId);

    }*/
    public User getRedisUserById(Long userId);

    //加互斥锁
    public User getUserByIdWithLock(Long userId);




    // 模拟数据库查询（实际项目中替换为MyBatis/MyBatis-Plus的Mapper调用）
    public default User getUserFromDb(Long userId) {
        System.out.println("【数据库查询】用户ID：" + userId);
        User user = new User();
        user.setId(userId);
        user.setUserName("zhangsan_" + userId);
        user.setPhone("1380013800" + userId);
        //user.setEmail("zhangsan" + userId + "@example.com");
        user.setIsDeleted(1);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(new Date());
        return user;
    }

    /**
     * 更新用户信息（更新后清除缓存，避免脏数据）
     * @CacheEvict：清除指定缓存
     * key：要清除的缓存key，和查询时的key保持一致（#user.id）

    @CacheEvict(value = "user", key = "#user.id")
    public default void updateUser(User user) {
        System.out.println("【数据库更新】用户ID：" + user.getId());
        // 实际项目中：调用Mapper更新数据库
    }


    public default void updateUser(User user) {
        System.out.println("【数据库更新】用户ID：" + user.getId());
        // 实际项目中：调用Mapper更新数据库
    }*/

    public User updateUser(User user);

    /**
     * 删除用户（清除缓存）
     * allEntries = false：仅清除当前用户的缓存；true则清除整个"user"缓存空间的所有数据
     */
    @CacheEvict(value = "user", key = "#userId", allEntries = false)
    public default void deleteUser(Long userId) {
        System.out.println("【数据库删除】用户ID：" + userId);
        // 实际项目中：调用Mapper删除数据库记录
    }

    /**
     * 批量删除用户（清除整个user缓存空间）
     * allEntries = true：清除value="user"下的所有缓存
     */
    @CacheEvict(value = "user", allEntries = true)
    public default void batchDeleteUser() {
        System.out.println("【批量删除】清除所有用户缓存");
        // 实际项目中：批量删除数据库记录
    }



}