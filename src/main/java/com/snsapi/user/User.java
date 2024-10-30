package com.snsapi.user;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.snsapi.chat.ChatMessage;
import com.snsapi.friend.AddFriend;
import com.snsapi.post.Post;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.*;

@Data
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Email
    @Column(unique = true, nullable = false, length = 50)
    private String email;

    @JsonProperty("password")
    @Column(nullable = true, length = 255)
    private String password;

    @JsonProperty("name")
    @Column(nullable = true, length = 255)
    private String name;

    @JsonProperty("gender")
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private Gender gender;

    @JsonProperty("profilePicture")
    private String profilePicture;

    @JsonProperty("phone")
    @Column(nullable = true)
    private String phone;

    @JsonProperty("active")
    @Column(nullable = false)
    private Boolean active;

    @JsonProperty("biography")
    @Column(nullable = true, length = 255)
    private String biography;

    @JsonProperty("birthday")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Column(nullable = true)
    private LocalDate birthday;


    @JsonProperty("address")
    @Column(nullable = true, length = 255)
    private String address;

    @JsonProperty("creationDate")
    @Column(nullable = true)
    private LocalDate creationDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    List<Post> posts = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    public boolean isFriend(User user) {
        return friends.stream().anyMatch(f -> f.getFriend().getId().equals(user.getId()));
    }
    @OneToMany(mappedBy = "sender")
    private Set<ChatMessage> sentMessages;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (Role role : roles) {
            authorities.add(new SimpleGrantedAuthority(role.name()));
        }
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonBackReference
    private List<AddFriend> friends = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status_friend", nullable = false)
    private StatusFriend statusFriend = StatusFriend.PUBLIC;

}


