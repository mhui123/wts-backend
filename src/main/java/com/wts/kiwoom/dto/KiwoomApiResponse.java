package com.wts.kiwoom.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 키움 REST API 공통 응답 포맷 중 인증 판별에 사용하는 최소 필드.
 * - return_code: 결과 코드 (성공 0 등)
 * - return_msg: 결과 메시지
 * 필요 시 실제 응답 스펙에 따라 필드 추가/수정하세요.
 */
@Setter
@Getter
public class KiwoomApiResponse {
    private String return_code;
    private String return_msg;

}

