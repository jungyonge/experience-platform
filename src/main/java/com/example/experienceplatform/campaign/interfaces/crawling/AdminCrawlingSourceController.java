package com.example.experienceplatform.campaign.interfaces.crawling;

import com.example.experienceplatform.campaign.application.crawling.CrawlingSourceInfo;
import com.example.experienceplatform.campaign.application.crawling.CrawlingSourceService;
import com.example.experienceplatform.campaign.application.crawling.CrawlingTestResult;
import com.example.experienceplatform.campaign.interfaces.crawling.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/crawling/sources")
public class AdminCrawlingSourceController {

    private final CrawlingSourceService crawlingSourceService;

    public AdminCrawlingSourceController(CrawlingSourceService crawlingSourceService) {
        this.crawlingSourceService = crawlingSourceService;
    }

    @GetMapping
    public ResponseEntity<CrawlingSourceListResponse> getSources() {
        List<CrawlingSourceInfo> sources = crawlingSourceService.getAllSources();
        List<String> crawlerTypes = crawlingSourceService.getAvailableCrawlerTypes();
        return ResponseEntity.ok(CrawlingSourceListResponse.from(sources, crawlerTypes));
    }

    @PostMapping
    public ResponseEntity<CrawlingSourceResponse> createSource(
            @Valid @RequestBody CrawlingSourceCreateRequest request) {
        CrawlingSourceInfo info = crawlingSourceService.create(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(CrawlingSourceResponse.from(info));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CrawlingSourceResponse> updateSource(
            @PathVariable Long id,
            @Valid @RequestBody CrawlingSourceUpdateRequest request) {
        CrawlingSourceInfo info = crawlingSourceService.update(id, request.toCommand());
        return ResponseEntity.ok(CrawlingSourceResponse.from(info));
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<CrawlingSourceResponse> toggleActive(@PathVariable Long id) {
        CrawlingSourceInfo info = crawlingSourceService.toggleActive(id);
        return ResponseEntity.ok(CrawlingSourceResponse.from(info));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<CrawlingTestResponse> testCrawl(@PathVariable Long id) {
        CrawlingTestResult result = crawlingSourceService.testCrawl(id);
        return ResponseEntity.ok(CrawlingTestResponse.from(result));
    }
}
