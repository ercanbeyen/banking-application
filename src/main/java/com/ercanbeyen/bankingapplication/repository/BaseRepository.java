package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.model.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface BaseRepository<T extends BaseEntity> extends JpaRepository<T, Integer> {

}
