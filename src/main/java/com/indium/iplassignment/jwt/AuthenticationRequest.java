package com.indium.iplassignment.jwt;

import lombok.Data;

@Data
public class AuthenticationRequest {
    private String username;
    private String password;

    // Constructors, getters, and setters
}
