package com.example.experienceplatform.campaign.infrastructure;

import com.example.experienceplatform.campaign.domain.Region;
import com.example.experienceplatform.campaign.domain.RegionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RegionJpaRepository implements RegionRepository {

    private final RegionSpringDataJpaRepository springDataRepository;

    public RegionJpaRepository(RegionSpringDataJpaRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public List<Region> findAllOrderBySidoAndSigungu() {
        return springDataRepository.findAllByOrderBySidoAscSigunguAsc();
    }

    @Override
    public Optional<Region> findById(Long id) {
        return springDataRepository.findById(id);
    }

    @Override
    public List<Region> findBySido(String sido) {
        return springDataRepository.findBySidoOrderBySigunguAsc(sido);
    }
}
