package com.snsapi.auth;

import com.snsapi.config.jwt.JwtResponse;
import com.snsapi.config.jwt.JwtService;
import com.snsapi.user.User;
import com.snsapi.user.UserServiceInterface;
import com.snsapi.user.UserServices;
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
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserServiceInterface userDetailsService;

    private final UserServices userServices;

    @Value("${GOOGLE_APPLICATION_CLIENT_ID}")
    private String clientId;

    @Value("${GOOGLE_APPLICATION_CLIENT_SECRET}")
    private String clientSecret;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService, UserServiceInterface userDetailsService, UserServices userServices) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userServices = userServices;
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

            return ResponseEntity.ok(new JwtResponse(currentUser.get().getId(), jwt,userDetails.getUsername(), currentUser.get().getName(), currentUser.get().getProfilePicture(), currentUser.get().getRoles().toString()));
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
    public ResponseEntity<Map<String, Serializable>> googleCallback(@RequestParam String code) {
        try {
            String accessToken = getAccessToken(code);
            Map<String, Object> userAttributes = getUserInfo(accessToken);
            String email = (String) userAttributes.get("email");
            if (userServices.findByEmail(email).isEmpty()) {
                User user = userServices.saveGG(email);
                System.out.println(userServices.findByEmail(email));
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(Map.of("email", email, "password", user.getPassword()));
            } else {
                Long password = 123456789L;
                return ResponseEntity.ok(Map.of("email", email, "password", password));
            }
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
}


