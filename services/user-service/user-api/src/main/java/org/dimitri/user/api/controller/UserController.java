package org.dimitri.user.api.controller;

import org.dimitri.user.application.UserService;
import org.dimitri.user.domain.User;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public List<User> list() {
        return users.list();
    }

    @GetMapping("/{id}")
    public User get(@PathVariable UUID id) {
        return users.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@RequestParam String email) {
        return users.create(email);
    }
}



