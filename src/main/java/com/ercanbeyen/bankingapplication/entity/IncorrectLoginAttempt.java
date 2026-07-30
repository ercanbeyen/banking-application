package com.ercanbeyen.bankingapplication.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "incorrect_login_attempts")
public class IncorrectLoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private Instant attemptedAt;
    @ManyToOne
    @JoinColumn(name = "user_credentials_id", referencedColumnName = "username")
    private UserCredentials userCredentials;
}
