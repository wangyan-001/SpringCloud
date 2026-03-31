package com.example.userservice.config;

import io.seata.core.context.RootContext;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Seata 过滤器：从请求头取出 XID，绑定到当前线程
 */
@Component
public class SeataXidFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        // 从请求头取出 XID
        String xid = req.getHeader("TX_XID");
        // 绑定 XID 到 Seata 上下文
        RootContext.bind(xid);
        try {
            chain.doFilter(request, response);
        } finally {
            // 移除 XID 绑定
            RootContext.unbind();
        }
    }
}