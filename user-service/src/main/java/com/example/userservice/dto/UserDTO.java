package com.example.userservice.dto;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String phone;
    // 记录当前提供服务的端口，方便看负载均衡效果
    private Integer serverPort;
}