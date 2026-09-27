package com.pickview.api;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class FailureHandler {

    @ExceptionHandler(ApiFailure.class)
    public ResponseEntity<Map<String, String>> handleFailure(ApiFailure failure) {
        return ResponseEntity.status(failure.getStatus()).body(Map.of("message", failure.getMessage()));
    }

    @ExceptionHandler({ MethodArgumentNotValidException.class, HttpMessageNotReadableException.class })
    public ResponseEntity<Map<String, String>> handleValidation(Exception failure) {
        return ResponseEntity.badRequest().body(Map.of("message", "입력값을 확인해 주세요. / Check input values."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUploadLimit(MaxUploadSizeExceededException failure) {
        return ResponseEntity.status(413).body(Map.of("message", "최대 100MB 파일을 선택하세요. / Choose a file up to 100MB."));
    }

    @ExceptionHandler({ DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class })
    public ResponseEntity<Map<String, String>> handleConflict(RuntimeException failure) {
        return ResponseEntity.status(409).body(Map.of("message", "변경된 데이터입니다. 새로고침 후 다시 시도해 주세요. / Refresh and retry."));
    }
}
