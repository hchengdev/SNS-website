package com.snsapi.auth;

import com.snsapi.email.EmailService;
import com.snsapi.exception.UserNotFoundException;
import com.snsapi.user.User;
import com.snsapi.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public void findPassword(String email, HttpSession session) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        Random random = new Random();
        String code = String.valueOf(random.nextInt(9000) + 1000);
        session.setAttribute("code", code);
        session.setMaxInactiveInterval(30 * 60);
        String name = user.getName();
        String subject = "Request Password Recovery";
        String text = String.format("Hello %s,\n\nYour password recovery request has been successful, please enter the following code in the Code section of the website to continue:\nCode: %s\n\nIf it's not you, there's no need to do anything.", name, code);
        emailService.sendEmail(email, subject, text);
        session.setAttribute("resetEmail", email);
    }

    public void changePassword(String email, String enteredCode, String newPassword, HttpSession session) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        String code = (String) session.getAttribute("code");
        if (code != null && code.equals(enteredCode)) {
            user.setPassword(encodePassword(newPassword));
            userRepository.save(user);
            String name = user.getName();
            String subject = "Password change successful";
            String text = String.format("Hello %s,\n\nYour password recovery request has been successful, Your new password is:\nPassword: %s\n\nPlease protect it and do not share it with anyone.", name,newPassword);
            emailService.sendEmail(email, subject, text);
        } else {
            throw new UserNotFoundException("Invalid recovery code: " + enteredCode);
        }
    }


    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

}
