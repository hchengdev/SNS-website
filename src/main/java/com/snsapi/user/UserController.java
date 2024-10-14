package com.snsapi.user;

import com.snsapi.config.jwt.JwtService;
import com.snsapi.exception.UserNotFoundException;
import com.snsapi.friend.AddFriendService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class UserController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserServices userService;
    private final UserServiceInterface userDetailsService;
    private final AddFriendService addFriendService;

    @Autowired
    public UserController(AuthenticationManager authenticationManager, JwtService jwtService, UserServiceInterface userDetailsService, UserServices userService, AddFriendService addFriendService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.addFriendService = addFriendService;
    }

    @PostMapping("/api/v1/register")
    public ResponseEntity<?> register(@RequestBody AddUserRequest request) {
        try {
            userService.save(request);
            return ResponseEntity.status(HttpStatus.CREATED).body("Đăng ký thành công");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("api/v1/user/{id}")
    public ResponseEntity<User> getUserById(@PathVariable int id) {
        userService.findById(id);
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("api/v1/me")
    public ResponseEntity<?> informationUser(@RequestHeader("Authorization") String token) {
        try {
            token = token.startsWith("Bearer") ? token.substring(7) : token;
            int id = jwtService.getUserIdFromToken(token);
            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Người dùng không tồn tại");
            }
            return ResponseEntity.ok(userService.informationUser(user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token không hợp lệ");
        }
    }

    @PutMapping("/api/v1/me")
    public ResponseEntity<String> updateUser(
            @RequestHeader("Authorization") String token,
            @RequestParam String name,
            @RequestParam String phone,
            @RequestParam Gender gender,
            @RequestParam LocalDate birthday,
            @RequestParam String biography,
            @RequestParam String address,
            @RequestParam(required = false) MultipartFile profilePicture) {
        try {
            token = token.startsWith("Bearer") ? token.substring(7) : token;
            int id = jwtService.getUserIdFromToken(token);
            FormUpdateRequest updateRequest = FormUpdateRequest.builder()
                    .name(name)
                    .phone(phone)
                    .gender(gender)
                    .birthday(birthday)
                    .biography(biography)
                    .address(address)
                    .profilePicture(profilePicture)
                    .build();
            userService.update(id, updateRequest);
            return ResponseEntity.ok("User updated successfully");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An error occurred while updating user");
        }
    }

    @PutMapping("/api/v1/me/password")
    public ResponseEntity<String> updatePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdatePasswordRequest request) {
        try {
            token = token.startsWith("Bearer") ? token.substring(7) : token;
            int id = jwtService.getUserIdFromToken(token);
            if (userService.updatePassword(id, request.getNewPassword(), request.getCurrentPassword()) != null) {
                return ResponseEntity.ok("user updated successfully");
            }
            return ResponseEntity.status(500).body("Incorrect password");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
        }
    }

    @GetMapping("/api/v1/me/friends")
    public ResponseEntity<?> getFriends( @RequestHeader("Authorization") String token) {
        try {
            token = token.startsWith("Bearer")? token.substring(7) : token;
            int id = jwtService.getUserIdFromToken(token);
            List<User> findAllFriends = addFriendService.findAllFriends(id);
            if (findAllFriends.isEmpty()) {
                return ResponseEntity.ok("Không có bạn bè.");
            }
            return ResponseEntity.ok(findAllFriends);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lấy danh sách bạn bè thất bại.");
        }
    }

    @GetMapping("/api/v1/me/{id}/friends")
    public ResponseEntity<?> getFriends( @PathVariable("id") Integer friendId) {
        try {
            List<User> findAllFriends = addFriendService.findAllFriends(friendId);
            if (findAllFriends.isEmpty()) {
                return ResponseEntity.ok("Không có bạn bè.");
            }
            return ResponseEntity.ok(findAllFriends);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lấy danh sách bạn bè thất bại.");
        }
    }
}
