
package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.Loan;
import com.smartbank.smartbank.entity.LoanStatus;
import com.smartbank.smartbank.entity.RepaymentStatus;
import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.LoanRepository;
import com.smartbank.smartbank.repository.LoanRepaymentRepository;
import com.smartbank.smartbank.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/loans")
public class LoanController {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final LoanRepaymentRepository repaymentRepository;

    public LoanController(
            LoanRepository loanRepository,
            UserRepository userRepository,
            LoanRepaymentRepository repaymentRepository) {

        this.loanRepository = loanRepository;
        this.userRepository = userRepository;
        this.repaymentRepository = repaymentRepository;
    }

    private User getLoggedInUser(Authentication authentication) {
        String email = authentication.getName()
                .trim()
                .toLowerCase(Locale.ROOT);

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user not found"));
    }

    private boolean hasUnfinishedLoan(User user) {

        List<Loan> loans =
                loanRepository.findByUserOrderByAppliedAtDesc(user);

        for (Loan loan : loans) {

            if (loan.getStatus() == LoanStatus.PENDING) {
                return true;
            }

            if (loan.getStatus() == LoanStatus.APPROVED) {

                long unpaidInstallments =
                        repaymentRepository.countByLoanAndPaymentStatus(
                                loan,
                                RepaymentStatus.PENDING);

                if (unpaidInstallments > 0) {
                    return true;
                }

                // If an approved loan has no repayment records,
                // block another application rather than assume it is paid.
                if (unpaidInstallments == 0
                        && repaymentRepository
                        .findByLoanOrderByInstallmentNumberAsc(loan)
                        .isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    @GetMapping
    public String showLoanPage(
            Authentication authentication,
            Model model) {

        User user = getLoggedInUser(authentication);

        List<Loan> loans =
                loanRepository.findByUserOrderByAppliedAtDesc(user);

        model.addAttribute("loans", loans);
        model.addAttribute("customerName", user.getFullName());
        model.addAttribute("hasUnfinishedLoan",
                hasUnfinishedLoan(user));

        return "loans/apply";
    }

    @PostMapping("/apply")
    @Transactional
    public String applyForLoan(
            @RequestParam String loanType,
            @RequestParam BigDecimal loanAmount,
            @RequestParam BigDecimal monthlyIncome,
            @RequestParam Integer durationMonths,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        User user = getLoggedInUser(authentication);

        if (hasUnfinishedLoan(user)) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "You already have an active loan application or unpaid loan. Complete all repayments before applying for another loan."
            );
            return "redirect:/loans";
        }

        if (loanType == null ||
                !List.of("PERSONAL", "HOME", "EDUCATION", "VEHICLE")
                        .contains(loanType.trim().toUpperCase(Locale.ROOT))) {

            redirectAttributes.addFlashAttribute(
                    "error", "Please select a valid loan type.");
            return "redirect:/loans";
        }

        if (loanAmount == null ||
                loanAmount.compareTo(BigDecimal.ZERO) <= 0 ||
                loanAmount.compareTo(new BigDecimal("100000000")) > 0 ||
                loanAmount.scale() > 2) {

            redirectAttributes.addFlashAttribute(
                    "error", "Enter a valid loan amount.");
            return "redirect:/loans";
        }

        if (monthlyIncome == null ||
                monthlyIncome.compareTo(BigDecimal.ZERO) <= 0 ||
                monthlyIncome.compareTo(new BigDecimal("100000000")) > 0 ||
                monthlyIncome.scale() > 2) {

            redirectAttributes.addFlashAttribute(
                    "error", "Enter a valid monthly income.");
            return "redirect:/loans";
        }

        if (durationMonths == null ||
                durationMonths < 1 ||
                durationMonths > 360) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    "Loan duration must be between 1 and 360 months.");
            return "redirect:/loans";
        }

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setLoanType(
                loanType.trim().toUpperCase(Locale.ROOT));
        loan.setLoanAmount(loanAmount);
        loan.setMonthlyIncome(monthlyIncome);
        loan.setDurationMonths(durationMonths);
        loan.setStatus(LoanStatus.PENDING);

        loanRepository.save(loan);

        redirectAttributes.addFlashAttribute(
                "success",
                "Loan application submitted successfully!");

        return "redirect:/loans";
    }
}
