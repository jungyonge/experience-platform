package com.example.experienceplatform.campaign.interfaces.crawling;

import com.example.experienceplatform.campaign.application.crawling.CrawlingOrchestrator;
import com.example.experienceplatform.campaign.infrastructure.crawling.CrawlingResult;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLog;
import com.example.experienceplatform.campaign.infrastructure.crawling.log.CrawlingLogRepository;
import com.example.experienceplatform.campaign.interfaces.crawling.dto.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/crawling")
public class CrawlingController {

    private final CrawlingOrchestrator orchestrator;
    private final CrawlingLogRepository crawlingLogRepository;

    public CrawlingController(CrawlingOrchestrator orchestrator,
                              CrawlingLogRepository crawlingLogRepository) {
        this.orchestrator = orchestrator;
        this.crawlingLogRepository = crawlingLogRepository;
    }

    @PostMapping("/execute")
    public ResponseEntity<CrawlingExecuteResponse> executeAll() {
        List<CrawlingResult> results = orchestrator.executeAll();
        return ResponseEntity.ok(CrawlingExecuteResponse.from(results));
    }

    @PostMapping("/execute/{sourceCode}")
    public ResponseEntity<CrawlingResultResponse> executeBySource(@PathVariable String sourceCode) {
        CrawlingResult result = orchestrator.executeBySourceCode(sourceCode);
        return ResponseEntity.ok(CrawlingResultResponse.from(result));
    }

    @GetMapping("/logs")
    public ResponseEntity<CrawlingLogListResponse> getLogs(
            @RequestParam(required = false) String sourceCode,
            @RequestParam(defaultValue = "20") int limit) {
        if (limit > 100) limit = 100;

        List<CrawlingLog> logs;
        if (sourceCode != null && !sourceCode.isBlank()) {
            logs = crawlingLogRepository.findRecent(sourceCode, PageRequest.of(0, limit));
        } else {
            logs = crawlingLogRepository.findRecent(PageRequest.of(0, limit));
        }

        return ResponseEntity.ok(CrawlingLogListResponse.from(logs));
    }
}
