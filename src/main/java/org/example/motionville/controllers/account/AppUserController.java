package org.example.motionville.controllers.account;

import jakarta.validation.Valid;
import org.example.motionville.dto.account.ChangePasswordRequest;
import org.example.motionville.dto.channel.ChannelResponse;
import org.example.motionville.dto.account.UserCreateRequest;
import org.example.motionville.dto.account.UserResponse;
import org.example.motionville.dto.account.UserUpdateRequest;
import org.example.motionville.services.account.AppUserService;
import org.example.motionville.services.channel.ChannelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

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

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#id, authentication)")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        requireCurrentUser(id, principal);

        UserResponse response =
                appUserService.getUserById(id);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {

        List<UserResponse> responses =
                appUserService.getAllUsers();

        return ResponseEntity.ok(responses);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#id, authentication)")
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        requireCurrentUser(id, principal);

        UserResponse response =
                appUserService.updateUser(id, request);

        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#id, authentication)")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails principal) {

        requireCurrentUser(id, principal);
        appUserService.resetPasswordForUser(id, request.getNewPassword());

        return ResponseEntity.ok(Map.of("message", "Password successfully reset"));
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#id, authentication)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {

        requireCurrentUser(id, principal);
        appUserService.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @GetMapping("/{userId}/channels")
    public ResponseEntity<List<ChannelResponse>> getUserChannels(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails principal) {

        requireCurrentUser(userId, principal);

        List<ChannelResponse> responses =
                channelService.getChannelsByUserId(userId);

        return ResponseEntity.ok(responses);
    }
    private void requireCurrentUser(Long id, UserDetails principal) {
        if (principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return;
        }

        UserResponse currentUser =
                appUserService.getUserByUsername(principal.getUsername());

        if (!id.equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only access your own user account"
            );
        }
    }

}