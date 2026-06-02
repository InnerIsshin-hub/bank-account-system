package com.student.bank.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUser {
    private Long userId;
    private String userName;
    private String role;
    private Integer tokenVersion;
}
