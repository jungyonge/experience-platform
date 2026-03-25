package com.example.experienceplatform.campaign.domain;

import java.util.List;
import java.util.Optional;

public interface RegionRepository {

    List<Region> findAllOrderBySidoAndSigungu();

    Optional<Region> findById(Long id);

    List<Region> findBySido(String sido);
}
