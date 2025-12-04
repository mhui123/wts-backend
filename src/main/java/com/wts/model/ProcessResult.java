package com.wts.model;

public class ProcessResult {
    private boolean success;
    private String message;
    private String errorCode;

    public ProcessResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ProcessResult(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
    }

    // getter, setter 메서드들
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
}
