package org.example.motionville.services.account;

import org.example.motionville.entity.account.AppUser;

public interface EmailService {

    void sendVerificationEmail(AppUser user, String token);
}
