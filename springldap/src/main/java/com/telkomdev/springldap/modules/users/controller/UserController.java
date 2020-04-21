package com.telkomdev.springldap.modules.users.controller;

import com.telkomdev.springldap.modules.users.domain.User;
import com.telkomdev.springldap.shared.EmptyJson;
import com.telkomdev.springldap.shared.Response;
import com.telkomdev.springldap.shared.Result;
import com.telkomdev.springldap.shared.service.LdapClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    @Autowired
    private LdapClient ldapClient;

    @GetMapping("/")
    public Response index(Model model) {
        return new Response(HttpStatus.OK.value(), true, new EmptyJson(), "server up and running");
    }

    @GetMapping("/users/{name}")
    public User getUser(@PathVariable String name) {
        User user = new User(name, name + "@yahoo.com", name, "1234");
        return user;
    }

    @PostMapping("/users/auth")
    public ResponseEntity<String> login(@RequestBody User user) {
        if (user.getUsername().equals("") && user.getPassword().equals("")) {
            return ResponseEntity.badRequest().body("data cannot be empty");
        }

        Boolean success = ldapClient.authenticate(user.getUsername(), user.getPassword());
        if (!success) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid username or password");
        }

        return ResponseEntity.ok("login success");
    }

    @PostMapping("/users/create")
    public ResponseEntity<String> create(@RequestBody User user) {
        if (user.getUsername().equals("") && user.getPassword().equals("") && user.getEmail().equals("") && user.getFullName().equals("")) {
            return ResponseEntity.badRequest().body("data cannot be empty");
        }

        Result<User, String> result = ldapClient.create(user);
        if (result.getData() == null) {
            return ResponseEntity.badRequest().body(result.getError());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body("success create new user");
    }
}
