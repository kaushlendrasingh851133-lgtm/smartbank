
package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.Admin;
import com.smartbank.smartbank.entity.LoanStatus;

import com.smartbank.smartbank.repository.AdminRepository;
import com.smartbank.smartbank.repository.UserRepository;
import com.smartbank.smartbank.repository.AccountRepository;
import com.smartbank.smartbank.repository.TransactionRepository;
import com.smartbank.smartbank.repository.LoanRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class AdminPageController {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final PasswordEncoder passwordEncoder;

    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AdminPageController(
            AdminRepository adminRepository,
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            LoanRepository loanRepository,
            PasswordEncoder passwordEncoder) {

        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.loanRepository = loanRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin-login";
    }

    @PostMapping("/admin/login")
    public String loginAdmin(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        Optional<Admin> result =
                adminRepository.findByUsername(username);

        if (result.isEmpty()
                || !passwordEncoder.matches(
                password, result.get().getPassword())
                || !"ADMIN".equals(result.get().getRole())) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Invalid admin username or password.");

            return "redirect:/admin/login?error";
        }

        Admin admin = result.get();

        request.changeSessionId();

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(
                context, request, response);

        return "redirect:/admin/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {

        // Total registered customers
        model.addAttribute(
                "totalCustomers",
                userRepository.count()
        );

        // Total bank accounts
        model.addAttribute(
                "totalAccounts",
                accountRepository.count()
        );

        // Total transactions
        model.addAttribute(
                "totalTransactions",
                transactionRepository.count()
        );

        // Pending loan applications
        model.addAttribute(
                "pendingLoans",
                loanRepository.countByStatus(LoanStatus.PENDING)
        );

        // Approved loan applications
        model.addAttribute(
                "approvedLoans",
                loanRepository.countByStatus(LoanStatus.APPROVED)
        );

        // Rejected loan applications
        model.addAttribute(
                "rejectedLoans",
                loanRepository.countByStatus(LoanStatus.REJECTED)
        );

        return "admin-dashboard";
    }
}
