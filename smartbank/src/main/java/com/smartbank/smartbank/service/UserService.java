package com.smartbank.smartbank.service;

import com.smartbank.smartbank.dto.RegisterRequest;
import com.smartbank.smartbank.entity.Account;
import com.smartbank.smartbank.entity.AccountType;
import com.smartbank.smartbank.entity.AccountStatus;
import com.smartbank.smartbank.entity.VerificationStatus;
import com.smartbank.smartbank.entity.User;
import com.smartbank.smartbank.repository.AccountRepository;
import com.smartbank.smartbank.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(RegisterRequest request) {

        // Validate full name
        if (request.getFullName() == null
                || request.getFullName().isBlank()) {
            throw new IllegalArgumentException(
                    "Full name is required."
            );
        }

        // Validate email
        if (request.getEmail() == null
                || request.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required."
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase();

        // Validate password
        if (request.getPassword() == null
                || request.getPassword().length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters."
            );
        }

        // Check duplicate email
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email is already registered."
            );
        }

        // Create user
        User user = new User();

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);

        // Secure password hashing
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole("USER");

        User savedUser = userRepository.save(user);

        // Create bank account
        Account account = new Account();

        String accountNumber;

        do {
            StringBuilder number = new StringBuilder("1");

            for (int i = 0; i < 15; i++) {
                number.append(secureRandom.nextInt(10));
            }

            accountNumber = number.toString();

        } while (accountRepository
                .findByAccountNumber(accountNumber)
                .isPresent());

        account.setAccountNumber(accountNumber);
        account.setBalance(BigDecimal.ZERO);

        // Default account type
        AccountType selectedType = request.getAccountType();

        if (selectedType == null) {
            selectedType = AccountType.SAVINGS;
        }

        account.setAccountType(selectedType);

        // Account requires verification
        account.setVerificationStatus(
                VerificationStatus.PENDING
        );

        account.setAccountStatus(AccountStatus.ACTIVE);

        account.setUser(savedUser);

        accountRepository.save(account);

        return savedUser;
    }
}