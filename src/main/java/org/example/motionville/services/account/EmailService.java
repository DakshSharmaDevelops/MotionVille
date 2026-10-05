package org.example.motionville.services.account;

import org.example.motionville.entity.account.AppUser;

public interface EmailService {

    default void sendVerificationEmail(AppUser user, String token) {
        sendVerificationEmail(user, token, null);
    }

    void sendVerificationEmail(AppUser user, String token, String otp);

    void sendOtpEmail(String email, String otp);

    void sendPasswordResetEmail(AppUser user, String token);
}
