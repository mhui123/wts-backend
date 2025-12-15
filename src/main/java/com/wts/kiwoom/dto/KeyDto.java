package com.wts.kiwoom.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KeyDto {
    private Long userId;
    private String appKey;
    private String appSecret;
    private String token;
}
