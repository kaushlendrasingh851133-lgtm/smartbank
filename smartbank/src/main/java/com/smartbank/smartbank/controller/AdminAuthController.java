
package com.smartbank.smartbank.controller;

import com.smartbank.smartbank.entity.Admin;
import com.smartbank.smartbank.repository.AdminRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthController(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private Admin getLoggedInAdmin(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream()
                .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return null;
        }

        return adminRepository.findByUsername(authentication.getName())
                .filter(admin -> "ADMIN".equals(admin.getRole()))
                .orElse(null);
    }

    // ADMIN LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        String username = request.get("username");
        String password = request.get("password");

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Username and password are required"));
        }

        Admin admin = adminRepository.findByUsername(username)
                .orElse(null);

        if (admin == null
                || !passwordEncoder.matches(password, admin.getPassword())
                || !"ADMIN".equals(admin.getRole())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Invalid admin credentials"));
        }

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        admin.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin login successful");
        response.put("username", admin.getUsername());
        response.put("role", admin.getRole());
        response.put("email",
                admin.getEmail() == null ? "" : admin.getEmail());

        return ResponseEntity.ok(response);
    }

    // GET CURRENT ADMIN DETAILS
    @GetMapping("/me")
    public ResponseEntity<?> currentAdmin(Authentication authentication) {

        Admin admin = getLoggedInAdmin(authentication);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Admin login required"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("username", admin.getUsername());
        response.put("role", admin.getRole());
        response.put("email",
                admin.getEmail() == null ? "" : admin.getEmail());

        return ResponseEntity.ok(response);
    }

    // UPDATE ADMIN EMAIL
    @PostMapping("/email")
    public ResponseEntity<?> updateAdminEmail(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Admin admin = getLoggedInAdmin(authentication);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Admin login required"));
        }

        String email = request.get("email");

        if (email == null || email.isBlank()
                || !email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Please enter a valid email address"));
        }

        admin.setEmail(email.trim());
        adminRepository.save(admin);

        return ResponseEntity.ok(Map.of(
                "message", "Admin email saved successfully",
                "email", admin.getEmail()
        ));
    }

    // UPLOAD OR UPDATE ADMIN PROFILE PHOTO
    @PostMapping(
            value = "/profile-picture",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Admin admin = getLoggedInAdmin(authentication);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Admin login required"));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Please select an image"));
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Image must be 5 MB or smaller"));
        }

        String contentType = file.getContentType();

        if (contentType == null
                || !(contentType.equalsIgnoreCase("image/jpeg")
                || contentType.equalsIgnoreCase("image/png")
                || contentType.equalsIgnoreCase("image/webp")
                || contentType.equalsIgnoreCase("image/gif"))) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Only JPG, PNG, WEBP or GIF images are allowed"));
        }

        try {
            admin.setProfilePicture(file.getBytes());
            admin.setProfilePictureType(contentType);
            adminRepository.save(admin);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile picture saved successfully"));
        } catch (IOException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "message", "Unable to read the uploaded image"));
        }
    }

    // DISPLAY SAVED PROFILE PHOTO
    @GetMapping("/profile-picture")
    public ResponseEntity<?> getProfilePicture(
            Authentication authentication) {

        Admin admin = getLoggedInAdmin(authentication);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Admin login required"));
        }

        byte[] picture = admin.getProfilePicture();
        String contentType = admin.getProfilePictureType();

        if (picture == null || picture.length == 0
                || contentType == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header("Cache-Control", "no-store")
                .body(picture);
    }

    // REMOVE SAVED PROFILE PHOTO
    @DeleteMapping("/profile-picture")
    public ResponseEntity<?> removeProfilePicture(
            Authentication authentication) {

        Admin admin = getLoggedInAdmin(authentication);

        if (admin == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Admin login required"));
        }

        admin.setProfilePicture(null);
        admin.setProfilePictureType(null);
        adminRepository.save(admin);

        return ResponseEntity.ok(Map.of(
                "message", "Profile picture removed successfully"));
    }

    // ADMIN LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(
                Map.of("message", "Admin logged out successfully"));
    }
}
