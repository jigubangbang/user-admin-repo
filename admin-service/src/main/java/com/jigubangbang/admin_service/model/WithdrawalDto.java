package com.jigubangbang.admin_service.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WithdrawalDto {
    private String userId;             
    private String reasonCode;        
    private String reasonText;        
    private LocalDateTime withdrawnAt;
    private String withdrawalType;    
}
