package com.wts.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonRequestDto {

    private String taskType;        // 작업 유형 (예: "data_analysis", "ml_prediction" 등)
    private String command;         // 실행할 명령
    private Map<String, Object> parameters;  // 작업 파라미터
    private long userId;          // 요청 사용자 ID (옵션)
    private boolean async;          // 비동기 실행 여부
}