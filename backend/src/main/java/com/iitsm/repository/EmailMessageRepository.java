package com.iitsm.repository;

import com.iitsm.entity.EmailMessage;
import org.springframework.data.jpa.repository.*;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {}
