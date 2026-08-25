package software.pxel.learneasy.service;

import software.pxel.learneasy.api.dto.admin.response.AnalyticResponse;
import software.pxel.learneasy.model.enums.Period;

public interface AnalyticsService {

    AnalyticResponse getAnalyticsDashboard(Period period);
}
