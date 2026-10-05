package org.example.motionville.services.account;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.motionville.entity.account.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String fromEmail;
    private final String fromName;
    private final String verificationUrlBase;
    private final String resetPasswordUrlBase;

    public SmtpEmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${motionville.mail.enabled:false}") boolean mailEnabled,
            @Value("${motionville.mail.from:noreply@motionville.com}") String fromEmail,
            @Value("${motionville.mail.from-name:MotionVille}") String fromName,
            @Value("${motionville.mail.verification-url-base:http://localhost:5173/verify-email}") String verificationUrlBase,
            @Value("${motionville.mail.reset-password-url-base:http://localhost:5173/reset-password}") String resetPasswordUrlBase) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.verificationUrlBase = verificationUrlBase;
        this.resetPasswordUrlBase = resetPasswordUrlBase;
    }

    @Override
    public void sendVerificationEmail(AppUser user, String token) {
        String verificationUrl = verificationUrlBase + "?token=" + token;

        if (!mailEnabled || mailSender == null) {
            log.info("[EmailService:DEV] SMTP disabled. Verification link for user '{}' ({}): {}",
                    user.getUsername(), user.getEmail(), verificationUrl);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setSubject("Verify your MotionVille account");

            String html = """
                    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; color: #1e293b;">
                        <h2 style="color: #0f172a; margin-bottom: 16px;">Welcome to MotionVille!</h2>
                        <p style="font-size: 16px; line-height: 1.5;">Hello <strong>%s</strong>,</p>
                        <p style="font-size: 16px; line-height: 1.5;">Please verify your email address to unlock full features on MotionVille.</p>
                        <div style="margin: 32px 0;">
                            <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; display: inline-block;">Verify Email Address</a>
                        </div>
                        <p style="font-size: 14px; color: #64748b;">Or paste this link into your browser:</p>
                        <p style="font-size: 13px; color: #2563eb; word-break: break-all;"><a href="%s">%s</a></p>
                    </div>
                    """.formatted(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                    verificationUrl, verificationUrl, verificationUrl);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent verification email to '{}'", user.getEmail());

        } catch (Exception exception) {
            log.error("Failed to send verification email to '{}': {}", user.getEmail(), exception.getMessage());
            if (exception.getMessage() != null && exception.getMessage().contains("535")) {
                log.warn("[EmailService] Gmail SMTP rejected credentials (535 BadCredentials). Google requires a 16-character 'App Password', NOT your regular account password. Generate one at: https://myaccount.google.com/apppasswords");
            }
        }
    }

    @Override
    public void sendOtpEmail(String email, String otp) {
        if (!mailEnabled || mailSender == null) {
            log.info("[EmailService:DEV] Registration verification OTP for '{}': {}", email, otp);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setSubject(otp + " is your MotionVille verification code");

            String html = """
                    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; color: #1e293b;">
                        <h2 style="color: #0f172a; margin-bottom: 16px;">Verify your email address</h2>
                        <p style="font-size: 16px; line-height: 1.5;">Please use the following 6-digit verification code to complete your MotionVille account registration:</p>
                        <div style="margin: 28px 0; padding: 18px 24px; background-color: #f1f5f9; border-radius: 8px; text-align: center;">
                            <span style="font-size: 32px; font-weight: 700; letter-spacing: 6px; color: #0f172a;">%s</span>
                        </div>
                        <p style="font-size: 14px; color: #64748b;">This code will expire in 10 minutes. If you did not request this code, no account has been created and you can safely ignore this email.</p>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                        <p style="font-size: 12px; color: #94a3b8;">MotionVille Team</p>
                    </div>
                    """.formatted(otp);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent registration OTP email to '{}'", email);

        } catch (Exception exception) {
            log.error("Failed to send OTP email to '{}': {}", email, exception.getMessage());
            if (exception.getMessage() != null && exception.getMessage().contains("535")) {
                log.warn("[EmailService] Gmail SMTP rejected credentials (535 BadCredentials). Google requires a 16-character 'App Password', NOT your regular account password. Generate one at: https://myaccount.google.com/apppasswords");
            }
        }
    }

    @Override
    public void sendPasswordResetEmail(AppUser user, String token) {
        String resetUrl = resetPasswordUrlBase + "?token=" + token;

        if (!mailEnabled || mailSender == null) {
            log.info("[EmailService:DEV] SMTP disabled. Password reset link for user '{}' ({}): {}",
                    user.getUsername(), user.getEmail(), resetUrl);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setSubject("Reset your MotionVille password");

            String html = """
                    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; color: #1e293b;">
                        <h2 style="color: #0f172a; margin-bottom: 16px;">Reset Your MotionVille Password</h2>
                        <p style="font-size: 16px; line-height: 1.5;">Hello <strong>%s</strong>,</p>
                        <p style="font-size: 16px; line-height: 1.5;">We received a request to reset the password for your MotionVille account. Click the button below to choose a new password:</p>
                        <div style="margin: 32px 0;">
                            <a href="%s" style="background-color: #e11d48; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; display: inline-block;">Reset Password</a>
                        </div>
                        <p style="font-size: 14px; color: #64748b;">This link is valid for 30 minutes. If you did not request a password reset, you can safely ignore this email.</p>
                        <p style="font-size: 14px; color: #64748b; margin-top: 16px;">Or copy and paste this URL into your browser:</p>
                        <p style="font-size: 13px; color: #e11d48; word-break: break-all;"><a href="%s" style="color: #e11d48;">%s</a></p>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                        <p style="font-size: 12px; color: #94a3b8;">MotionVille Security Team</p>
                    </div>
                    """.formatted(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                    resetUrl, resetUrl, resetUrl);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent password reset email to '{}'", user.getEmail());

        } catch (Exception exception) {
            log.error("Failed to send password reset email to '{}': {}", user.getEmail(), exception.getMessage());
            if (exception.getMessage() != null && exception.getMessage().contains("535")) {
                log.warn("[EmailService] Gmail SMTP rejected credentials (535 BadCredentials). Google requires a 16-character 'App Password', NOT your regular account password. Generate one at: https://myaccount.google.com/apppasswords");
            }
        }
    }
}
