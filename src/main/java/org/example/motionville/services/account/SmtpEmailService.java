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

    public SmtpEmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${motionville.mail.enabled:false}") boolean mailEnabled,
            @Value("${motionville.mail.from:noreply@motionville.com}") String fromEmail,
            @Value("${motionville.mail.from-name:MotionVille}") String fromName,
            @Value("${motionville.mail.verification-url-base:http://localhost:5173/verify-email}") String verificationUrlBase) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.verificationUrlBase = verificationUrlBase;
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
                        <p style="font-size: 16px; line-height: 1.5;">Thank you for registering. Please verify your email address to unlock full features including channel creation, video uploads, and live streaming.</p>
                        <div style="margin: 32px 0;">
                            <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; display: inline-block;">Verify Email Address</a>
                        </div>
                        <p style="font-size: 14px; color: #64748b;">Or paste this link into your browser:</p>
                        <p style="font-size: 13px; color: #2563eb; word-break: break-all;"><a href="%s">%s</a></p>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 32px 0;" />
                        <p style="font-size: 12px; color: #94a3b8;">This verification link will expire in 24 hours. If you did not create a MotionVille account, you can safely ignore this email.</p>
                    </div>
                    """.formatted(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                    verificationUrl, verificationUrl, verificationUrl);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent verification email to '{}'", user.getEmail());

        } catch (MessagingException | UnsupportedEncodingException | MailException exception) {
            log.error("Failed to send verification email to '{}': {}", user.getEmail(), exception.getMessage(), exception);
        }
    }
}
