package org.example.entity;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderCreatedEvent implements Serializable {
    private String orderId;
    private Long userId;
    private Long productId;
    private BigDecimal amount;
    // 构造器、getter/setter
}
