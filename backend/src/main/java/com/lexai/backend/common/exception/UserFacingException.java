package com.lexai.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 返回给前端的业务/网关类错误，携带 HTTP 状态与中文说明，不暴露堆栈。
 */
public class UserFacingException extends RuntimeException {

    private final HttpStatus status;

    public UserFacingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public UserFacingException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
