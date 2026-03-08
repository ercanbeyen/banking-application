package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.model.Survey;
import com.ercanbeyen.bankingapplication.model.SurveyCompositeKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SurveyRepository extends CassandraRepository<Survey, SurveyCompositeKey> {

}
