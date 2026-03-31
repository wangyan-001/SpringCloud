package org.example.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 订单创建请求 DTO
 * 用于接收前端/网关传递的订单参数
 */
@Data
public class OrderDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 订单金额
     */
    private BigDecimal amount;
}
