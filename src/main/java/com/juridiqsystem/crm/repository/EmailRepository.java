package com.juridiqsystem.crm.repository;

import com.juridiqsystem.crm.model.Email;
import com.juridiqsystem.crm.model.Funil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {
}
