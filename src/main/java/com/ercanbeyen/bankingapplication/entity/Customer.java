package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.*;

@Setter
@Entity
@Table(name = "customers", indexes = {@Index(name = "nationalId_index", columnList = "nationalId")})
public non-sealed class Customer extends BaseEntity {
    @Getter
    @Column(nullable = false, length = 100)
    private String name;
    @Getter
    @Column(nullable = false, length = 100)
    private String surname;
    @Getter
    @Column(name = "national_id", nullable = false, length = 11, unique = true)
    private String nationalId;
    @Getter
    @Column(name = "phone_number", length = 13, unique = true)
    private String phoneNumber;
    @Getter
    @Column(nullable = false, unique = true)
    private String email;
    @Getter
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Getter
    @Column(name = "birth_date")
    private LocalDate birthDate;
    @Getter
    @ManyToMany(mappedBy = "customers")
    private Set<Address> addresses = new HashSet<>();
    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "profile_photo")
    private File profilePhoto;
    @Getter
    @SQLRestriction("closed_at IS NULL")
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts = new ArrayList<>();
    @Getter
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    public Optional<Account> getAccount(Integer id) {
        return accounts.stream()
                .filter(account -> Objects.equals(account.getId(), id))
                .findFirst();
    }

    public Optional<File> getProfilePhoto() {
        return Optional.ofNullable(profilePhoto);
    }
}
