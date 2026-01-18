package com.prodapt.network_ticketing.repository;

import com.prodapt.network_ticketing.entity.IssueCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueCategoryRepository extends JpaRepository<IssueCategory, Long> {
}
