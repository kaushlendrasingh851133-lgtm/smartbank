package com.smartbank.smartbank.config;

import com.smartbank.smartbank.entity.Admin;
import com.smartbank.smartbank.repository.AdminRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminInitializer {

    @Bean
    public CommandLineRunner initializeAdmin(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:}") String username,
            @Value("${app.admin.password:}") String password) {

        return args -> {

            if (username.isBlank() || password.isBlank()) {
                System.out.println(
                        "Admin credentials are missing in application.properties"
                );
                return;
            }

            if (adminRepository.findByUsername(username).isEmpty()) {

                Admin admin = new Admin();

                admin.setUsername(username);
                admin.setPassword(passwordEncoder.encode(password));
                admin.setRole("ADMIN");

                adminRepository.save(admin);

                System.out.println("Admin account created successfully!");

            } else {
                System.out.println("Admin account already exists.");
            }
        };
    }
}