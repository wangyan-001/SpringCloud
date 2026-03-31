package com.example.gateway_service.Listener;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;


/* 动态监控 Nacos 配置变更 */
@Component
public class NacosDynamicRouteListener {

    private static final String DATA_ID = "gateway-service.yaml";
    private static final String GROUP = "DEFAULT_GROUP";
    private static final String NACOS_SERVER_ADDR = "127.0.0.1:8848";
    private static final String NACOS_NAMESPACE = "";

    // 核心修复：注入 Spring 提供的负载均衡器工厂
    @Resource
    private RouteDefinitionWriter routeDefinitionWriter;

    @PostConstruct
    public void init() {
        try {
            Properties properties = new Properties();
            properties.put("serverAddr", NACOS_SERVER_ADDR);
            properties.put("namespace", NACOS_NAMESPACE);
            ConfigService configService = NacosFactory.createConfigService(properties);

            // 监听配置
            configService.addListener(DATA_ID, GROUP, new Listener() {
                @Override
                public Executor getExecutor() {
                    return Runnable::run; // 使用通用线程池
                }

                @Override
                public void receiveConfigInfo(String configContent) {
                    System.out.println("接收到 Nacos 配置变更，更新路由...");
                    updateRoutes(configContent);
                }
            });

            // 初始化加载
            String initConfig = configService.getConfig(DATA_ID, GROUP, 5000);
            updateRoutes(initConfig);
        } catch (NacosException e) {
            e.printStackTrace();
        }
    }

    private void updateRoutes(String configContent) {
        if (configContent == null || configContent.isEmpty()) return;

        try {
            Yaml yaml = new Yaml(new Constructor(Map.class));
            Map<String, Object> configMap = yaml.load(configContent);

            // 提取层级
            Map<String, Object> spring = (Map<String, Object>) configMap.get("spring");
            Map<String, Object> cloud = (Map<String, Object>) spring.get("cloud");
            Map<String, Object> gateway = (Map<String, Object>) cloud.get("gateway");
            List<Map<String, Object>> routesList = (List<Map<String, Object>>) gateway.get("routes");

            if (routesList == null) return;

            // 1. 清空原有路由
            routeDefinitionWriter.delete(Mono.empty()).block();

            // 2. 批量添加新路由
            for (Map<String, Object> routeMap : routesList) {
                RouteDefinition route = new RouteDefinition();
                route.setId((String) routeMap.get("id"));
                route.setUri(URI.create((String) routeMap.get("uri"))); // lb://user-service

                // 解析 Predicates (List<String>)
                List<Object> predicates = (List<Object>) routeMap.get("predicates");
                if (predicates != null) {
                    for (Object p : predicates) {
                        route.getPredicates().add(new PredicateDefinition(p.toString()));
                    }
                }

                // 解析 Filters (List<String>)
                List<Object> filters = (List<Object>) routeMap.get("filters");
                if (filters != null) {
                    for (Object f : filters) {
                        route.getFilters().add(new FilterDefinition(f.toString()));
                    }
                }

                // 核心修复：这里保存路由，Spring 会自动关联 LoadBalancer
                routeDefinitionWriter.save(Mono.just(route)).block();
                System.out.println("路由加载成功：" + route.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}