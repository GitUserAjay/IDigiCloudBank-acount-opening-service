package com.idigicloud.accountopening.service;

import com.idigicloud.accountopening.dto.request.LoginRequest;
import com.idigicloud.accountopening.dto.request.RegisterRequest;
import com.idigicloud.accountopening.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
