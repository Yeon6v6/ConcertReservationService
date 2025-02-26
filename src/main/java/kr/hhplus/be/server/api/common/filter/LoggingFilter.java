package kr.hhplus.be.server.api.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@WebFilter(urlPatterns = "/*")
public class LoggingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("[LoggingFilter] Initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long startTime = System.currentTimeMillis();
//        log.info("[LoggingFilter] 요청 IP : {}", request.getRemoteAddr());
//        log.info("[LoggingFilter] 요청 시간 : {}", System.currentTimeMillis());
//
        chain.doFilter(request, response);

        long duration = System.currentTimeMillis() - startTime;
//        log.info("[LoggingFilter] 요청 처리 시간 : {} ms", duration);
    }

    @Override
    public void destroy() {
//        log.info("[LoggingFilter] 종료 시간 : {}", System.currentTimeMillis());
    }
}
