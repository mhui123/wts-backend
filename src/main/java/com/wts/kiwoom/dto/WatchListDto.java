package com.wts.kiwoom.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WatchListDto {
    private String groupId;
    private String groupName;
    private List<String> stockCodes;
    private long userId;
}
