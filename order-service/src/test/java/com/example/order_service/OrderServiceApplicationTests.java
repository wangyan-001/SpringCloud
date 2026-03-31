package com.example.order_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import java.util.Properties;
@SpringBootTest
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}


	@Test
	public void NacosConfigTest() {

			// 1. 填写你Nacos的真实配置（复制你application.yml里的参数）
			String serverAddr = "127.0.0.1:8848";
			String namespaceId = "0234c20e-500a-4c0e-b231-cbbeb2c676b5"; // 你的命名空间ID
			String dataId = "order-service.yml"; // 你的Data ID
			String group = "DEFAULT_GROUP"; // 你的Group

			// 2. 创建Nacos配置客户端
			Properties properties = new Properties();
			properties.put("serverAddr", serverAddr);
			properties.put("namespace", namespaceId); // 关键：必须填ID，不是名称

			// 3. 拉取配置
        ConfigService configService = null;
        try {
            configService = NacosFactory.createConfigService(properties);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
        String configContent = null;
        try {
            configContent = configService.getConfig(dataId, group, 5000);
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }

        // 4. 打印结果
			System.out.println("===== Nacos配置拉取结果 =====");
			if (configContent == null || configContent.isEmpty()) {
				System.out.println("❌ 拉取失败：配置不存在或参数不匹配");
			} else {
				System.out.println("✅ 拉取成功：\n" + configContent);
			}
		}


}
