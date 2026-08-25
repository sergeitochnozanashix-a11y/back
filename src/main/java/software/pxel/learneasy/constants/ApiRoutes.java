package software.pxel.learneasy.constants;

public final class ApiRoutes {

    private ApiRoutes() {
        throw new IllegalStateException("You shouldn't create constant class");
    }

    public static final String MODULES_URI = "/api/v1/modules";
    public static final String LESSON_URI = "/api/v1/lesson";
    public static final String TESTS_API_URI = "/api/v1/tests";
    public static final String FILE_STORAGE_URI = "/api/v1/file-storage";
    public static final String AUTH_URI = "/api/v1/auth";
    public static final String COURSES_URI = "/api/v1/courses";
    public static final String USER_PROGRESS_URI = "/api/v1/progress";
    public static final String USER_URI = "/api/v1/users";
    public static final String ARTICLES_URI = "/api/v1/articles";
    public static final String AI_COMPANION_URI = "/api/v1/ai-companion/chats";
    public static final String ANALYTICS_URI = "/api/v1/admin/analytics";

    public static final String FILE_STORAGE_DOWNLOAD_ROUTE = "/download";
}
