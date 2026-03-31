package com.example.gateway_service.config;

import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.Entry;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SentinelExceptionReportFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .onErrorResume(e -> {
                    Entry entry = ContextUtil.getContext().getCurEntry();
                    if (entry != null) {
                        Tracer.traceEntry(e, entry);
                    }
                    return Mono.error(e);
                })
                .then(Mono.fromRunnable(() -> {
                    HttpStatus status = exchange.getResponse().getStatusCode();
                    if (status != null && status.is5xxServerError()) {
                        Entry entry = ContextUtil.getContext().getCurEntry();
                        if (entry != null) {
                            Tracer.traceEntry(new RuntimeException("下游500异常"), entry);
                        }
                    }
                }));
    }

    @Override
    public int getOrder() {
        return -100; // 确保顺序正确
    }
}