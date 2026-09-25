package com.pickview.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@RestControllerAdvice
public class FailureHandler {
    @ExceptionHandler(ApiFailure.class)
    public ResponseEntity<Map<String, String>> handleFailure(ApiFailure failure) {
        return ResponseEntity.status(failure.getStatus()).body(Map.of("message", failure.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException failure) {
        return ResponseEntity.badRequest().body(Map.of("message", "입력값을 확인해 주세요. / Check input values."));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, String>> handleConflict(RuntimeException failure) {
        return ResponseEntity.status(409).body(Map.of("message", "변경된 데이터입니다. 새로고침 후 다시 시도해 주세요. / Refresh and retry."));
    }
}
