package com.wts.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "wts_users")
@Getter
@Setter
public class User {

    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String provider; // e.g. "google"
    private String providerId; // provider's user id (sub)

    @Column(unique = true)
    private String email;

    private String name;
    private String pictureUrl;

    private String roles; // comma-separated roles

    private boolean enabled = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;

    public User() {}

    public User(String provider, String providerId, String email, String name, String pictureUrl) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.roles = "ROLE_USER";
    }

    public static User createGuest(String guestId) {
        User guest = new User();
        guest.setProvider("guest");
        guest.setProviderId(guestId);
        guest.setEmail(guestId + "@guest.wts");
        guest.setName("게스트_" + guestId.substring(6, 14)); // GUEST_ 제거 후 8자리
        guest.setPictureUrl(null);
        guest.setRoles("ROLE_GUEST");
        guest.setEnabled(true);
        return guest;
    }

    public boolean isGuest() {
        return "guest".equals(this.provider);
    }

    public boolean isExpiredGuest() {
        if (!isGuest()) return false;
        return this.createdAt.isBefore(LocalDateTime.now().minusHours(24));
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        lastLoginAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }


}

