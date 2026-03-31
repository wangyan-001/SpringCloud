package com.example.userservice.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 自定义配置类：让 RedisTemplate 存储 JSON 格式数据
 */
@Configuration
public class RedisConfig {

    /**
     * 自定义 RedisTemplate Bean，指定序列化规则
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        // 1. 创建 RedisTemplate 对象
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 2. 设置 Redis 连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 3. 创建 JSON 序列化器（核心：把对象转成 JSON 字符串）
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        // 3.1 配置 ObjectMapper：让 Jackson 能序列化/反序列化所有字段
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 3.2 支持多态类型（比如 List<User>、Map<String, User> 等复杂类型）
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // 4. 创建 String 序列化器（Key 用 String 存储，避免乱码）
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // 5. 核心配置：设置序列化规则
        redisTemplate.setKeySerializer(stringRedisSerializer);          // Key 序列化
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);  // Value 序列化
        redisTemplate.setHashKeySerializer(stringRedisSerializer);      // Hash Key 序列化
        redisTemplate.setHashValueSerializer(jackson2JsonRedisSerializer); // Hash Value 序列化

        // 6. 初始化 RedisTemplate
        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }
}