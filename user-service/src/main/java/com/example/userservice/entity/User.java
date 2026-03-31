package com.example.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户实体类，对应数据库 user 表
 */
@Data // lombok 注解，自动生成 getter/setter/toString 等
@TableName("user") // 指定数据库表名
public class User implements Serializable {

    // 序列化版本号（可选，建议加，避免序列化兼容问题）
    private static final long serialVersionUID = 1L;

    // 主键，雪花算法生成
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    // 用户名
    private String userName;
    // 手机号
    private String phone;

    // 创建时间

    private Date createTime;

    // 逻辑删除字段
    private Integer isDeleted;

    //积分
    private Long points;
}