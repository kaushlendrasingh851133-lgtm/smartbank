package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.HelpRequest;
import com.smartbank.smartbank.repository.HelpRequestRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HelpAdminController {

    private final HelpRequestRepository helpRequestRepository;

    public HelpAdminController(
            HelpRequestRepository helpRequestRepository) {
        this.helpRequestRepository = helpRequestRepository;
    }

    // Show all user help requests to admin
    @GetMapping("/admin/help-requests")
    public String showHelpRequests(Model model) {

        model.addAttribute(
                "helpRequests",
                helpRequestRepository.findAllWithUser()
        );

        return "admin/admin-help-requests";
    }

    // Admin replies to a user's help request
    @PostMapping("/admin/help-requests/{id}/reply")
    public String replyToHelpRequest(
            @PathVariable Long id,
            @RequestParam String adminReply,
            RedirectAttributes redirectAttributes) {

        if (adminReply == null || adminReply.isBlank()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please enter a reply."
            );

            return "redirect:/admin/help-requests";
        }

        if (adminReply.length() > 2000) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Reply cannot exceed 2000 characters."
            );

            return "redirect:/admin/help-requests";
        }

        HelpRequest request = helpRequestRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Help request not found"
                        )
                );

        request.setAdminReply(adminReply.trim());

        helpRequestRepository.save(request);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Reply sent successfully."
        );

        return "redirect:/admin/help-requests";
    }
}
