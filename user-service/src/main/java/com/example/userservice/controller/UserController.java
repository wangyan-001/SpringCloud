package com.example.userservice.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.userservice.dto.UserDTO;
import com.example.userservice.entity.User;
import com.example.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

@RestController
public class UserController {

    // 获取当前服务端口
    @Value("${server.port}")
    private Integer serverPort;

    @Resource
    private UserService userService;
    /**
     * 根据ID查询用户
     */
    @GetMapping("/users/{id}")
    public UserDTO getUserById(@PathVariable Long id) {
        // 模拟数据
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setUsername("用户" + id);
        user.setPhone("1380000000" + id);
        user.setServerPort(serverPort); // 标记当前服务端口
        System.out.println("用户服务" + serverPort + "处理了请求，用户ID：" + id);
        return user;
    }

    /*Put 请求 */
    @PutMapping("/users/{id}")
    public UserDTO updateUser(@PathVariable Long id, @RequestBody UserDTO user) {
        // 模拟数据
        user.setId(id);
        user.setServerPort(serverPort); // 标记当前服务端口
        System.out.println("用户服务" + serverPort + "处理了请求，用户ID：" + id);
        return user;
    }

    /*自定义SQL*/
    @PostMapping("/selectByCustomSql")
    public List<User> selectByCustomSql(){
        return userService.selectByCustomSql();
    }

    // 新增用户
    @PostMapping("/add")
    public boolean addUser(@RequestBody User user) {
        return userService.save(user);
    }

    // 根据手机号查询用户
    @GetMapping("phone/{phone}")
    public User getUserByPhone(@PathVariable String phone) {

        // 故意抛异常，用来触发降级！
        throw new RuntimeException("调用超时");

        //return userService.getByPhone(phone);
    }

    @GetMapping("/user/phone/{phone}")
    @SentinelResource(value = "user-phone", blockHandler = "handleBlock")
    public String getPhone(@PathVariable String phone) {
        return "手机号：" + phone;
    }

    // 限流/熔断兜底方法
    public String handleBlock(String phone, BlockException e) {
        return "热点限流：该手机号查询太频繁，请稍后再试";
    }

    // 分页查询用户
    @GetMapping("/page/{current}/{size}")
    public IPage<User> getUserPage(@PathVariable long current, @PathVariable long size) {
        // 构建分页对象
        IPage<User> page = new Page<>(current, size);
        // 构建查询条件（示例：查询未删除的用户）
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>().eq("is_deleted", 0);
        return userService.getUserPage(page, queryWrapper);
    }

    /* Redis */

    @GetMapping("Redisson/{userId}")
    public User getUserByIdWithRedisson(@PathVariable Long userId) {
        return userService.getUserByIdWithRedisson(userId);
    }

    // 1. 查询用户（测试缓存命中/未命中）
    @GetMapping("/{userId}")
    public User getUser(@PathVariable Long userId) {
        System.out.println("用户服务" + serverPort + "处理了请求，用户ID：" + userId);
        return userService.getRedisUserById(userId);
    }

    @GetMapping("lockUserId/{userId}")
    public User getUserByIdWithLock(@PathVariable Long userId) {
        System.out.println("用户服务" + serverPort + "处理了请求，用户ID：" + userId);
        return userService.getUserByIdWithLock(userId);
    }



    // 2. 更新用户（测试缓存清除）
    @PostMapping("/updateUser")
    public String updateUser(@RequestBody User user) {
        userService.updateUser(user);
        return "用户更新成功，已清除缓存";
    }

    // 3. 删除用户（测试缓存清除）
    @DeleteMapping("/{userId}")
    public String deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return "用户删除成功，已清除缓存";
    }

    // 用于Order服务调用
    @PostMapping("user/deductBalance")
    public void deductBalance(Long userId, BigDecimal amount) {
        userService.deductBalance(userId, amount);
    }
}