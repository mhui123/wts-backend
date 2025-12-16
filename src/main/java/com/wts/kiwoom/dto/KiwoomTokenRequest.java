package com.wts.kiwoom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class KiwoomTokenRequest {
    @NotBlank
    private String kiwoomToken;
}
