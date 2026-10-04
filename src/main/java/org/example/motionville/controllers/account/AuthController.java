package org.example.motionville.controllers.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.motionville.dto.account.LoginRequest;
import org.example.motionville.dto.account.UserResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.account.RefreshToken;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.security.JwtService;
import org.example.motionville.security.RefreshTokenService;
import org.example.motionville.services.account.AppUserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserService appUserService;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(
            AuthenticationManager authenticationManager,
            AppUserService appUserService,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.appUserService = appUserService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {

        migrateLegacyPasswordIfNeeded(request.getUsername().trim(), request.getPassword());

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getUsername().trim(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username/email or password"
            );
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication failed"
            );
        }

        AppUser user = userRepository.findByUsername(authentication.getName())
                .orElseGet(() -> userRepository.findByEmail(authentication.getName())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")));

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        ResponseCookie accessCookie = jwtService.createAccessTokenCookie(accessToken);
        ResponseCookie refreshCookie = jwtService.createRefreshTokenCookie(refreshToken.getToken());

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(
                appUserService.getUserByUsername(user.getUsername())
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserResponse> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        String rawRefreshToken = jwtService.extractRefreshToken(httpRequest);
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token is missing"
            );
        }

        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(rawRefreshToken);
        AppUser user = newRefreshToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);

        ResponseCookie accessCookie = jwtService.createAccessTokenCookie(newAccessToken);
        ResponseCookie refreshCookie = jwtService.createRefreshTokenCookie(newRefreshToken.getToken());

        httpResponse.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(
                appUserService.getUserByUsername(user.getUsername())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String rawRefreshToken = jwtService.extractRefreshToken(request);
        if (rawRefreshToken != null) {
            refreshTokenService.revokeToken(rawRefreshToken);
        }

        ResponseCookie cleanAccess = jwtService.cleanAccessTokenCookie();
        ResponseCookie cleanRefresh = jwtService.cleanRefreshTokenCookie();

        response.addHeader(HttpHeaders.SET_COOKIE, cleanAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cleanRefresh.toString());

        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(
            @AuthenticationPrincipal UserDetails principal) {

        if (principal == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        return ResponseEntity.ok(
                appUserService.getUserByUsername(principal.getUsername())
        );
    }

    private void migrateLegacyPasswordIfNeeded(String usernameOrEmail, String rawPassword) {
        AppUser user = userRepository.findByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.findByEmail(usernameOrEmail).orElse(null));

        if (user == null || user.getPassword() == null) {
            return;
        }

        if (!user.getPassword().startsWith("$2")
                && user.getPassword().equals(rawPassword)) {
            String encodedPassword = passwordEncoder.encode(rawPassword);
            user.setPassword(encodedPassword);
            userRepository.save(user);
        }
    }
}
