package com.toshit.PulseWatch.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendDownAlert(String to, String monitorName, String url, String reason) {
        send(to, "[PulseWatch] DOWN: " + monitorName,
                monitorName + " (" + url + ") is down.\nReason: " + reason);
    }

    @Async
    public void sendReminder(String to, String monitorName, String url) {
        send(to, "[PulseWatch] Still DOWN: " + monitorName,
                monitorName + " (" + url + ") is still down.");
    }

    @Async
    public void sendRecoveryAlert(String to, String monitorName, String url) {
        send(to, "[PulseWatch] Recovered: " + monitorName,
                monitorName + " (" + url + ") has been up for 5 minutes. The incident is resolved.");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}