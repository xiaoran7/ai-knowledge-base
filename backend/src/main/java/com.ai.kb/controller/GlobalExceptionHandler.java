package com.ai.kb.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理非法参数异常（如用户名已存在、邮箱已存在）
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)  // 400
            .body(e.getMessage());  // 只返回错误消息
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)  // 400
            .body(e.getMessage());  // 只返回错误消息
    }

    /**
     * 处理其他所有异常（兜底）
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneral(Exception e) {
        e.printStackTrace();  // 打印堆栈便于调试
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)  // 500
            .body("服务器内部错误：" + e.getMessage());
    }
}
