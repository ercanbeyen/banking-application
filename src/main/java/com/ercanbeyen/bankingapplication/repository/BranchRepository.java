package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.model.Branch;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends BaseRepository<Branch> {
    Optional<Branch> findByName(String name);
}
