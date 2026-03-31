package org.example;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootTest
public class DatabaseConnectionTest {

    // 注入数据源
    @Autowired
    private DataSource dataSource;

    @Test
    public void testConnection() {
        try (Connection conn = dataSource.getConnection()) {
            System.out.println("✅ Spring Boot 连接数据库成功！");
            System.out.println("数据源类型：" + dataSource.getClass().getName());
        } catch (Exception e) {
            System.err.println("❌ 连接失败：" + e.getMessage());
        }
    }
}