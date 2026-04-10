package com.ercanbeyen.bankingapplication.repository;

import com.ercanbeyen.bankingapplication.dto.FilePreviewInfo;
import com.ercanbeyen.bankingapplication.entity.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<File, String> {
    @Query(value = """
            SELECT f.name AS name, f.id AS id, f.type AS type, OCTET_LENGTH(f.data) AS size
            FROM files f
            """, nativeQuery = true)
    List<FilePreviewInfo> findAllPreviewInfos();
}
