package org.dimitri.user.api.controller;

import org.dimitri.user.api.security.JwtService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public Map<String, String> login() {
        String token = jwtService.generateToken("1");
        return Map.of("token", token);
    }
}

