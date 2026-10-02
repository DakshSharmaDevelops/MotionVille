package org.example.motionville.controllers;

import jakarta.validation.Valid;
import org.example.motionville.dto.ChannelResponse;
import org.example.motionville.dto.LoginRequest;
import org.example.motionville.dto.UserCreateRequest;
import org.example.motionville.dto.UserResponse;
import org.example.motionville.dto.UserUpdateRequest;
import org.example.motionville.services.AppUserService;
import org.example.motionville.services.ChannelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserService appUserService;
    private final ChannelService channelService;

    public AppUserController(
            AppUserService appUserService,
            ChannelService channelService) {
        this.appUserService = appUserService;
        this.channelService = channelService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        UserResponse response = appUserService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request) {

        UserResponse response =
                appUserService.login(
                        request.getUsername(),
                        request.getPassword());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id) {

        UserResponse response =
                appUserService.getUserById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        List<UserResponse> responses =
                appUserService.getAllUsers();

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {

        UserResponse response =
                appUserService.updateUser(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id) {

        appUserService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/channels")
    public ResponseEntity<List<ChannelResponse>> getUserChannels(
            @PathVariable Long userId) {

        List<ChannelResponse> responses =
                channelService.getChannelsByUserId(userId);

        return ResponseEntity.ok(responses);
    }
}