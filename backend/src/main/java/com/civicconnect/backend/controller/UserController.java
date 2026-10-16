package com.civicconnect.backend.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicconnect.backend.auth.AuthService;
import com.civicconnect.backend.auth.AuthenticatedUser;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService auth;

    public UserController(AuthService auth) {
        this.auth = auth;
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal AuthenticatedUser actor) {
        return AuthController.publicUser(auth.profile(actor));
    }
}
