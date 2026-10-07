package org.example.motionville.services.account;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.report.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.net.URI;

@Service
public class SmtpEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private volatile TemplateEngine fallbackTemplateEngine;
    private final boolean mailEnabled;
    private final String fromEmail;
    private final String fromName;
    private final String frontendBaseUrl;
    private final String verificationUrlBase;
    private final String resetPasswordUrlBase;

    @Autowired
    public SmtpEmailService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${motionville.mail.enabled:false}") boolean mailEnabled,
            @Value("${motionville.mail.from:noreply@motionville.com}") String fromEmail,
            @Value("${motionville.mail.from-name:MotionVille}") String fromName,
            @Value("${motionville.mail.frontend-url:${motionville.frontend-public-url:${motionville.frontend-origin:}}}") String frontendBaseUrl,
            @Value("${motionville.mail.verification-url-base:}") String verificationUrlBase,
            @Value("${motionville.mail.reset-password-url-base:}") String resetPasswordUrlBase) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.mailEnabled = mailEnabled;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.frontendBaseUrl = frontendBaseUrl;
        this.verificationUrlBase = verificationUrlBase;
        this.resetPasswordUrlBase = resetPasswordUrlBase;
    }

    @Override
    public void sendVerificationEmail(AppUser user, String token, String otp) {
        String verificationUrl = buildUrl(verificationUrlBase, "/verify-email", "token", token);

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

            Context context = new Context();
            context.setVariable("recipientName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            context.setVariable("otp", otp);
            context.setVariable("verificationUrl", verificationUrl);

            String html = renderTemplate("email/verification-email", context);

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

            Context context = new Context();
            context.setVariable("otp", otp);

            String html = renderTemplate("email/otp-email", context);

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
        String resetUrl = buildUrl(resetPasswordUrlBase, "/reset-password", "token", token);

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

            Context context = new Context();
            context.setVariable("recipientName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
            context.setVariable("resetUrl", resetUrl);

            String html = renderTemplate("email/password-reset-email", context);

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
        if (resolvedActionUrl != null && resolvedActionUrl.startsWith("/")) {
            resolvedActionUrl = getFrontendBaseUrl() + resolvedActionUrl;
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

            Context context = new Context();
            context.setVariable("recipientName", recipient.getDisplayName() != null ? recipient.getDisplayName() : recipient.getUsername());
            context.setVariable("messageText", messageText);
            context.setVariable("actionUrl", resolvedActionUrl);

            String html = renderTemplate("email/notification-email", context);

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
                targetTitle = "Comment: \"" + report.getComment().getBody() + "\" (ID: " + report.getComment().getId() + ")";
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

            Context context = new Context();
            context.setVariable("recipientName", recipientName != null ? recipientName : "Admin");
            context.setVariable("reportId", report.getId());
            context.setVariable("reason", report.getReason() != null ? report.getReason().name() : "N/A");
            context.setVariable("contentType", contentType);
            context.setVariable("targetTitle", targetTitle);
            context.setVariable("targetExtra", targetExtra);
            context.setVariable("reporterName", reporterName);
            context.setVariable("reporterEmail", reporterEmail);
            context.setVariable("details", report.getDetails());
            context.setVariable("contentUrl", contentUrl);

            String html = renderTemplate("email/report-notification-email", context);

            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent report notification email for report #{} to admin '{}' ({})", report.getId(), recipientName, recipientEmail);

        } catch (Exception exception) {
            log.error("Failed to send report notification email for report #{} to '{}': {}",
                    report.getId(), recipientEmail, exception.getMessage());
        }
    }

    private String getFrontendBaseUrl() {
        if (frontendBaseUrl != null && !frontendBaseUrl.isBlank()) {
            return stripTrailingSlash(frontendBaseUrl);
        }
        String reqOrigin = getRequestOrigin();
        if (reqOrigin != null && !reqOrigin.isBlank()) {
            return stripTrailingSlash(reqOrigin);
        }
        return "";
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

    private String buildUrl(String configuredBase, String defaultPath, String paramName, String paramValue) {
        String base = configuredBase;
        if (base == null || base.isBlank()) {
            String frontendBase = getFrontendBaseUrl();
            base = stripTrailingSlash(frontendBase) + defaultPath;
        } else if (!base.contains(defaultPath)) {
            base = stripTrailingSlash(base) + defaultPath;
        }
        return base + (base.contains("?") ? "&" : "?") + paramName + "=" + paramValue;
    }

    private String renderTemplate(String templateName, Context context) {
        return getTemplateEngine().process(templateName, context);
    }

    private TemplateEngine getTemplateEngine() {
        if (templateEngine != null) {
            return templateEngine;
        }
        if (fallbackTemplateEngine == null) {
            synchronized (this) {
                if (fallbackTemplateEngine == null) {
                    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
                    resolver.setPrefix("templates/");
                    resolver.setSuffix(".html");
                    resolver.setTemplateMode(TemplateMode.HTML);
                    resolver.setCharacterEncoding("UTF-8");
                    resolver.setCacheable(false);
                    SpringTemplateEngine engine = new SpringTemplateEngine();
                    engine.setTemplateResolver(resolver);
                    fallbackTemplateEngine = engine;
                }
            }
        }
        return fallbackTemplateEngine;
    }

    private String stripTrailingSlash(String str) {
        if (str == null) return "";
        String s = str.trim();
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }
}
