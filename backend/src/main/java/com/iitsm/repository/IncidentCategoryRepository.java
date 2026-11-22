package com.iitsm.repository;

import com.iitsm.entity.IncidentCategory;
import org.springframework.data.jpa.repository.*;

public interface IncidentCategoryRepository extends JpaRepository<IncidentCategory, Long> {}
