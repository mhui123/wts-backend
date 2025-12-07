package com.wts.model;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResult {
    private boolean success;
    private String message;
    private String errorCode;
    private int processedCount;
    private int totalCount;
    private Object data;

    public ProcessResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ProcessResult(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
    }
}