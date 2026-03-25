package com.example.experienceplatform.campaign.interfaces;

import com.example.experienceplatform.campaign.application.CampaignDetail;
import com.example.experienceplatform.campaign.application.CampaignListInfo;
import com.example.experienceplatform.campaign.application.CampaignService;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.domain.CrawlingSourceRepository;
import com.example.experienceplatform.campaign.domain.Region;
import com.example.experienceplatform.campaign.domain.RegionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final CrawlingSourceRepository crawlingSourceRepository;
    private final RegionRepository regionRepository;

    public CampaignController(CampaignService campaignService,
                              CrawlingSourceRepository crawlingSourceRepository,
                              RegionRepository regionRepository) {
        this.campaignService = campaignService;
        this.crawlingSourceRepository = crawlingSourceRepository;
        this.regionRepository = regionRepository;
    }

    @GetMapping
    public ResponseEntity<CampaignListResponse> searchCampaigns(@ModelAttribute CampaignSearchRequest request) {
        request.validate();
        CampaignListInfo listInfo = campaignService.searchCampaigns(request.toCommand());
        return ResponseEntity.ok(CampaignListResponse.from(listInfo));
    }

    @GetMapping("/filters")
    public ResponseEntity<FilterOptionResponse> getFilters() {
        List<CrawlingSource> activeSources = crawlingSourceRepository.findAllActiveOrderByDisplayOrder();
        List<Region> regions = regionRepository.findAllOrderBySidoAndSigungu();
        return ResponseEntity.ok(FilterOptionResponse.create(activeSources, regions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignDetailResponse> getDetail(@PathVariable Long id) {
        CampaignDetail detail = campaignService.getDetail(id);
        return ResponseEntity.ok(CampaignDetailResponse.from(detail));
    }
}
