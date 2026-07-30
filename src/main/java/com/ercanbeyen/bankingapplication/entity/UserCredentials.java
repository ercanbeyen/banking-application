package com.ercanbeyen.bankingapplication.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_credentials")
public class UserCredentials {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Getter
    @Setter
    @Column(unique = true)
    private String username;
    @Getter
    @Setter
    @Column(unique = true)
    private Integer customerId;
    @Getter
    @Setter
    private String password;
    @Getter
    @Setter
    private boolean accountNonLocked = true;
    @Getter
    @Setter
    private Integer failedAttempt = 0;
    @Getter
    @Setter
    private LocalDateTime lockAt;
    @Getter
    @Setter
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    @Getter
    @Setter
    private Integer passwordRenewalPeriod;
    @Getter
    @Setter
    private Instant passwordUpdatedAt;
    @ElementCollection
    @CollectionTable(
            name = "user_passwords",
            joinColumns = @JoinColumn(name = "username", referencedColumnName = "username")
    )
    @Column(name = "password")
    private List<String> passwordHistory = new ArrayList<>();

    public Queue<String> getPasswordHistory() {
        return new LinkedList<>(passwordHistory);
    }

    public void setPasswordHistory(Queue<String> passwordHistoryQueue) {
        List<String> passwords = new ArrayList<>(passwordHistoryQueue);
        this.passwordHistory.clear();

        if (!passwords.isEmpty()) {
            this.passwordHistory.addAll(passwords);
        }
    }
}
