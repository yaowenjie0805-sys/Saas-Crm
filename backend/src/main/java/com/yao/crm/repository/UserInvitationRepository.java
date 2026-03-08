package com.yao.crm.repository;

import com.yao.crm.entity.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, String> {
    Optional<UserInvitation> findByTokenAndUsedFalse(String token);
    List<UserInvitation> findByTenantIdAndUsedFalseOrderByCreatedAtDesc(String tenantId);
}
