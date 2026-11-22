package com.iitsm.repository;

import com.iitsm.entity.ServiceItem;
import org.springframework.data.jpa.repository.*;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {}
