package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.Gender;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Setter
@Entity
@Table(name = "customers")
public non-sealed class Customer extends BaseEntity {
    @Getter
    private String name;
    @Getter
    private String surname;
    @Getter
    @Column(name = "national_id", length = 11, unique = true)
    private String nationalId;
    @Getter
    @Column(name = "phone_number", length = 13, unique = true)
    private String phoneNumber;
    @Getter
    private String email;
    @Getter
    @Enumerated(EnumType.STRING)
    private Gender gender;
    @Getter
    @Column(name = "birth_date")
    private LocalDate birthDate;
    @Getter
    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Address address;
    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "profile_photo")
    private File profilePhoto;
    @Getter
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts = new ArrayList<>();

    public Optional<File> getProfilePhoto() {
        return Optional.ofNullable(profilePhoto);
    }
}
