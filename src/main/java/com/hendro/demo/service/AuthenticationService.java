package com.hendro.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hendro.demo.dto.LoginUserDto;
import com.hendro.demo.dto.RegisterUserDto;
import com.hendro.demo.dto.VerifyUserDto;
import com.hendro.demo.model.User;
import com.hendro.demo.repository.UserRepository;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager annotationManager;
    private final EmailService emailService;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager annotationManager,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.annotationManager = annotationManager;
        this.emailService = emailService;
    }

    public User signUp(RegisterUserDto input) {
        User user = new User(input.getUsername(), input.getEmail(), passwordEncoder.encode(input.getPassword()));
        user.setVertificationCode(generateVertificationCode());
        user.setVertificationCodeExpireAt(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);
        sendVertificationEmail(user);
        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified. Please vertify your account");
        }
        annotationManager.authenticate(
                new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword()));
        return user;
    }

    public void verifyUser(VerifyUserDto input) {
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getVertificationCodeExpireAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired");

            }
            if (user.getVertificationCode().equals(input.getVertificationCode())) {
                user.setEnabled(true);
                user.setVertificationCode(null);
                user.setVertificationCodeExpireAt(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Invalid vertification code");
            }
        } else {
            throw new RuntimeException("User not found");
        }

    }

    public void resendVertificationCode(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account is already verified");
            }
            user.setVertificationCode(generateVertificationCode());
            user.setVertificationCodeExpireAt(LocalDateTime.now().plusHours(1));
            sendVertificationEmail(user);
            userRepository.save(user);

        } else {
            throw new RuntimeException("User not found");
        }
    }

    private void sendVertificationEmail(User user) {
        String subject = "Account Vertification";
        String verificationCode = "VERTIFICATION CODE" + user.getVertificationCode();
        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please enter the verification code below to continue:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<h3 style=\"color: #333;\">Verification Code:</h3>"
                + "<p style=\"font-size: 18px; font-weight: bold; color: #007bff;\">" + verificationCode + "</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVertificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateVertificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }

}
