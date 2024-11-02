package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.Survey;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SurveyRepository extends CassandraRepository<Survey, UUID> {
    @AllowFiltering
    Optional<Survey> findByYearAndCustomerNationalId(int year, String customerNationalId);
}
