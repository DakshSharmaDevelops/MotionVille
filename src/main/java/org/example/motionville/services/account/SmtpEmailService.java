package org.example.motionville.services.account;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.report.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.UnsupportedEncodingException;
import java.net.URI;

@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String fromEmail;
    private final String fromName;
    private final String verificationUrlBase;
    private final String resetPasswordUrlBase;
    private final String frontendOrigin;
    private final String frontendPublicUrl;

    @Autowired
    public SmtpEmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${motionville.mail.enabled:false}") boolean mailEnabled,
            @Value("${motionville.mail.from:noreply@motionville.com}") String fromEmail,
            @Value("${motionville.mail.from-name:MotionVille}") String fromName,
            @Value("${motionville.mail.verification-url-base:http://localhost:5173/verify-email}") String verificationUrlBase,
            @Value("${motionville.mail.reset-password-url-base:http://localhost:5173/reset-password}") String resetPasswordUrlBase,
            @Value("${motionville.frontend-origin:http://localhost:5173}") String frontendOrigin,
            @Value("${motionville.frontend-public-url:http://localhost:5173}") String frontendPublicUrl) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.verificationUrlBase = verificationUrlBase;
        this.resetPasswordUrlBase = resetPasswordUrlBase;
        this.frontendOrigin = frontendOrigin;
        this.frontendPublicUrl = frontendPublicUrl;
    }

    public SmtpEmailService(
            JavaMailSender mailSender,
            boolean mailEnabled,
            String fromEmail,
            String fromName,
            String verificationUrlBase,
            String resetPasswordUrlBase) {
        this(mailSender, mailEnabled, fromEmail, fromName, verificationUrlBase, resetPasswordUrlBase,
                "http://localhost:5173", "http://localhost:5173");
    }

    @Override
    public void sendVerificationEmail(AppUser user, String token, String otp) {
        String verificationUrl = resolveUrl(verificationUrlBase, "/verify-email", "token", token);

        if (!mailEnabled || mailSender == null) {
            log.info("[EmailService:DEV] Registration verification for user '{}' ({}): OTP = {} | Link = {}",
                    user.getUsername(), user.getEmail(), otp != null ? otp : "N/A", verificationUrl);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setSubject("Verify your MotionVille account" + (otp != null ? " - Code: " + otp : ""));

            String otpSection = otp != null ? """
                    <div style="margin: 24px 0; padding: 20px; background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; text-align: center;">
                        <p style="margin: 0 0 8px 0; font-size: 12px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 1px;">Option 1: Enter this 6-digit code</p>
                        <span style="font-size: 32px; font-weight: 800; letter-spacing: 8px; color: #0f172a;">%s</span>
                        <p style="margin: 8px 0 0 0; font-size: 12px; color: #94a3b8;">Valid for 10 minutes</p>
                    </div>
                    <div style="text-align: center; margin: 16px 0; font-weight: 700; color: #94a3b8; font-size: 13px;">— OR —</div>
                    """.formatted(otp) : "";

            String html = """
                    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; color: #1e293b;">
                        <h2 style="color: #0f172a; margin-bottom: 16px;">Welcome to MotionVille!</h2>
                        <p style="font-size: 16px; line-height: 1.5;">Hello <strong>%s</strong>,</p>
                        <p style="font-size: 16px; line-height: 1.5;">Please verify your email address to complete your account registration. You can choose either method below:</p>
                        %s
                        <div style="margin: 20px 0; text-align: center;">
                            <p style="margin: 0 0 10px 0; font-size: 12px; font-weight: 700; color: #64748b; text-transform: uppercase; letter-spacing: 1px;">Option 2: Click to verify instantly</p>
                            <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: 600; display: inline-block;">Verify Email Address</a>
                            <p style="font-size: 13px; color: #64748b; margin-top: 14px;">Or copy and paste this URL into your browser:</p>
                            <p style="font-size: 12px; color: #2563eb; word-break: break-all;"><a href="%s" style="color: #2563eb;">%s</a></p>
                        </div>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                        <p style="font-size: 12px; color: #94a3b8;">MotionVille Team</p>
                    </div>
                    """.formatted(
                    user.getDisplayName() != null ? user.getDisplayName() : user.getUsername(),
                    otpSection,
                    verificationUrl,
                    verificationUrl,
                    verificationUrl
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent dual verification email to '{}'", user.getEmail());

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
        String resetUrl = resolveUrl(resetPasswordUrlBase, "/reset-password", "token", token);

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

    @Override
    public void sendNotificationEmail(AppUser recipient, String subject, String messageText, String actionUrl) {
        String resolvedActionUrl = actionUrl;
        if (resolvedActionUrl != null && resolvedActionUrl.startsWith("http://localhost:5173") && isNonLocalhostUrl(getFrontendBaseUrl())) {
            resolvedActionUrl = getFrontendBaseUrl() + resolvedActionUrl.substring("http://localhost:5173".length());
        }

        if (!mailEnabled || mailSender == null) {
            log.info("[EmailService:DEV] Notification email to '{}' ({}): {} | Link = {}",
                    recipient.getUsername(), recipient.getEmail(), messageText, resolvedActionUrl != null ? resolvedActionUrl : "N/A");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipient.getEmail());
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setSubject("MotionVille Notification: " + subject);

            String buttonHtml = resolvedActionUrl != null ? """
                    <div style="margin: 24px 0;">
                        <a href="%s" style="background-color: #2563eb; color: #ffffff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; display: inline-block;">View on MotionVille</a>
                    </div>
                    """.formatted(resolvedActionUrl) : "";

            String html = """
                    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; color: #1e293b;">
                        <h2 style="color: #0f172a; margin-bottom: 16px;">New Notification</h2>
                        <p style="font-size: 16px; line-height: 1.5;">Hello <strong>%s</strong>,</p>
                        <p style="font-size: 16px; line-height: 1.5; background-color: #f8fafc; padding: 16px; border-left: 4px solid #2563eb; border-radius: 4px;">%s</p>
                        %s
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                        <p style="font-size: 12px; color: #94a3b8;">MotionVille Notifications Team</p>
                    </div>
                    """.formatted(
                    recipient.getDisplayName() != null ? recipient.getDisplayName() : recipient.getUsername(),
                    messageText,
                    buttonHtml
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent notification email to '{}'", recipient.getEmail());

        } catch (Exception exception) {
            log.error("Failed to send notification email to '{}': {}", recipient.getEmail(), exception.getMessage());
        }
    }

    @Override
    public void sendReportNotificationEmail(String recipientEmail, String recipientName, Report report) {
        if (!mailEnabled || mailSender == null) {
            log.info("[EmailService:DEV] Report notification for report #{} to '{}' ({}): Reason={}, Details={}",
                    report.getId(), recipientName, recipientEmail, report.getReason(), report.getDetails());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setFrom(new InternetAddress(fromEmail, fromName));
            helper.setSubject("[MotionVille Moderation] New Report #" + report.getId() + " - " + report.getReason());

            String frontendBase = getFrontendBaseUrl();
            String contentType;
            String targetTitle;
            String targetExtra;
            String contentUrl;

            if (report.getVideo() != null) {
                contentType = "Video";
                targetTitle = report.getVideo().getTitle() + " (ID: " + report.getVideo().getVideoId() + ")";
                targetExtra = report.getVideo().getChannel() != null ? "Channel: " + report.getVideo().getChannel().getName() : "";
                contentUrl = frontendBase + "/watch/" + report.getVideo().getVideoId();
            } else if (report.getComment() != null) {
                contentType = "Comment";
                targetTitle = "Comment: \"" + escapeHtml(report.getComment().getBody()) + "\" (ID: " + report.getComment().getId() + ")";
                String authorName = report.getComment().getAuthor() != null ? report.getComment().getAuthor().getUsername() : "Unknown";
                targetExtra = "Author: " + authorName + (report.getComment().getVideo() != null ? " | Video: " + report.getComment().getVideo().getTitle() : "");
                contentUrl = report.getComment().getVideo() != null
                        ? frontendBase + "/watch/" + report.getComment().getVideo().getVideoId()
                        : frontendBase;
            } else {
                contentType = "General";
                targetTitle = "N/A";
                targetExtra = "";
                contentUrl = frontendBase;
            }

            String reporterName = report.getReporter() != null
                    ? (report.getReporter().getDisplayName() != null ? report.getReporter().getDisplayName() : report.getReporter().getUsername())
                    : "Anonymous";
            String reporterEmail = report.getReporter() != null && report.getReporter().getEmail() != null
                    ? report.getReporter().getEmail()
                    : "N/A";

            String detailsSection = (report.getDetails() != null && !report.getDetails().isBlank()) ? """
                    <tr>
                        <td style="padding: 8px 0; color: #64748b; font-weight: 600; vertical-align: top;">Reporter Notes:</td>
                        <td style="padding: 8px 0; color: #334155;">%s</td>
                    </tr>
                    """.formatted(escapeHtml(report.getDetails())) : "";

            String html = """
                    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px; color: #1e293b;">
                        <div style="border-bottom: 2px solid #ef4444; padding-bottom: 12px; margin-bottom: 20px;">
                            <span style="font-size: 12px; font-weight: 700; color: #ef4444; text-transform: uppercase; letter-spacing: 1.5px;">MotionVille Moderation Alert</span>
                            <h2 style="color: #0f172a; margin: 6px 0 0 0;">New Content Report #%d</h2>
                        </div>
                        <p style="font-size: 15px; line-height: 1.5; color: #334155;">Hello <strong>%s</strong>,</p>
                        <p style="font-size: 15px; line-height: 1.5; color: #334155;">A new report has been submitted and requires administrative review:</p>
                        
                        <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 18px; margin: 20px 0;">
                            <table style="width: 100%%; font-size: 14px; border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; width: 130px; font-weight: 600;">Report ID:</td>
                                    <td style="padding: 8px 0; color: #0f172a; font-weight: 600;">#%d</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-weight: 600;">Reason:</td>
                                    <td style="padding: 8px 0; color: #dc2626; font-weight: 700;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-weight: 600;">Target Type:</td>
                                    <td style="padding: 8px 0; color: #0f172a;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-weight: 600; vertical-align: top;">Target:</td>
                                    <td style="padding: 8px 0; color: #0f172a;">
                                        <strong>%s</strong>
                                        %s
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b; font-weight: 600;">Reported By:</td>
                                    <td style="padding: 8px 0; color: #0f172a;">%s (%s)</td>
                                </tr>
                                %s
                            </table>
                        </div>

                        <div style="margin: 28px 0; text-align: center;">
                            <a href="%s" style="background-color: #0f172a; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 6px; font-weight: 600; display: inline-block;">View Reported Content</a>
                        </div>
                        <p style="font-size: 13px; color: #64748b; margin-top: 14px; text-align: center;">Or copy and paste this link into your browser:</p>
                        <p style="font-size: 12px; color: #2563eb; word-break: break-all; text-align: center;"><a href="%s" style="color: #2563eb;">%s</a></p>

                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 24px 0;" />
                        <p style="font-size: 12px; color: #94a3b8;">MotionVille Moderation System</p>
                    </div>
                    """.formatted(
                    report.getId(),
                    recipientName != null ? recipientName : "Admin",
                    report.getId(),
                    report.getReason(),
                    contentType,
                    targetTitle,
                    targetExtra.isBlank() ? "" : "<div style=\"font-size: 12px; color: #64748b; margin-top: 2px;\">" + targetExtra + "</div>",
                    reporterName,
                    reporterEmail,
                    detailsSection,
                    contentUrl,
                    contentUrl,
                    contentUrl
            );

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent report notification email for report #{} to admin '{}' ({})", report.getId(), recipientName, recipientEmail);

        } catch (Exception exception) {
            log.error("Failed to send report notification email for report #{} to '{}': {}",
                    report.getId(), recipientEmail, exception.getMessage());
        }
    }

    public String getFrontendBaseUrl() {
        if (isNonLocalhostUrl(frontendPublicUrl)) {
            return stripTrailingSlash(frontendPublicUrl);
        }
        if (isNonLocalhostUrl(frontendOrigin)) {
            return stripTrailingSlash(frontendOrigin);
        }
        String reqOrigin = getRequestOrigin();
        if (isNonLocalhostUrl(reqOrigin)) {
            return stripTrailingSlash(reqOrigin);
        }
        if (frontendPublicUrl != null && !frontendPublicUrl.isBlank()) {
            return stripTrailingSlash(frontendPublicUrl);
        }
        if (frontendOrigin != null && !frontendOrigin.isBlank()) {
            return stripTrailingSlash(frontendOrigin);
        }
        return "http://localhost:5173";
    }

    private String getRequestOrigin() {
        try {
            RequestAttributes attribs = RequestContextHolder.getRequestAttributes();
            if (attribs instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest request = servletRequestAttributes.getRequest();
                String origin = request.getHeader("Origin");
                if (origin != null && !origin.isBlank()) {
                    return stripTrailingSlash(origin.trim());
                }
                String referer = request.getHeader("Referer");
                if (referer != null && !referer.isBlank()) {
                    try {
                        URI uri = URI.create(referer.trim());
                        String portPart = (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443)
                                ? ":" + uri.getPort() : "";
                        return uri.getScheme() + "://" + uri.getHost() + portPart;
                    } catch (Exception ignored) {
                    }
                }
                String forwardedHost = request.getHeader("X-Forwarded-Host");
                if (forwardedHost != null && !forwardedHost.isBlank()) {
                    String proto = request.getHeader("X-Forwarded-Proto");
                    String scheme = (proto != null && !proto.isBlank()) ? proto.split(",")[0].trim() : request.getScheme();
                    return scheme + "://" + forwardedHost.split(",")[0].trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isNonLocalhostUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String lower = url.trim().toLowerCase();
        return (lower.startsWith("http://") || lower.startsWith("https://"))
                && !lower.contains("localhost")
                && !lower.contains("127.0.0.1");
    }

    private String resolveUrl(String configuredUrl, String defaultPath, String paramName, String paramValue) {
        String base = configuredUrl;
        if (base == null || base.isBlank() || base.contains("localhost") || base.contains("127.0.0.1")) {
            String deployedBase = getFrontendBaseUrl();
            if (isNonLocalhostUrl(deployedBase)) {
                base = deployedBase + defaultPath;
            }
        }
        if (base == null || base.isBlank()) {
            base = "http://localhost:5173" + defaultPath;
        }
        if (!base.contains(defaultPath)) {
            base = stripTrailingSlash(base) + defaultPath;
        }
        return base + (base.contains("?") ? "&" : "?") + paramName + "=" + paramValue;
    }

    private String stripTrailingSlash(String str) {
        if (str == null) return null;
        String s = str.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
