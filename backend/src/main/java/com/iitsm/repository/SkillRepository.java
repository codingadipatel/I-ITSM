package com.iitsm.repository;

import com.iitsm.entity.Skill;
import org.springframework.data.jpa.repository.*;

public interface SkillRepository extends JpaRepository<Skill, Long> {
  java.util.Optional<Skill> findByNameIgnoreCase(String name);
}
