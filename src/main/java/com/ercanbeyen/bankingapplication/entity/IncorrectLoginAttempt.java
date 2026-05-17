package com.ercanbeyen.bankingapplication.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "incorrect_login_attempts")
public class IncorrectLoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private LocalDateTime dateTime;
    @ManyToOne
    @JoinColumn(name = "user_credentials_id", referencedColumnName = "username")
    private UserCredentials userCredentials;
}
