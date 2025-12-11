package com.pjsent.sentinel.common.exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 */
public class ResourceNotFoundException extends BusinessException {
    
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }
    
    public ResourceNotFoundException(String resource, String identifier) {
        super("RESOURCE_NOT_FOUND", String.format("%s을(를) 찾을 수 없습니다. ID: %s", resource, identifier));
    }
    
    public ResourceNotFoundException(String resource, Long id) {
        this(resource, String.valueOf(id));
    }
}
