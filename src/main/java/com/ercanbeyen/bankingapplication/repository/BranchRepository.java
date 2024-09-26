package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.entity.Branch;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends BaseRepository<Branch> {
    Optional<Branch> findByName(String name);
}
