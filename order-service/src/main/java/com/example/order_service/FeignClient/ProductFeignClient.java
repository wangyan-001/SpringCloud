package com.example.order_service.FeignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 调用商品服务的Feign客户端
 * name：对应product-service的服务名（必须和product-service的spring.application.name一致）
 */
@FeignClient(name = "product-service") // 核心注解：指定要调用的微服务名
public interface ProductFeignClient {

    /**
     * 远程调用商品服务的扣库存接口
     * 注意：
     * 1. 请求方式、路径要和product-service的Controller完全一致
     * 2. @RequestParam 必须加，否则参数传递会失败
     */
    @PostMapping("/product/deductStock")
    void deductStock(@RequestParam("productId") Long productId, @RequestParam("count") Integer count);
}
