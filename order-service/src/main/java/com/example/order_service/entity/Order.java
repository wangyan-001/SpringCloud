package com.example.order_service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order") // 对应数据库表名
public class Order {

    // 订单ID（主键自增）
    @TableId(type = IdType.AUTO)
    private Long id;

    // 用户ID
    private Long userId;

    // 商品ID
    private Long productId;

    // 购买数量
    private Integer count;

    // 订单金额
    private BigDecimal amount;

    // 创建时间
    private LocalDateTime createTime;
}
