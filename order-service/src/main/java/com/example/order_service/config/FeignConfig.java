package com.example.order_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.seata.core.context.RootContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seata Feign 拦截器：把 XID 放到 HTTP 请求头中，传递给下游服务
 */
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor seataFeignInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 获取 Seata 全局事务ID（XID）
                String xid = RootContext.getXID();
                if (xid != null) {
                    // 把 XID 放到请求头（key 必须是 "TX_XID"，Seata 默认识别）
                    template.header("TX_XID", xid);
                }
            }
        };
    }
}