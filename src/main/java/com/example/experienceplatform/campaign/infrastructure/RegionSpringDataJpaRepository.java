package com.example.experienceplatform.campaign.infrastructure;

import com.example.experienceplatform.campaign.domain.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface RegionSpringDataJpaRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderBySidoAscSigunguAsc();

    List<Region> findBySidoOrderBySigunguAsc(String sido);
}
