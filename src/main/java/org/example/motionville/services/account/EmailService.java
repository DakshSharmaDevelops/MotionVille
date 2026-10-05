package org.example.motionville.services.account;

import org.example.motionville.entity.account.AppUser;

public interface EmailService {

    void sendVerificationEmail(AppUser user, String token);

    void sendOtpEmail(String email, String otp);

    void sendPasswordResetEmail(AppUser user, String token);
}
