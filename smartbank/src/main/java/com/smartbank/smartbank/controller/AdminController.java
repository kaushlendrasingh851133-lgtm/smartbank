
package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.entity.Account;
import com.smartbank.smartbank.entity.AccountStatus;
import com.smartbank.smartbank.entity.VerificationStatus;
import com.smartbank.smartbank.entity.Transaction;
import com.smartbank.smartbank.entity.FixedDeposit;
import com.smartbank.smartbank.entity.Loan;
import com.smartbank.smartbank.entity.LoanRepayment;

import com.smartbank.smartbank.repository.UserRepository;
import com.smartbank.smartbank.repository.AccountRepository;
import com.smartbank.smartbank.repository.TransactionRepository;
import com.smartbank.smartbank.repository.FixedDepositRepository;
import com.smartbank.smartbank.repository.LoanRepository;
import com.smartbank.smartbank.repository.LoanRepaymentRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FixedDepositRepository fixedDepositRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;

    public AdminController(
            UserRepository userRepository,
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            FixedDepositRepository fixedDepositRepository,
            LoanRepository loanRepository,
            LoanRepaymentRepository loanRepaymentRepository) {

        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fixedDepositRepository = fixedDepositRepository;
        this.loanRepository = loanRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
    }

    // CUSTOMER MANAGEMENT
    @GetMapping("/customers")
    public String customers(
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        List<User> customers = userRepository.findAll();

        if (search != null && !search.trim().isEmpty()) {
            String keyword = search.trim().toLowerCase(Locale.ROOT);

            customers = customers.stream()
                    .filter(user ->
                            (user.getFullName() != null
                                    && user.getFullName()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(keyword))
                                    ||
                                    (user.getEmail() != null
                                            && user.getEmail()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("customers", customers);
        model.addAttribute("search", search == null ? "" : search);

        return "admin/customers";
    }

    // ACCOUNT MANAGEMENT
    @GetMapping("/accounts")
    public String accounts(
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        List<Account> accounts = accountRepository.findAllWithUser();

        if (search != null && !search.trim().isEmpty()) {
            String keyword = search.trim().toLowerCase(Locale.ROOT);

            accounts = accounts.stream()
                    .filter(account ->
                            (account.getAccountNumber() != null
                                    && account.getAccountNumber()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(keyword))
                                    ||
                                    (account.getUser() != null
                                            && account.getUser().getFullName() != null
                                            && account.getUser().getFullName()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword))
                                    ||
                                    (account.getUser() != null
                                            && account.getUser().getEmail() != null
                                            && account.getUser().getEmail()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("accounts", accounts);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("totalAccounts", accountRepository.count());

        model.addAttribute("pendingVerification",
                accountRepository.countByVerificationStatus(
                        VerificationStatus.PENDING));

        model.addAttribute("verifiedAccounts",
                accountRepository.countByVerificationStatus(
                        VerificationStatus.VERIFIED));

        model.addAttribute("rejectedAccounts",
                accountRepository.countByVerificationStatus(
                        VerificationStatus.REJECTED));

        return "admin/accounts";
    }

    @PostMapping("/accounts/verification")
    public String updateVerification(
            @RequestParam Long accountId,
            @RequestParam VerificationStatus status,
            RedirectAttributes redirectAttributes) {

        Account account = accountRepository.findById(accountId).orElse(null);

        if (account == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Account not found.");
            return "redirect:/admin/accounts";
        }

        account.setVerificationStatus(status);
        accountRepository.save(account);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Verification status updated successfully.");

        return "redirect:/admin/accounts";
    }

    @PostMapping("/accounts/status")
    public String updateAccountStatus(
            @RequestParam Long accountId,
            @RequestParam AccountStatus status,
            RedirectAttributes redirectAttributes) {

        Account account = accountRepository.findById(accountId).orElse(null);

        if (account == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Account not found.");
            return "redirect:/admin/accounts";
        }

        account.setAccountStatus(status);
        accountRepository.save(account);

        redirectAttributes.addFlashAttribute(
                "successMessage", "Account status updated successfully.");

        return "redirect:/admin/accounts";
    }

    // TRANSACTION MANAGEMENT
    @GetMapping("/transactions")
    public String transactions(
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        List<Transaction> allTransactions =
                transactionRepository.findAllWithAccounts();

        List<Transaction> transactions = allTransactions;

        if (search != null && !search.trim().isEmpty()) {
            String keyword = search.trim().toLowerCase(Locale.ROOT);

            transactions = allTransactions.stream()
                    .filter(t ->
                            (t.getReferenceNumber() != null
                                    && t.getReferenceNumber()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(keyword))
                                    ||
                                    (t.getTransactionType() != null
                                            && t.getTransactionType()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword))
                                    ||
                                    (t.getStatus() != null
                                            && t.getStatus()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword))
                                    ||
                                    (t.getDescription() != null
                                            && t.getDescription()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword))
                                    ||
                                    (t.getSenderAccount() != null
                                            && t.getSenderAccount().getAccountNumber() != null
                                            && t.getSenderAccount().getAccountNumber()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword))
                                    ||
                                    (t.getReceiverAccount() != null
                                            && t.getReceiverAccount().getAccountNumber() != null
                                            && t.getReceiverAccount().getAccountNumber()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("transactions", transactions);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("totalTransactions", allTransactions.size());

        model.addAttribute("successfulTransactions",
                allTransactions.stream()
                        .filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus()))
                        .count());

        model.addAttribute("failedTransactions",
                allTransactions.stream()
                        .filter(t -> "FAILED".equalsIgnoreCase(t.getStatus()))
                        .count());

        model.addAttribute("pendingTransactions",
                allTransactions.stream()
                        .filter(t -> "PENDING".equalsIgnoreCase(t.getStatus()))
                        .count());

        return "admin/transaction-monitoring";
    }

    // FIXED DEPOSIT DETAILS
    @GetMapping("/fixed-deposits")
    public String fixedDeposits(
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        List<FixedDeposit> allDeposits =
                fixedDepositRepository.findAllWithUserAndAccount();

        List<FixedDeposit> fixedDeposits = allDeposits;

        if (search != null && !search.trim().isEmpty()) {
            String keyword = search.trim().toLowerCase(Locale.ROOT);

            fixedDeposits = allDeposits.stream()
                    .filter(fd ->
                            (fd.getUser() != null
                                    && fd.getUser().getFullName() != null
                                    && fd.getUser().getFullName()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(keyword))
                                    ||
                                    (fd.getUser() != null
                                            && fd.getUser().getEmail() != null
                                            && fd.getUser().getEmail()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword))
                                    ||
                                    (fd.getAccount() != null
                                            && fd.getAccount().getAccountNumber() != null
                                            && fd.getAccount().getAccountNumber()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword))
                                    ||
                                    (fd.getStatus() != null
                                            && fd.getStatus().name()
                                            .toLowerCase(Locale.ROOT)
                                            .contains(keyword)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("fixedDeposits", fixedDeposits);
        model.addAttribute("search", search == null ? "" : search);
        model.addAttribute("totalFDs", allDeposits.size());

        model.addAttribute("activeFDs",
                allDeposits.stream()
                        .filter(fd -> fd.getStatus() != null
                                && "ACTIVE".equals(fd.getStatus().name()))
                        .count());

        model.addAttribute("maturedFDs",
                allDeposits.stream()
                        .filter(fd -> fd.getStatus() != null
                                && "MATURED".equals(fd.getStatus().name()))
                        .count());

        return "admin/fd-details";
    }

    // LOANS AND REPAYMENTS - GROUPED BY CUSTOMER
    @GetMapping("/loan-repayments")
    public String loanRepayments(
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        // Fetch every loan, including loans without repayment records.
        List<Loan> allLoans = loanRepository.findAllByOrderByAppliedAtDesc();

        // Fetch repayment records with their associated loans and customers.
        List<LoanRepayment> allRepayments =
                loanRepaymentRepository.findAllWithLoanAndUser();

        String keyword = search == null
                ? ""
                : search.trim().toLowerCase(Locale.ROOT);

        List<Loan> filteredLoans = allLoans.stream()
                .filter(loan -> {
                    if (keyword.isEmpty()) {
                        return true;
                    }

                    String customerName = loan.getUser().getFullName();
                    String email = loan.getUser().getEmail();
                    String loanType = loan.getLoanType();
                    String loanId = String.valueOf(loan.getId());
                    String loanStatus = loan.getStatus() == null
                            ? ""
                            : loan.getStatus().name();

                    boolean loanMatches =
                            (customerName != null
                                    && customerName.toLowerCase(Locale.ROOT)
                                    .contains(keyword))
                                    ||
                                    (email != null
                                            && email.toLowerCase(Locale.ROOT)
                                            .contains(keyword))
                                    ||
                                    (loanType != null
                                            && loanType.toLowerCase(Locale.ROOT)
                                            .contains(keyword))
                                    ||
                                    loanId.contains(keyword)
                                    ||
                                    loanStatus.toLowerCase(Locale.ROOT)
                                            .contains(keyword);

                    boolean repaymentMatches = allRepayments.stream()
                            .filter(r -> r.getLoan() != null
                                    && r.getLoan().getId().equals(loan.getId()))
                            .anyMatch(r ->
                                    String.valueOf(r.getInstallmentNumber())
                                            .contains(keyword)
                                            ||
                                            (r.getPaymentStatus() != null
                                                    && r.getPaymentStatus().name()
                                                    .toLowerCase(Locale.ROOT)
                                                    .contains(keyword)));

                    return loanMatches || repaymentMatches;
                })
                .collect(Collectors.toList());

        // Group loans by customer ID.
        Map<Long, List<Loan>> loansByCustomer =
                filteredLoans.stream()
                        .filter(loan -> loan.getUser() != null)
                        .collect(Collectors.groupingBy(
                                loan -> loan.getUser().getId(),
                                LinkedHashMap::new,
                                Collectors.toList()));

        // Group repayment records by loan ID.
        Map<Long, List<LoanRepayment>> repaymentsByLoan =
                allRepayments.stream()
                        .filter(r -> r.getLoan() != null)
                        .collect(Collectors.groupingBy(
                                r -> r.getLoan().getId(),
                                LinkedHashMap::new,
                                Collectors.toList()));

        model.addAttribute("loansByCustomer", loansByCustomer);
        model.addAttribute("repaymentsByLoan", repaymentsByLoan);
        model.addAttribute("search", search == null ? "" : search);

        model.addAttribute("totalLoans", allLoans.size());
        model.addAttribute("totalRepayments", allRepayments.size());

        model.addAttribute("paidRepayments",
                allRepayments.stream()
                        .filter(r -> r.getPaymentStatus() != null
                                && "PAID".equals(r.getPaymentStatus().name()))
                        .count());

        model.addAttribute("pendingRepayments",
                allRepayments.stream()
                        .filter(r -> r.getPaymentStatus() != null
                                && "PENDING".equals(r.getPaymentStatus().name()))
                        .count());

        return "admin/loan-repayments";
    }

    // REPORTS & ANALYTICS
    @GetMapping("/reports-analytics")
    public String reportsAnalytics(Model model) {

        List<Transaction> transactions = transactionRepository.findAll();
        List<Account> accounts = accountRepository.findAll();
        List<User> customers = userRepository.findAll();

        long successfulTransactions = transactions.stream()
                .filter(t -> "SUCCESS".equalsIgnoreCase(t.getStatus()))
                .count();

        long failedTransactions = transactions.stream()
                .filter(t -> "FAILED".equalsIgnoreCase(t.getStatus()))
                .count();

        long pendingTransactions = transactions.stream()
                .filter(t -> "PENDING".equalsIgnoreCase(t.getStatus()))
                .count();

        Map<String, Long> typeCounts = new LinkedHashMap<>();

        typeCounts.put("Deposit", transactions.stream()
                .filter(t -> "DEPOSIT".equalsIgnoreCase(t.getTransactionType()))
                .count());

        typeCounts.put("Withdrawal", transactions.stream()
                .filter(t -> "WITHDRAW".equalsIgnoreCase(t.getTransactionType())
                        || "WITHDRAWAL".equalsIgnoreCase(t.getTransactionType()))
                .count());

        typeCounts.put("Transfer", transactions.stream()
                .filter(t -> "TRANSFER".equalsIgnoreCase(t.getTransactionType()))
                .count());

        Map<String, Long> monthlyCounts = new LinkedHashMap<>();
        YearMonth currentMonth = YearMonth.now();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);

            long count = transactions.stream()
                    .filter(t -> t.getCreatedAt() != null)
                    .filter(t -> YearMonth.from(t.getCreatedAt()).equals(month))
                    .count();

            monthlyCounts.put(month.format(formatter), count);
        }

        long verifiedAccounts = accounts.stream()
                .filter(a -> a.getVerificationStatus()
                        == VerificationStatus.VERIFIED)
                .count();

        long pendingAccounts = accounts.stream()
                .filter(a -> a.getVerificationStatus()
                        == VerificationStatus.PENDING)
                .count();

        long rejectedAccounts = accounts.stream()
                .filter(a -> a.getVerificationStatus()
                        == VerificationStatus.REJECTED)
                .count();

        model.addAttribute("totalCustomers", customers.size());
        model.addAttribute("totalAccounts", accounts.size());
        model.addAttribute("totalTransactions", transactions.size());

        model.addAttribute("successfulTransactions", successfulTransactions);
        model.addAttribute("failedTransactions", failedTransactions);
        model.addAttribute("pendingTransactions", pendingTransactions);

        model.addAttribute("verifiedAccounts", verifiedAccounts);
        model.addAttribute("pendingAccounts", pendingAccounts);
        model.addAttribute("rejectedAccounts", rejectedAccounts);

        model.addAttribute("typeLabels", new ArrayList<>(typeCounts.keySet()));
        model.addAttribute("typeValues", new ArrayList<>(typeCounts.values()));

        model.addAttribute("monthlyLabels", new ArrayList<>(monthlyCounts.keySet()));
        model.addAttribute("monthlyValues", new ArrayList<>(monthlyCounts.values()));

        return "admin/reports-analytics";
    }
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        return "admin-dashboard";
    }

    // ADMIN SETTINGS
    @GetMapping("/settings")
    public String settings() {
        return "admin/settings";
    }
}
