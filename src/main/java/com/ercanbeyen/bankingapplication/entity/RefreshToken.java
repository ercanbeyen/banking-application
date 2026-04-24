package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.TokenStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "user_credentials_id", referencedColumnName = "id")
    private UserCredentials userCredentials;
    @Column(nullable = false, unique = true)
    private String token;
    @Enumerated(EnumType.STRING)
    private TokenStatus status;
    @Column(nullable = false)
    private Instant expiryDate;
}
