package software.pxel.learneasy.service;

import software.pxel.learneasy.api.dto.mainpage.MainPageInfoResponse;

public interface MainPageService {

    /**
     * Gathers all necessary information for the main page view for a given user.
     *
     * @param userId the ID of the user
     * @return a composite DTO containing course progress and module details
     */
    MainPageInfoResponse getMainPageInfo(Long userId);
}
