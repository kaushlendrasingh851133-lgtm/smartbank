package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.dto.RegisterRequest;
import com.smartbank.smartbank.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute(
                "registerRequest",
                new RegisterRequest()
        );
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute RegisterRequest request,
            Model model) {

        try {
            userService.register(request);
            return "redirect:/login?registered";

        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("registerRequest", request);
            return "register";
        }
    }
}