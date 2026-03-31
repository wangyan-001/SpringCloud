package com.example.gateway_service.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SentinelGatewayConfig {

    public SentinelGatewayConfig() {
        GatewayCallbackManager.setBlockHandler(new BlockRequestHandler() {
            @Override
            public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable t) {
                Map<String, Object> result = new HashMap<>();
                result.put("code", 429);

                if (t instanceof BlockException) {
                    if (t instanceof DegradeException) {
                        result.put("message", "【熔断生效】用户服务异常，已降级！");
                    } else if (t instanceof FlowException) {
                        result.put("message", "【限流生效】请求频率过高！");
                    } else if (t instanceof ParamFlowException) {
                        result.put("message", "【热点限流】请求频率过高！");
                    } else {
                        result.put("message", "系统繁忙，请稍后再试");
                    }
                } else {
                    result.put("code", 500);
                    result.put("message", "系统内部错误");
                }

                return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(result);
            }
        });
    }
}