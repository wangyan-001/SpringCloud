package com.example.userservice.config;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * 适配 Hikari 数据源的 Seata 代理配置（无 Druid，避免冲突）
 */
@Configuration
public class SeataDataSourceConfig {

    // 读取 spring.datasource 配置，创建 Hikari 数据源
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public HikariDataSource hikariDataSource() {
        return new HikariDataSource();
    }

    // Seata 代理 Hikari 数据源（@Primary 必须加）
    @Bean
    @Primary
    public DataSource dataSourceProxy(HikariDataSource hikariDataSource) {
        return new DataSourceProxy(hikariDataSource);
    }
}
