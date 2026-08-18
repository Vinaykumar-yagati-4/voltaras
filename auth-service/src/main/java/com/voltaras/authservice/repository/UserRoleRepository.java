package com.voltaras.authservice.repository;

import com.voltaras.authservice.entity.UserRole;
import com.voltaras.authservice.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UserRole.UserRoleId> {

    boolean existsByRole_Name(RoleType roleName);
}
