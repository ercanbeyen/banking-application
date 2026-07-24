package com.ercanbeyen.bankingapplication.entity;

import com.ercanbeyen.bankingapplication.constant.enums.AgreementSubject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "agreements")
public class Agreement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @Column(unique = true, nullable = false)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgreementSubject subject;
    @ManyToMany
    private List<File> files;
    @OneToMany(mappedBy = "agreement", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<CustomerAgreement> customers;
    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime createdAt;

    @Override
    public String toString() {
        List<String> customerNationalIds = customers.stream()
                .map(customerAgreement -> customerAgreement.getCustomer().getNationalId())
                .toList();

        List<String> fileNames = files.stream()
                .map(File::getName)
                .toList();

        return "Agreement{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", subject=" + subject +
                ", files=" + fileNames +
                ", customers=" + customerNationalIds +
                ", createdAt=" + createdAt +
                '}';
    }
}
