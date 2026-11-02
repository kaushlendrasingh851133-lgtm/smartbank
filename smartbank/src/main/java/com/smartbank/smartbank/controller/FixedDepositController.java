
package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.UserRepository;
import com.smartbank.smartbank.service.FixedDepositService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Locale;

@Controller
@RequestMapping("/fixed-deposit")
public class FixedDepositController {

    private final FixedDepositService fixedDepositService;
    private final UserRepository userRepository;

    public FixedDepositController(
            FixedDepositService fixedDepositService,
            UserRepository userRepository) {
        this.fixedDepositService = fixedDepositService;
        this.userRepository = userRepository;
    }

    private User loggedInUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException(
                    "Please log in to continue.");
        }

        String email = authentication.getName()
                .trim().toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException("User not found."));
    }

    @GetMapping
    public String showFDPage(
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            User user = loggedInUser(authentication);

            model.addAttribute("customerName", user.getFullName());
            model.addAttribute(
                    "fds",
                    fixedDepositService.getUserFDs(user.getId()));

            return "fixed-deposit";

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());
            return "redirect:/dashboard";
        }
    }

    @PostMapping("/create")
    public String createFD(
            @RequestParam BigDecimal amount,
            @RequestParam int durationMonths,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User user = loggedInUser(authentication);

            fixedDepositService.createFD(
                    user.getId(), amount, durationMonths);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Fixed Deposit created successfully!");

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());

        } catch (RuntimeException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "FD could not be created. Check your account and try again.");
        }

        return "redirect:/fixed-deposit";
    }

    @PostMapping("/withdraw/{id}")
    public String withdrawFD(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            User user = loggedInUser(authentication);

            fixedDepositService.withdrawMaturedFD(
                    user.getId(), id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "FD matured successfully! Principal and interest credited to your account.");

        } catch (IllegalArgumentException
                 | IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage", ex.getMessage());

        } catch (RuntimeException ex) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "FD withdrawal failed. Please try again.");
        }

        return "redirect:/fixed-deposit";
    }
}
