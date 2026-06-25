package com.cantor.journal.notification;

import com.cantor.journal.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * 인앱 알림을 저장하고, SMTP가 설정되어 있으면(app.mail.enabled=true) 이메일도 발송한다.
 * 이메일 발송 실패는 알림 저장에 영향을 주지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@cantor.org}")
    private String from;

    @Transactional
    public void notify(User recipient, NotificationType type, String message, String link) {
        if (recipient == null) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .recipient(recipient)
                .type(type)
                .message(message)
                .link(link)
                .read(false)
                .build());
        sendEmail(recipient, message, link);
    }

    @Transactional
    public void notifyAll(Collection<User> recipients, NotificationType type, String message, String link) {
        for (User u : recipients) {
            notify(u, type, message, link);
        }
    }

    private void sendEmail(User recipient, String message, String link) {
        if (!mailEnabled || recipient.getEmail() == null) {
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            return;
        }
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(from);
            mail.setTo(recipient.getEmail());
            mail.setSubject("[Cantor Journal] 알림");
            mail.setText(message + (link != null ? "\n\n링크: " + link : ""));
            sender.send(mail);
        } catch (Exception e) {
            log.warn("이메일 발송 실패: {}", recipient.getEmail(), e);
        }
    }
}
