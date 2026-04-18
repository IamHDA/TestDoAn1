package com.vn.backend.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vn.backend.entities.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

@AllArgsConstructor
@Data
public class CustomUserDetails implements UserDetails {
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String email;
    private String phone;
    private boolean isActive;
    private String code;
    private String fullName;
    private String avatarUrl;    //  ảnh
    private Boolean isDeleted;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private LocalDateTime lastLogin;
    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }
      // Tạo CustomUserDetails từ User entity
      public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.phone = user.getPhone();
      }

    public CustomUserDetails(User user,
                             Collection<? extends GrantedAuthority> authorities) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.gender = user.getGender();
        this.address = user.getAddress();
        this.avatarUrl = user.getAvatarUrl();
        this.isActive=user.getIsActive();
        this.isDeleted=user.getIsDeleted();
        this.dateOfBirth=user.getDateOfBirth();
        this.code = user.getCode();
        this.fullName = user.getFullName();
        this.authorities = authorities;
    }


    @Override
    public String getPassword() {
        return this.password;
    }
    @Override
    public String getUsername() {
        return this.username;
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
        return true;
    }
}