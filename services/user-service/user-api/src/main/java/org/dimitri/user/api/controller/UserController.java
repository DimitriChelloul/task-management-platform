package org.dimitri.user.api.controller;

import org.dimitri.user.domain.port.in.UserCommandPort;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserCommandPort userCommandPort;

    public UserController(UserCommandPort userCommandPort) {
        this.userCommandPort = userCommandPort;
    }

    @PostMapping
    public Map<String, Object> create(@RequestParam String email) {
        UUID id = userCommandPort.createUser(email);
        return Map.of("id", id.toString(), "status", "created");
    }
}



