package com.wts.kiwoom.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class KiwoomApiRequest {
    private Long userId;
    private String appKey;
    private String appSecret;
    private String token;
}

