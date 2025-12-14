package com.wts.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonResponseDto {

    private boolean success;                    // 성공 여부
    private String message;                     // 응답 메시지
    private Object data;                        // 결과 데이터
    private String taskId;                      // 작업 ID (비동기 작업용)
    private Map<String, Object> metadata;       // 추가 메타데이터
    private LocalDateTime timestamp;            // 응답 시간
    private String errorCode;                   // 에러 코드 (실패시)
}
