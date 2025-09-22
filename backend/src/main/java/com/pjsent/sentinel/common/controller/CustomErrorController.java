package com.pjsent.sentinel.common.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 커스텀 에러 페이지 컨트롤러
 */
@Slf4j
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object error = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        
        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }
        
        // 로깅
        log.error("Error occurred - Status: {}, Path: {}, Message: {}", 
            statusCode, path, message);
        
        // 모델에 에러 정보 추가
        model.addAttribute("status", statusCode);
        model.addAttribute("error", getErrorType(statusCode));
        model.addAttribute("message", getErrorMessage(statusCode, message));
        model.addAttribute("path", path);
        model.addAttribute("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return "error";
    }
    
    private String getErrorType(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "Error";
        };
    }
    
    private String getErrorMessage(int statusCode, Object message) {
        String defaultMessage = switch (statusCode) {
            case 400 -> "잘못된 요청입니다.";
            case 401 -> "인증이 필요합니다.";
            case 403 -> "접근 권한이 없습니다.";
            case 404 -> "요청하신 페이지를 찾을 수 없습니다.";
            case 500 -> "서버 내부 오류가 발생했습니다.";
            default -> "예상치 못한 오류가 발생했습니다.";
        };
        
        return message != null ? message.toString() : defaultMessage;
    }
}
