package com.example.experienceplatform.campaign.domain;

import java.util.List;
import java.util.Optional;

public interface CrawlingSourceRepository {

    CrawlingSource save(CrawlingSource source);

    Optional<CrawlingSource> findById(Long id);

    Optional<CrawlingSource> findByCode(String code);

    List<CrawlingSource> findAllActiveOrderByDisplayOrder();

    List<CrawlingSource> findAllOrderByDisplayOrder();

    boolean existsByCode(String code);
}
