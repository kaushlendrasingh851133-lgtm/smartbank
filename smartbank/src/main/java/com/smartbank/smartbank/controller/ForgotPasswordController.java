
package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.UserRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Optional;

@Controller
public class ForgotPasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final String EMAIL = "resetEmail";
    private static final String OTP = "resetOtp";
    private static final String EXPIRY = "otpExpiry";
    private static final String ATTEMPTS = "otpAttempts";
    private static final String VERIFIED = "otpVerified";

    public ForgotPasswordController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Open Forgot Password Page
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    // Generate OTP
    @PostMapping("/forgot-password")
    public String generateOtp(
            @RequestParam String email,
            HttpSession session,
            Model model) {

        email = email.trim().toLowerCase();

        Optional<User> user = userRepository.findByEmail(email);

        if (user.isEmpty()) {
            model.addAttribute(
                    "error",
                    "No account found with this email address."
            );
            return "forgot-password";
        }

        // Generate six digit OTP
        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1000000)
        );

        session.setAttribute(EMAIL, email);
        session.setAttribute(OTP, otp);
        session.setAttribute(
                EXPIRY,
                System.currentTimeMillis() + 5 * 60 * 1000
        );
        session.setAttribute(ATTEMPTS, 0);
        session.setAttribute(VERIFIED, false);

        // Display verification code on screen
        model.addAttribute("generatedOtp", otp);

        model.addAttribute(
                "success",
                "Verification code generated successfully."
        );

        return "forgot-password";
    }

    // Verify OTP
    @PostMapping("/verify-otp")
    public String verifyOtp(
            @RequestParam String otp,
            HttpSession session,
            Model model) {

        String savedOtp = (String) session.getAttribute(OTP);
        Long expiry = (Long) session.getAttribute(EXPIRY);

        if (savedOtp == null || expiry == null) {
            model.addAttribute(
                    "error",
                    "Please generate a new OTP."
            );
            return "forgot-password";
        }

        // Check OTP expiry
        if (System.currentTimeMillis() > expiry) {

            clearResetSession(session);

            model.addAttribute(
                    "error",
                    "OTP expired. Please generate a new OTP."
            );

            return "forgot-password";
        }

        Integer attempts = (Integer) session.getAttribute(ATTEMPTS);

        if (attempts == null) {
            attempts = 0;
        }

        // Maximum five attempts
        if (attempts >= 5) {

            clearResetSession(session);

            model.addAttribute(
                    "error",
                    "Too many incorrect attempts. Generate a new OTP."
            );

            return "forgot-password";
        }

        session.setAttribute(ATTEMPTS, attempts + 1);

        // Check entered OTP
        if (!savedOtp.equals(otp.trim())) {

            model.addAttribute(
                    "error",
                    "Invalid OTP. Please try again."
            );

            // Keep OTP and input box visible
            model.addAttribute("generatedOtp", savedOtp);

            return "forgot-password";
        }

        // OTP verified successfully
        session.removeAttribute(OTP);
        session.removeAttribute(EXPIRY);
        session.removeAttribute(ATTEMPTS);

        session.setAttribute(VERIFIED, true);

        return "redirect:/reset-password";
    }

    // Open New Password Page
    @GetMapping("/reset-password")
    public String resetPasswordPage(HttpSession session) {

        Boolean verified = (Boolean) session.getAttribute(VERIFIED);

        if (!Boolean.TRUE.equals(verified)) {
            return "redirect:/forgot-password";
        }

        return "reset-password";
    }

    // Update Password
    @PostMapping("/complete-password-reset")
    public String updatePassword(
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            Model model) {

        Boolean verified = (Boolean) session.getAttribute(VERIFIED);

        if (!Boolean.TRUE.equals(verified)) {
            return "redirect:/forgot-password";
        }

        if (newPassword.length() < 8 || newPassword.length() > 72) {

            model.addAttribute(
                    "error",
                    "Password must be between 8 and 72 characters."
            );

            return "reset-password";
        }

        if (!newPassword.equals(confirmPassword)) {

            model.addAttribute(
                    "error",
                    "Passwords do not match."
            );

            return "reset-password";
        }

        String email = (String) session.getAttribute(EMAIL);

        if (email == null) {
            return "redirect:/forgot-password";
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {

            clearResetSession(session);

            return "redirect:/forgot-password";
        }

        User user = optionalUser.get();

        user.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);

        clearResetSession(session);

        return "redirect:/login?passwordReset";
    }

    // Clear OTP and reset information
    private void clearResetSession(HttpSession session) {

        session.removeAttribute(EMAIL);
        session.removeAttribute(OTP);
        session.removeAttribute(EXPIRY);
        session.removeAttribute(ATTEMPTS);
        session.removeAttribute(VERIFIED);
    }
}
