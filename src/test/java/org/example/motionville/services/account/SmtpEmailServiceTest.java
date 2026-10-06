package org.example.motionville.services.account;

import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.report.Report;
import org.example.motionville.entity.report.enums.ReportReason;
import org.example.motionville.entity.video.Video;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailServiceTest {

    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenAnswer(invocation ->
                new MimeMessage(Session.getInstance(new Properties())));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private String extractContent(Object content) throws Exception {
        if (content instanceof String s) {
            return s;
        } else if (content instanceof Multipart multipart) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                sb.append(extractContent(multipart.getBodyPart(i).getContent()));
            }
            return sb.toString();
        }
        return content != null ? content.toString() : "";
    }

    @Test
    void verificationEmailUsesConfiguredDeployedFrontendUrl() throws Exception {
        SmtpEmailService emailService = new SmtpEmailService(
                mailSender,
                null,
                true,
                "noreply@motionville.com",
                "MotionVille",
                "https://motionville-app.vercel.app",
                "https://motionville-app.vercel.app/verify-email",
                "https://motionville-app.vercel.app/reset-password"
        );

        AppUser user = new AppUser();
        user.setUsername("testuser");
        user.setEmail("user@example.com");

        emailService.sendVerificationEmail(user, "testToken123", "123456");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        String content = extractContent(sentMessage.getContent());

        assertTrue(content.contains("https://motionville-app.vercel.app/verify-email?token=testToken123"));
        assertFalse(content.contains("http://localhost:5173"));
    }

    @Test
    void passwordResetEmailUsesConfiguredDeployedFrontendUrl() throws Exception {
        SmtpEmailService emailService = new SmtpEmailService(
                mailSender,
                null,
                true,
                "noreply@motionville.com",
                "MotionVille",
                "https://motionville-app.vercel.app",
                "https://motionville-app.vercel.app/verify-email",
                "https://motionville-app.vercel.app/reset-password"
        );

        AppUser user = new AppUser();
        user.setUsername("testuser");
        user.setEmail("user@example.com");

        emailService.sendPasswordResetEmail(user, "resetTokenXYZ");

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        String content = extractContent(sentMessage.getContent());

        assertTrue(content.contains("https://motionville-app.vercel.app/reset-password?token=resetTokenXYZ"));
        assertFalse(content.contains("http://localhost:5173"));
    }

    @Test
    void dynamicOriginFromHttpRequestIsUsedWhenConfiguredIsEmpty() throws Exception {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Origin", "https://deployed.motionville.internal");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        SmtpEmailService emailService = new SmtpEmailService(
                mailSender,
                null,
                true,
                "noreply@motionville.com",
                "MotionVille",
                null,
                null,
                null
        );

        AppUser user = new AppUser();
        user.setUsername("dynamicuser");
        user.setEmail("dynamic@example.com");

        emailService.sendVerificationEmail(user, "dynToken789", null);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        String content = extractContent(sentMessage.getContent());

        assertTrue(content.contains("https://deployed.motionville.internal/verify-email?token=dynToken789"));
    }

    @Test
    void sendReportNotificationEmailDispatchesVideoReportCorrectly() throws Exception {
        SmtpEmailService emailService = new SmtpEmailService(
                mailSender,
                null,
                true,
                "noreply@motionville.com",
                "MotionVille",
                "https://app.motionville.com",
                "https://app.motionville.com/verify-email",
                "https://app.motionville.com/reset-password"
        );

        AppUser reporter = new AppUser();
        reporter.setId(5L);
        reporter.setUsername("reporterGuy");
        reporter.setEmail("reporter@example.com");

        Channel channel = new Channel();
        channel.setName("Test Channel");

        Video video = new Video();
        video.setVideoId(99L);
        video.setTitle("Inappropriate Content Video");
        video.setChannel(channel);

        Report report = new Report();
        report.setId(101L);
        report.setReporter(reporter);
        report.setVideo(video);
        report.setReason(ReportReason.HATE);
        report.setDetails("Violates community guidelines on hate speech");

        emailService.sendReportNotificationEmail("admin@motionville.com", "Admin Name", report);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertTrue(sentMessage.getSubject().contains("Report #101"));
        assertTrue(sentMessage.getSubject().contains("HATE"));

        String content = extractContent(sentMessage.getContent());
        assertTrue(content.contains("Inappropriate Content Video"));
        assertTrue(content.contains("reporterGuy"));
        assertTrue(content.contains("https://app.motionville.com/watch/99"));
        assertTrue(content.contains("Violates community guidelines on hate speech"));
    }

    @Test
    void sendReportNotificationEmailDispatchesCommentReportCorrectly() throws Exception {
        SmtpEmailService emailService = new SmtpEmailService(
                mailSender,
                null,
                true,
                "noreply@motionville.com",
                "MotionVille",
                "https://app.motionville.com",
                "https://app.motionville.com/verify-email",
                "https://app.motionville.com/reset-password"
        );

        AppUser reporter = new AppUser();
        reporter.setUsername("watcher");
        reporter.setEmail("watcher@example.com");

        AppUser commentAuthor = new AppUser();
        commentAuthor.setUsername("spammer123");

        Video video = new Video();
        video.setVideoId(55L);
        video.setTitle("Great Video");

        Comment comment = new Comment();
        comment.setId(77L);
        comment.setBody("Buy crypto here now!");
        comment.setAuthor(commentAuthor);
        comment.setVideo(video);

        Report report = new Report();
        report.setId(102L);
        report.setReporter(reporter);
        report.setComment(comment);
        report.setReason(ReportReason.SPAM);
        report.setDetails("Repeated crypto spam");

        emailService.sendReportNotificationEmail("admin@motionville.com", "Admin", report);

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage sentMessage = messageCaptor.getValue();
        String content = extractContent(sentMessage.getContent());
        assertTrue(content.contains("Buy crypto here now!"));
        assertTrue(content.contains("spammer123"));
        assertTrue(content.contains("https://app.motionville.com/watch/55"));
    }
}
