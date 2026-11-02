
package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.HelpRequest;
import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.HelpRequestRepository;
import com.smartbank.smartbank.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class HelpController {

    private final HelpRequestRepository helpRequestRepository;
    private final UserRepository userRepository;

    public HelpController(
            HelpRequestRepository helpRequestRepository,
            UserRepository userRepository) {
        this.helpRequestRepository = helpRequestRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/help")
    public String showHelpPage(
            Authentication authentication,
            Model model) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("Logged-in user not found"));

        model.addAttribute("helpRequest", new HelpRequest());
        model.addAttribute(
                "helpRequests",
                helpRequestRepository.findByUserOrderByCreatedAtDesc(user)
        );

        return "help";
    }

    @PostMapping("/help/submit")
    public String submitHelpRequest(
            @ModelAttribute HelpRequest helpRequest,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("Logged-in user not found"));

        if (helpRequest.getCategory() == null
                || helpRequest.getCategory().isBlank()
                || helpRequest.getSubject() == null
                || helpRequest.getSubject().isBlank()
                || helpRequest.getMessage() == null
                || helpRequest.getMessage().isBlank()) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please fill in all fields."
            );
            return "redirect:/help";
        }

        if (helpRequest.getCategory().length() > 100
                || helpRequest.getSubject().length() > 255
                || helpRequest.getMessage().length() > 2000) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "One or more fields exceed the allowed length."
            );
            return "redirect:/help";
        }

        HelpRequest request = new HelpRequest();
        request.setUser(user);
        request.setCategory(helpRequest.getCategory().trim());
        request.setSubject(helpRequest.getSubject().trim());
        request.setMessage(helpRequest.getMessage().trim());
        request.setStatus("PENDING");

        helpRequestRepository.save(request);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Your help request has been submitted successfully."
        );

        return "redirect:/help";
    }
}
