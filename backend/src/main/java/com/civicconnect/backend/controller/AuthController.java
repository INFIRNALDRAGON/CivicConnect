package com.civicconnect.backend.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.civicconnect.backend.auth.AuthService;
import com.civicconnect.backend.auth.UserAccount;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginBody(String email, String password) {
    }

    public record RegisterBody(String name, String email, String password) {
    }

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginBody body) {
        return toResponse(auth.login(body.email(), body.password()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@RequestBody RegisterBody body) {
        return toResponse(auth.register(body.name(), body.email(), body.password()));
    }

    static Map<String, Object> publicUser(UserAccount u) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", u.id().toString());
        user.put("name", u.name());
        user.put("email", u.email());
        user.put("role", u.role().name());
        return user;
    }

    private static Map<String, Object> toResponse(AuthService.Session session) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("token", session.token());
        response.put("user", publicUser(session.user()));
        return response;
    }
}
