package com.pjsent.sentinel.common.exception;

/**
 * 비즈니스 로직 예외
 * 애플리케이션의 비즈니스 규칙 위반 시 발생하는 예외
 */
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final Object[] args;

    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
        this.args = null;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.args = null;
    }

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.args = null;
    }

    public BusinessException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }
}
