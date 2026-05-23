package com.ercanbeyen.bankingapplication.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_credentials")
public class UserCredentials {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String username;
    @Column(unique = true)
    private Integer customerId;
    private String password;
    private boolean accountNonLocked = true;
    private int failedAttempt = 0;
    private LocalDateTime lockAt;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_credentials_roles",
            joinColumns = @JoinColumn(name = "user_credentials_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}
