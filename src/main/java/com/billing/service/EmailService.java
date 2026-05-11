package com.billing.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@billingsystem.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Your BillingSystem Pro Password Reset OTP");
        message.setText("Your One-Time Password (OTP) for resetting your password is: " + otp + 
                        "\n\nThis OTP is valid for 10 minutes.\nIf you did not request a password reset, please ignore this email.");

        mailSender.send(message);
    }
}
