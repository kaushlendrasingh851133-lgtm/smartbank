package com.smartbank.smartbank.service;

import com.smartbank.smartbank.entity.Admin;
import com.smartbank.smartbank.repository.AdminRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder) {

        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Admin createAdmin(String username, String rawPassword) {

        if (adminRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException(
                    "Admin username already exists"
            );
        }

        Admin admin = new Admin(
                username,
                passwordEncoder.encode(rawPassword),
                "ADMIN"
        );

        return adminRepository.save(admin);
    }
}