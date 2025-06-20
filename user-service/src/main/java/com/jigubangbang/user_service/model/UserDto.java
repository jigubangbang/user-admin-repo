package com.jigubangbang.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;     // user.user_id
    private String name;   // user.name
    private String role;   // user.role
}
