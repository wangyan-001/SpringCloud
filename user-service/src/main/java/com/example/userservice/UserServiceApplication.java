package com.example.userservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableCaching // 开启缓存注解
//@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.example.userservice.mapper")
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class UserServiceApplication {
	public static void main(String[] args) {

		SpringApplication.run(UserServiceApplication.class, args);

		System.out.println("User Service Started yeah!!!!");
	}
}