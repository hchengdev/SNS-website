package com.snsapi.user;

import com.snsapi.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServices {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${upload.image}")
    private String uploadDir;

    public Page<User> findAll(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User findById(int id) throws UserNotFoundException {

        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public UpdateUserRequest informationUser(User request) {
        return UpdateUserRequest.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .gender(request.getGender())
                .email(request.getEmail())
                .biography(request.getBiography())
                .birthday(request.getBirthday())
                .profilePicture(request.getProfilePicture()).build();
    }

    public void save(AddUserRequest request) {
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate1 = formatter.format(date);
        LocalDate sqlDate1 = java.sql.Date.valueOf(formattedDate1).toLocalDate();
        var user = User.builder()
                .email(request.getEmail())
                .password(encodePassword(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .birthday(request.getBirthday())
                .creationDate(sqlDate1)
                .profilePicture("anh-ech-meme-hai-huoc_102044545.jpg")
                .statusFriend(StatusFriend.PUBLIC)
                .active(request.getActive() != null ? request.getActive() : true)
                .roles(new HashSet<>(Set.of(Role.ROLE_USER)))
                .build();
        userRepository.save(user);
    }

    public User saveGG(String email,String picture,String name) {
        var user = User.builder()
                .email(email)
                .password(encodePassword("123456789"))
                .name(name)
                .profilePicture(picture)
                .statusFriend(StatusFriend.PUBLIC)
                .active(true)
                .roles(new HashSet<>(Set.of(Role.ROLE_USER)))
                .build();
        return userRepository.save(user);
    }


    public void update(int id, FormUpdateRequest request) throws UserNotFoundException {
        User user = findById(id);

        user.setName(request.getName() != null ? request.getName() : user.getName());
        user.setGender(request.getGender() != null ? request.getGender() : user.getGender());
        user.setBiography(request.getBiography() != null ? request.getBiography() : user.getBiography());
        user.setPhone(request.getPhone() != null ? request.getPhone() : user.getPhone());
        user.setBirthday(request.getBirthday() != null ? request.getBirthday() : user.getBirthday());
        user.setAddress(request.getAddress() != null ? request.getAddress() : user.getAddress());

        MultipartFile profilePicture = request.getProfilePicture();
        if (profilePicture != null && !profilePicture.isEmpty()) {
            String fileName = profilePicture.getOriginalFilename();
            File file = new File(uploadDir + fileName);
            try {
                profilePicture.transferTo(file);
                user.setProfilePicture(fileName);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Error saving file");
            }
        }

        userRepository.save(user);
    }

    public void delete(int id) throws UserNotFoundException {
        findById(id);
        userRepository.deleteById(id);
    }

    public User updatePassword(int id, String newPassword, String oldPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (passwordEncoder.matches(oldPassword, user.getPassword())) {
            user.setPassword(encodePassword(newPassword));
            return userRepository.save(user);
        }
        return null;
    }

    public List<FindUserResponse> findByName(String name) throws Exception {
        if (name == null || name.isEmpty()) {
            List<User> users = userRepository.findAll();
            return users.stream()
                    .map(this::convertToFindUserRequest)
                    .collect(Collectors.toList());
        } else {
            List<User> users = userRepository.findByNameContainingIgnoreCase(name);
            if (users == null || users.isEmpty()) {
                throw new Exception("User not found");
            }
            return users.stream()
                    .map(this::convertToFindUserRequest)
                    .collect(Collectors.toList());
        }
    }

    public List<UserDTO> findUserByName (String name) throws IOException {
        if (name != null) {
            List<User> userDTOS = userRepository.findByName(name);
            return userDTOS.stream()
                    .map(this::convertToUserDTO)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private FindUserResponse convertToFindUserRequest(User user) {
        return FindUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .active(user.getActive())
                .profilePicture(user.getProfilePicture())
                .biography(user.getBiography())
                .address(user.getAddress())
                .build();
    }
    private UserDTO convertToUserDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .profilePicture(user.getProfilePicture())
                .build();
    }

    public User updateActive(Integer id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            User userEntity = user.get();
            userEntity.setActive(!userEntity.getActive());
           return userRepository.save(userEntity);
        } else {
            throw new UserNotFoundException(id);
        }
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    public List<NewUserByMonthResponse> getUserNumberByMonthOfYear(int year) {
        return  userRepository.getUserNumberByMonthOfYear(year);
    }
}

