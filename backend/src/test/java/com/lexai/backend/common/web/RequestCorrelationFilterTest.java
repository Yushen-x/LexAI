package com.lexai.backend.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * {@link RequestCorrelationFilter} 单元测试：验证 traceId 的透传/生成、回写响应头，
 * 以及请求结束后 MDC 被清理（防止线程复用串号）。
 */
class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private FilterChain capturingChain(AtomicReference<String> mdcDuringRequest) {
        return (req, res) -> mdcDuringRequest.set(MDC.get(RequestCorrelationFilter.TRACE_ID_MDC_KEY));
    }

    @Test
    @DisplayName("透传调用方提供的 X-Request-Id，并回写响应头")
    void propagatesIncomingTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestCorrelationFilter.TRACE_ID_HEADER, "trace-from-gateway");
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(request, response, capturingChain(seen));

        assertThat(seen.get()).isEqualTo("trace-from-gateway");
        assertThat(response.getHeader(RequestCorrelationFilter.TRACE_ID_HEADER))
                .isEqualTo("trace-from-gateway");
    }

    @Test
    @DisplayName("缺省时生成非空 traceId 并回写响应头")
    void generatesTraceIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(request, response, capturingChain(seen));

        assertThat(seen.get()).isNotBlank();
        assertThat(response.getHeader(RequestCorrelationFilter.TRACE_ID_HEADER))
                .isNotBlank()
                .isEqualTo(seen.get());
    }

    @Test
    @DisplayName("请求结束后 MDC 被清理")
    void clearsMdcAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, capturingChain(new AtomicReference<>()));

        assertThat(MDC.get(RequestCorrelationFilter.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("超长 X-Request-Id 被截断到 64 字符")
    void truncatesOverlongTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestCorrelationFilter.TRACE_ID_HEADER, "x".repeat(200));
        AtomicReference<String> seen = new AtomicReference<>();

        filter.doFilter(request, response, capturingChain(seen));

        assertThat(seen.get()).hasSize(64);
    }
}
