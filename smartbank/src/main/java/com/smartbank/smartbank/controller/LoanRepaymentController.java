
package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.LoanRepaymentRepository;
import com.smartbank.smartbank.repository.UserRepository;
import com.smartbank.smartbank.service.LoanRepaymentService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@RequestMapping("/loan-repayment")
public class LoanRepaymentController {

    private final LoanRepaymentRepository repaymentRepository;
    private final UserRepository userRepository;
    private final LoanRepaymentService loanRepaymentService;

    public LoanRepaymentController(
            LoanRepaymentRepository repaymentRepository,
            UserRepository userRepository,
            LoanRepaymentService loanRepaymentService) {

        this.repaymentRepository = repaymentRepository;
        this.userRepository = userRepository;
        this.loanRepaymentService = loanRepaymentService;
    }

    private User getLoggedInUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("Please log in first.");
        }

        String email = authentication.getName()
                .trim()
                .toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user not found."));
    }

    @GetMapping
    public String showRepayments(
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        try {
            User user = getLoggedInUser(authentication);

            loanRepaymentService.updateOverdueRepayments();

            model.addAttribute(
                    "repayments",
                    repaymentRepository
                            .findByLoan_User_IdOrderByDueDateAsc(
                                    user.getId()));

            model.addAttribute("customerName", user.getFullName());

            return "loan-repayment";

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                    "error", "Unable to load repayments. Please try again.");

            return "redirect:/dashboard";
        }
    }

    @PostMapping("/{repaymentId}/pay")
    public String payEmi(
            @PathVariable Long repaymentId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return "redirect:/login";
        }

        try {
            User user = getLoggedInUser(authentication);

            loanRepaymentService.payInstallment(
                    user.getId(), repaymentId);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "EMI paid successfully. Bank balance and transaction history updated.");

        } catch (IllegalArgumentException | IllegalStateException ex) {

            redirectAttributes.addFlashAttribute(
                    "error", ex.getMessage());

        } catch (RuntimeException ex) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "EMI payment failed. No successful payment was recorded. Please check your account and try again.");
        }

        return "redirect:/loan-repayment";
    }
}
