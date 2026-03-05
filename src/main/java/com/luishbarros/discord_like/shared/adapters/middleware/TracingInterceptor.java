package com.luishbarros.discord_like.shared.adapters.middleware;

import brave.Tracing;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TracingInterceptor implements HandlerInterceptor {

    private final Tracing tracing;

    public TracingInterceptor(Tracing tracing) {
        this.tracing = tracing;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                        Object handler) {
        var traceContext = tracing.currentTraceContext();
        var context = traceContext.get();

        if (context != null) {
            String traceId = context.traceIdString();
            String spanId = context.spanIdString();

            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);

            // Adicionar headers de tracing à resposta
            response.setHeader("X-Trace-Id", traceId);
            response.setHeader("X-Span-Id", spanId);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                           Object handler, Exception ex) {
        MDC.clear();
    }
}
