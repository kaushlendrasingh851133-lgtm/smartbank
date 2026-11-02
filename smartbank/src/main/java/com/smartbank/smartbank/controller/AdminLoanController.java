package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.Loan;
import com.smartbank.smartbank.entity.LoanStatus;
import com.smartbank.smartbank.repository.LoanRepository;
import com.smartbank.smartbank.service.LoanApprovalService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/loans")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLoanController {

    private final LoanRepository loanRepository;
    private final LoanApprovalService loanApprovalService;

    public AdminLoanController(
            LoanRepository loanRepository,
            LoanApprovalService loanApprovalService) {
        this.loanRepository = loanRepository;
        this.loanApprovalService = loanApprovalService;
    }

    @GetMapping
    public String showLoans(Model model) {
        model.addAttribute(
                "loans",
                loanRepository.findAllByOrderByAppliedAtDesc()
        );
        model.addAttribute(
                "pendingCount",
                loanRepository.countByStatus(LoanStatus.PENDING)
        );
        model.addAttribute(
                "approvedCount",
                loanRepository.countByStatus(LoanStatus.APPROVED)
        );
        model.addAttribute(
                "rejectedCount",
                loanRepository.countByStatus(LoanStatus.REJECTED)
        );

        return "admin/loans";
    }

    @PostMapping("/{id}/approve")
    public String approveLoan(
            @PathVariable Long id,
            @RequestParam(required = false) String adminRemark,
            RedirectAttributes redirectAttributes) {

        try {
            loanApprovalService.approveLoan(id, adminRemark);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Loan approved, amount credited and EMI schedule generated."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Loan approval failed: " + e.getMessage()
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Loan approval failed. Please check application logs."
            );
        }

        return "redirect:/admin/loans";
    }

    @PostMapping("/{id}/reject")
    public String rejectLoan(
            @PathVariable Long id,
            @RequestParam(required = false) String adminRemark,
            RedirectAttributes redirectAttributes) {

        try {
            Loan loan = loanApprovalService.rejectLoan(id, adminRemark);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Loan application rejected successfully."
            );
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Loan rejection failed: " + e.getMessage()
            );
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Loan rejection failed. Please check application logs."
            );
        }

        return "redirect:/admin/loans";
    }
}
