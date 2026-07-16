package com.supportai.repository;

import com.supportai.entity.CompanyUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyUserRepository extends JpaRepository<CompanyUser, Long> {
    List<CompanyUser> findByCompanyId(Long companyId);
    Optional<CompanyUser> findByUserIdAndCompanyId(Long userId, Long companyId);
    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);
    long countByCompanyIdAndRole(Long companyId, com.supportai.enums.RoleType role);
}
