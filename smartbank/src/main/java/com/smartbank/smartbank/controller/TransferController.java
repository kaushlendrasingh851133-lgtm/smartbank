package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.UserRepository;
import com.smartbank.smartbank.service.TransferService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
public class TransferController {

    private final TransferService transferService;
    private final UserRepository userRepository;

    public TransferController(
            TransferService transferService,
            UserRepository userRepository) {

        this.transferService = transferService;
        this.userRepository = userRepository;
    }

    @GetMapping("/transfer")
    public String showTransferPage(Model model) {
        return "transfer";
    }

    @PostMapping("/transfer")
    public String transferMoney(
            @RequestParam String receiverAccountNumber,
            @RequestParam BigDecimal amount,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            String email = authentication.getName();

            User user = userRepository.findByEmail(
                    email.trim().toLowerCase()
            ).orElseThrow(() ->
                    new IllegalArgumentException(
                            "Logged-in user was not found."
                    ));

            String reference = transferService.transfer(
                    user.getId(),
                    receiverAccountNumber,
                    amount
            );

            redirectAttributes.addFlashAttribute(
                    "transferSuccess",
                    "Transfer successful! Reference: " + reference
            );

        } catch (IllegalArgumentException ex) {

            redirectAttributes.addFlashAttribute(
                    "transferError",
                    ex.getMessage()
            );

        } catch (Exception ex) {

            redirectAttributes.addFlashAttribute(
                    "transferError",
                    "Transfer failed. Please try again."
            );
        }

        return "redirect:/transfer";
    }
}