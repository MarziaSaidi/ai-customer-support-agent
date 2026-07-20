package com.supportai.controller;

import com.supportai.dto.AnalyticsResponse;
import com.supportai.dto.QuestionStatResponse;
import com.supportai.service.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public AnalyticsResponse getAnalytics(
            @RequestParam Long companyId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return analyticsService.getCompanyAnalytics(companyId, principal.getUsername());
    }

    @GetMapping("/questions")
    public List<QuestionStatResponse> getTopQuestions(
            @RequestParam Long companyId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return analyticsService.getTopQuestions(companyId, principal.getUsername());
    }
}
