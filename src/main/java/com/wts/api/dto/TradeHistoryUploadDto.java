package com.wts.api.dto;

import com.wts.model.TradeRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeHistoryUploadDto {

    private String userId;                      // 사용자 ID
    private String uploadType;                  // 업로드 유형 (FULL, INCREMENTAL 등)
    private LocalDateTime uploadTime;           // 업로드 시간
    private List<TradeRecord> tradeRecords;     // 거래 내역 리스트
    private String sourceSystem;                // 소스 시스템 (KIWOOM, MANUAL 등)
    private MultipartFile file;                 // 업로드 파일

}
