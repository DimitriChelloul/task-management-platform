package org.dimitri.user.api.controller;

import org.dimitri.user.api.security.JwtService;
import org.dimitri.user.application.UserService;
import org.dimitri.user.domain.User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserService users;

    public AuthController(JwtService jwtService, UserService users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestParam String email) {
        User user = users.authenticate(email);
        return Map.of("token", jwtService.generateToken(user.id().toString()));
    }
}

