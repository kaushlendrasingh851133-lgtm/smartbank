package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.Account;
import com.smartbank.smartbank.entity.Transaction;
import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.AccountRepository;
import com.smartbank.smartbank.repository.TransactionRepository;
import com.smartbank.smartbank.repository.UserRepository;
import com.smartbank.smartbank.service.BankingService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class BankingController {

    private final BankingService bankingService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public BankingController(
            BankingService bankingService,
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {

        this.bankingService = bankingService;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/deposit")
    public String showDepositPage() {
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(
            @RequestParam BigDecimal amount,
            Authentication authentication,
            Model model) {

        try {
            User user = getAuthenticatedUser(authentication);

            bankingService.deposit(user.getId(), amount);

            return "redirect:/dashboard?depositSuccess";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "deposit";
        }
    }

    @GetMapping("/withdraw")
    public String showWithdrawPage() {
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(
            @RequestParam BigDecimal amount,
            Authentication authentication,
            Model model) {

        try {
            User user = getAuthenticatedUser(authentication);

            bankingService.withdraw(user.getId(), amount);

            return "redirect:/dashboard?withdrawSuccess";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "withdraw";
        }
    }

    @GetMapping("/transactions")
    public String transactionHistory(
            Authentication authentication,
            Model model) {

        User user = getAuthenticatedUser(authentication);

        Account account = accountRepository
                .findByUser_Id(user.getId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account not found."
                        ));

        List<Transaction> transactions =
                transactionRepository
                        .findBySenderAccount_IdOrReceiverAccount_IdOrderByCreatedAtDesc(
                                account.getId(),
                                account.getId()
                        );

        model.addAttribute("user", user);
        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);

        return "transactions";
    }

    private User getAuthenticatedUser(
            Authentication authentication) {

        if (authentication == null) {
            throw new IllegalArgumentException(
                    "Please log in first."
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found."
                        ));
    }
}