package com.snsapi.auth;

import com.snsapi.config.jwt.JwtResponse;
import com.snsapi.config.jwt.JwtService;
import com.snsapi.email.EmailService;
import com.snsapi.exception.UserNotFoundException;
import com.snsapi.user.User;
import com.snsapi.user.UserServiceInterface;
import com.snsapi.user.UserServices;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private HttpSession httpSession;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserServiceInterface userDetailsService;
    private final UserServices userServices;
    private final AuthService authService;
    private final EmailService emailService;

    @Value("${GOOGLE_APPLICATION_CLIENT_ID}")
    private String clientId;

    @Value("${GOOGLE_APPLICATION_CLIENT_SECRET}")
    private String clientSecret;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, UserServiceInterface userDetailsService, UserServices userServices, AuthService authService, EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userServices = userServices;
        this.authService = authService;
        this.emailService = emailService;
    }

    @PostMapping("/api/v1/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        Optional<User> currentUser = userDetailsService.findByUserEmail(user.getEmail());

        if (!currentUser.isPresent()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Người dùng không tìm thấy");
        }

        if (!currentUser.get().getActive()) {
            return ResponseEntity.status(403).body("Người dùng bị ban vĩnh viễn");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtService.generateTokenLogin(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();


            return ResponseEntity.ok(new JwtResponse(currentUser.get().getId(), jwt, userDetails.getUsername(), currentUser.get().getName(), currentUser.get().getProfilePicture(), currentUser.get().getRoles().toString()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Thông tin đăng nhập không chính xác");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/v1/auth/google")
    public ResponseEntity<String> googleLogin() {
        try {
            String redirectUri = "http://localhost:8080/auth/google/callback";
            String scope = "profile email";

            String encodedScope = URLEncoder.encode(scope, StandardCharsets.UTF_8.toString());
            String redirectUrl = "https://accounts.google.com/o/oauth2/auth?" +
                    "client_id=" + clientId +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString()) +
                    "&response_type=code" +
                    "&scope=" + encodedScope;

            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/auth/google/callback")
    public ResponseEntity<Map<String, Object>> googleCallback(@RequestParam String code) {
        try {
            String accessToken = getAccessToken(code);
            Map<String, Object> userAttributes = getUserInfo(accessToken);
            String email = (String) userAttributes.get("email");
            String picture = (String) userAttributes.get("picture");
            String name = (String) userAttributes.get("name");

            User currentUser;
            if (userServices.findByEmail(email).isEmpty()) {
                currentUser = userServices.saveGG(email, picture, name);
            } else {
                currentUser = userServices.findByEmail(email).get();
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String jwt = jwtService.generateTokenLogin(authentication);
            JwtResponse jwtResponse = new JwtResponse(
                    currentUser.getId(),
                    jwt,
                    userDetails.getUsername(),
                    currentUser.getName(),
                    currentUser.getProfilePicture(),
                    currentUser.getRoles().toString()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("redirectUrl", "http://localhost:3002");
            response.put("userDetails", jwtResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }

    private String getAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("code", code);
        requestBody.add("client_id", clientId);
        requestBody.add("client_secret", clientSecret);
        requestBody.add("redirect_uri", "http://localhost:8080/auth/google/callback");
        requestBody.add("grant_type", "authorization_code");

        ResponseEntity<Map> response = restTemplate.postForEntity("https://oauth2.googleapis.com/token", requestBody, Map.class);
        return (String) response.getBody().get("access_token");
    }

    private Map<String, Object> getUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> userInfoResponse = restTemplate.exchange("https://www.googleapis.com/oauth2/v3/userinfo", HttpMethod.GET, entity, Map.class);
        return userInfoResponse.getBody();
    }

    @PostMapping("/v1/auth/find-email")
    public ResponseEntity<String> findPassword(@RequestParam String email) {
        try {
            authService.findPassword(email, httpSession);
            return ResponseEntity.ok("Password recovery email has been sent.");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(404).body("User not found with the provided email.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while processing your request.");
        }
    }

    @PostMapping("/v1/auth/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String code, @RequestParam String newPassword) {
        String email = (String) httpSession.getAttribute("resetEmail");
        if (email == null) {
            return ResponseEntity.status(400).body("No email found in session.");
        }
        try {
            authService.changePassword(email, code, newPassword,httpSession);
            httpSession.removeAttribute("resetEmail");
            httpSession.removeAttribute("code");
            return ResponseEntity.ok("Password has been successfully reset.");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(404)
                    .body("Invalid recovery code: " + code);
        }
    }
}


