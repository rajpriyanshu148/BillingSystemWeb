package com.billing.controller;

import com.billing.model.OtpToken;
import com.billing.model.User;
import com.billing.repository.OtpTokenRepository;
import com.billing.repository.UserRepository;
import com.billing.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Controller responsible for handling user authentication and password recovery.
 * Provides endpoints for generating OTPs and resetting passwords securely.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for password recovery and authentication")
public class AuthController {

    private final UserRepository userRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          OtpTokenRepository otpTokenRepository,
                          EmailService emailService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpTokenRepository = otpTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Initiates the password recovery process by generating and emailing a 6-digit OTP.
     * Prevents username enumeration by returning a generic success message even if the user is not found.
     *
     * @param request A map containing the "username" of the account to recover.
     * @return ResponseEntity with a success message or an error if validation fails.
     */
    @PostMapping("/forgot-password")
    @Transactional
    @Operation(summary = "Request Password Reset OTP", description = "Generates a 6-digit OTP and sends it to the user's registered email address.")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
        }

        Optional<User> userOpt = userRepository.findByUsernameAndActiveTrue(username);
        if (userOpt.isEmpty()) {
            // Return success even if user not found to prevent username enumeration
            return ResponseEntity.ok(Map.of("success", true, "message", "If the username exists, an OTP has been sent to its registered email address."));
        }

        User user = userOpt.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            // Cannot send email if user has no email
            return ResponseEntity.badRequest().body(Map.of("error", "No email address is registered for this user."));
        }

        // Delete any existing OTPs for this email
        otpTokenRepository.deleteByEmail(user.getEmail());

        // Generate 6 digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        OtpToken token = new OtpToken(user.getEmail(), otp, 10);
        otpTokenRepository.save(token);

        try {
            emailService.sendOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to send email. Please check SMTP configuration."));
        }

        return ResponseEntity.ok(Map.of("success", true, "message", "OTP sent to registered email address."));
    }

    /**
     * Validates the provided OTP and resets the user's password if successful.
     *
     * @param request A map containing "username", "otp", and the "newPassword".
     * @return ResponseEntity indicating success or detailing validation/OTP errors.
     */
    @PostMapping("/reset-password")
    @Transactional
    @Operation(summary = "Reset Password via OTP", description = "Validates the OTP and updates the password for the specified user.")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (username == null || otp == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid request or password too short (min 6 chars)."));
        }

        Optional<User> userOpt = userRepository.findByUsernameAndActiveTrue(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid username or OTP."));
        }

        User user = userOpt.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No email address is registered for this user."));
        }

        Optional<OtpToken> tokenOpt = otpTokenRepository.findByEmailAndOtp(user.getEmail(), otp);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid OTP."));
        }

        OtpToken token = tokenOpt.get();
        if (token.isExpired()) {
            otpTokenRepository.delete(token);
            return ResponseEntity.badRequest().body(Map.of("error", "OTP has expired. Please request a new one."));
        }

        // Reset password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete used OTP
        otpTokenRepository.delete(token);

        return ResponseEntity.ok(Map.of("success", true, "message", "Password reset successfully. You can now log in."));
    }
}
