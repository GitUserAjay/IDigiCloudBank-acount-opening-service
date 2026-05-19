package com.idigicloud.accountopening.util;

import com.idigicloud.accountopening.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityContextUtil {

    private static UserRepository userRepository;

    public SecurityContextUtil(UserRepository repo) {
        SecurityContextUtil.userRepository = repo;
    }

    public static Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"))
                .getId();
    }
}
