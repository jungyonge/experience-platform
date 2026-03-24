package com.example.experienceplatform.campaign.interfaces;

import com.example.experienceplatform.campaign.application.CampaignDetail;
import com.example.experienceplatform.campaign.application.CampaignListInfo;
import com.example.experienceplatform.campaign.application.CampaignService;
import com.example.experienceplatform.campaign.domain.CampaignRepository;
import com.example.experienceplatform.campaign.domain.CrawlingSource;
import com.example.experienceplatform.campaign.domain.CrawlingSourceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignRepository campaignRepository;
    private final CrawlingSourceRepository crawlingSourceRepository;

    public CampaignController(CampaignService campaignService,
                              CampaignRepository campaignRepository,
                              CrawlingSourceRepository crawlingSourceRepository) {
        this.campaignService = campaignService;
        this.campaignRepository = campaignRepository;
        this.crawlingSourceRepository = crawlingSourceRepository;
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
        List<String> regions = campaignRepository.findDistinctRegions();
        return ResponseEntity.ok(FilterOptionResponse.create(activeSources, regions));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignDetailResponse> getDetail(@PathVariable Long id) {
        CampaignDetail detail = campaignService.getDetail(id);
        return ResponseEntity.ok(CampaignDetailResponse.from(detail));
    }
}
