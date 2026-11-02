package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.Account;
import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.AccountRepository;
import com.smartbank.smartbank.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public DashboardController(
            UserRepository userRepository,
            AccountRepository accountRepository) {

        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("User not found"));

        Account account = accountRepository
                .findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new IllegalStateException("Account not found"));

        model.addAttribute("user", user);
        model.addAttribute("account", account);

        return "dashboard";
    }
}