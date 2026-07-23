package com.supportai.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitInterceptorTest {

    @Test
    void allowsRequestsUpToTheLimitThenReturns429() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(2, 60_000);

        assertTrue(preHandle(interceptor, "1.2.3.4"), "1st request allowed");
        assertTrue(preHandle(interceptor, "1.2.3.4"), "2nd request allowed");

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request("1.2.3.4"), blocked, new Object()), "3rd request blocked");
        assertEquals(429, blocked.getStatus());
        assertTrue(blocked.getContentAsString().contains("Too many requests"));
    }

    @Test
    void limitsAreTrackedPerClient() throws Exception {
        RateLimitInterceptor interceptor = new RateLimitInterceptor(1, 60_000);

        assertTrue(preHandle(interceptor, "10.0.0.1"), "client A first request allowed");
        assertFalse(preHandle(interceptor, "10.0.0.1"), "client A second request blocked");
        assertTrue(preHandle(interceptor, "10.0.0.2"), "different client is unaffected");
    }

    private boolean preHandle(RateLimitInterceptor interceptor, String ip) throws Exception {
        return interceptor.preHandle(request(ip), new MockHttpServletResponse(), new Object());
    }

    private MockHttpServletRequest request(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        return request;
    }
}
