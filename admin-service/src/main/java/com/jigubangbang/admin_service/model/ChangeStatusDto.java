package com.jigubangbang.admin_service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeStatusDto {
    private String status;  // ACTIVE, BANNED, WITHDRAWN
}
