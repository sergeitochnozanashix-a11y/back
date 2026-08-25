package software.pxel.learneasy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.pxel.learneasy.api.dto.admin.response.AnalyticResponse;
import software.pxel.learneasy.controller.api.AdminAnalyticsApi;
import software.pxel.learneasy.model.enums.Period;
import software.pxel.learneasy.service.AnalyticsService;

import static software.pxel.learneasy.constants.ApiRoutes.ANALYTICS_URI;

@RestController
@RequiredArgsConstructor
@RequestMapping(ANALYTICS_URI)
public class AdminAnalyticsController implements AdminAnalyticsApi {

    private final AnalyticsService analyticsService;

    @Override
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public ResponseEntity<AnalyticResponse> getAnalyticsDashboard(@RequestParam(value = "period", defaultValue = "month") String period) {
        Period periodValueString = Period.fromString(period);
        return ResponseEntity.ok(analyticsService.getAnalyticsDashboard(periodValueString));
    }
}
