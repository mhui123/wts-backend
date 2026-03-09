package com.wts.summary.adapter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wts.summary.dto.TradeHistoryJsonRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeHistoryNdjsonAdapter {

    private static final String DEFAULT_MESSAGE = "변환완료";

    private final ObjectMapper objectMapper;

    public ParseResult parse(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return ParseResult.empty("NDJSON 응답이 비어 있습니다.");
            }

            String message = DEFAULT_MESSAGE;
            int totalCount = 0;
            List<TradeHistoryJsonRecord> records = new ArrayList<>();

            boolean firstLineConsumed = false;
            try {
                Map<String, Object> meta = objectMapper.readValue(firstLine, new TypeReference<>() {});
                if (meta != null && meta.containsKey("success")) {
                    Object metaMessage = meta.get("message");
                    if (metaMessage != null) {
                        message = metaMessage.toString();
                    }
                    Object data = meta.get("data");
                    if (data instanceof Map<?, ?> dataMap) {
                        Object rowCount = dataMap.get("row_count");
                        if (rowCount != null) {
                            totalCount = Integer.parseInt(rowCount.toString());
                        }
                    }
                    firstLineConsumed = true;
                }
            } catch (Exception ignore) {
                // First line may be data; parse below.
            }

            if (!firstLineConsumed) {
                records.add(objectMapper.readValue(firstLine, TradeHistoryJsonRecord.class));
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    records.add(objectMapper.readValue(line, TradeHistoryJsonRecord.class));
                } catch (Exception e) {
                    log.warn("NDJSON 레코드 파싱 실패: {}", e.getMessage());
                }
            }

            return new ParseResult(records, totalCount, message, false);
        }
    }

    public record ParseResult(List<TradeHistoryJsonRecord> records, int totalCount, String message, boolean empty) {
        public static ParseResult empty(String message) {
            return new ParseResult(List.of(), 0, message, true);
        }
    }
}

