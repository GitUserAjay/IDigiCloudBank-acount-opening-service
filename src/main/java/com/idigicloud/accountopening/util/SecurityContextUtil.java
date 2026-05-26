package com.idigicloud.accountopening.util;

import com.idigicloud.accountopening.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Utility bean for resolving the authenticated user's DB id from a JWT principal.
 *
 * NOTE: Previously used a static field for UserRepository which caused a NullPointerException
 * because Lombok's @RequiredArgsConstructor only injects non-static final fields.
 * Fixed by making userRepository a non-static final field so Spring injects it correctly.
 */
@Component
@RequiredArgsConstructor
public class SecurityContextUtil {

    // ✅ non-static + final → Lombok generates a one-arg constructor → Spring injects this correctly
    private final UserRepository userRepository;

    /**
     * Returns the DB primary key (id) of the currently authenticated user.
     *
     * @param userDetails the principal injected via @AuthenticationPrincipal
     * @return the user's Long id
     * @throws RuntimeException if the authenticated email is not found in the database
     */
    public Long getCurrentUserId(UserDetails userDetails) {
        return userRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException(
                        "Authenticated user not found in DB for email: " + userDetails.getUsername()))
                .getId();
    }
}
