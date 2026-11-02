package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.Account;
import com.smartbank.smartbank.entity.OtpPurpose;
import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.AccountRepository;
import com.smartbank.smartbank.repository.UserRepository;
import com.smartbank.smartbank.service.OtpService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountVerificationController {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public AccountVerificationController(
            OtpService otpService,
            UserRepository userRepository,
            AccountRepository accountRepository) {

        this.otpService = otpService;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/verify-account")
    public String showVerificationPage(
            Authentication authentication,
            Model model) {

        User user = getLoggedInUser(authentication);

        if (user == null) {
            return "redirect:/login";
        }

        Account account = accountRepository.findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new IllegalStateException("Account not found."));

        model.addAttribute("user", user);
        model.addAttribute("account", account);

        return "verify-account";
    }

    @PostMapping("/verify-account/send-otp")
    public String sendOtp(
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = getLoggedInUser(authentication);

        if (user == null) {
            return "redirect:/login";
        }

        try {
            Account account = accountRepository.findByUser_Id(user.getId())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Account not found."));

            if (account.getVerificationStatus()
                    == com.smartbank.smartbank.entity.VerificationStatus.VERIFIED) {

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "Your account is already verified."
                );

                return "redirect:/verify-account";
            }

            String otp = otpService.generateOtp(
                    user,
                    OtpPurpose.ACCOUNT_VERIFICATION
            );

            // Local development demo only. Do not expose OTPs in production.
            redirectAttributes.addFlashAttribute("demoOtp", otp);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Demo OTP generated successfully. It expires in 5 minutes."
            );

        } catch (Exception exception) {
            exception.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage() == null
                            ? "OTP could not be generated."
                            : exception.getMessage()
            );
        }

        return "redirect:/verify-account";
    }

    @PostMapping("/verify-account/confirm")
    public String confirmOtp(
            Authentication authentication,
            @RequestParam("otp") String otp,
            RedirectAttributes redirectAttributes) {

        User user = getLoggedInUser(authentication);

        if (user == null) {
            return "redirect:/login";
        }

        try {
            otpService.verifyAccountOtp(user.getId(), otp);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Account verified successfully!"
            );

            return "redirect:/dashboard";

        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage()
            );

        } catch (Exception exception) {
            exception.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Verification failed. Check the application console."
            );
        }

        return "redirect:/verify-account";
    }

    private User getLoggedInUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            return null;
        }

        return userRepository.findByEmail(authentication.getName())
                .orElse(null);
    }
}