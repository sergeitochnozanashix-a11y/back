package software.pxel.learneasy.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;
import static software.pxel.learneasy.constants.ApiRoutes.AI_COMPANION_URI;
import static software.pxel.learneasy.constants.ApiRoutes.ANALYTICS_URI;
import static software.pxel.learneasy.constants.ApiRoutes.ARTICLES_URI;
import static software.pxel.learneasy.constants.ApiRoutes.AUTH_URI;
import static software.pxel.learneasy.constants.ApiRoutes.COURSES_URI;
import static software.pxel.learneasy.constants.ApiRoutes.FILE_STORAGE_URI;
import static software.pxel.learneasy.constants.ApiRoutes.LESSON_URI;
import static software.pxel.learneasy.constants.ApiRoutes.MODULES_URI;
import static software.pxel.learneasy.constants.ApiRoutes.TESTS_API_URI;
import static software.pxel.learneasy.constants.ApiRoutes.USER_PROGRESS_URI;
import static software.pxel.learneasy.constants.ApiRoutes.USER_URI;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig {

    private final AuthenticationEntryPoint userAuthenticationEntryPoint;
    private final UserAuthProvider userAuthProvider;

    private static final String[] WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/error/**"
    };

    private static final String ADMIN_AUTHORITY = "ADMIN";

    // Группировка маршрутов для конфигурации безопасности
    private static final String[] PUBLIC_ACCESS_API_ROUTES = {
            AUTH_URI + "/**"
    };

    private static final String[] AUTHENTICATED_READ_ONLY_ROUTES = {
            MODULES_URI + "/**",
            LESSON_URI + "/**",
            TESTS_API_URI + "/**",
            USER_URI + "/**",
            COURSES_URI + "/**",
            USER_PROGRESS_URI + "/**",
            AI_COMPANION_URI + "/**"
    };

    private static final String[] ADMIN_MANAGED_ROUTES = {
            MODULES_URI + "/**",
            LESSON_URI + "/**",
            TESTS_API_URI + "/**",
            COURSES_URI + "/**",
            USER_PROGRESS_URI + "/**",
            ARTICLES_URI + "/**",
            ANALYTICS_URI + "/**"
    };

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(userAuthProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .exceptionHandling(customizer -> customizer.authenticationEntryPoint(userAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter(), BasicAuthenticationFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(WHITELIST).permitAll()
                        .requestMatchers(PUBLIC_ACCESS_API_ROUTES).permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.GET, FILE_STORAGE_URI + "/**").permitAll() // GET для файлового хранилища разрешен всем

                        .requestMatchers(HttpMethod.GET, ARTICLES_URI + "/**").permitAll()

                        // Правила для общих аутентифицированных и админских роутов
                        .requestMatchers(HttpMethod.GET, AUTHENTICATED_READ_ONLY_ROUTES).authenticated()
                        .requestMatchers(HttpMethod.POST, ADMIN_MANAGED_ROUTES).hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.PUT, ADMIN_MANAGED_ROUTES).hasAuthority(ADMIN_AUTHORITY)
                        .requestMatchers(HttpMethod.DELETE, ADMIN_MANAGED_ROUTES).hasAuthority(ADMIN_AUTHORITY)

                        .anyRequest().authenticated() // Все остальные запросы требуют аутентификации
                );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "https://edu.pxel.software",
                "https://*.pxel.software",
                "https://learnizy-frontend.vercel.app",
                // Preview-деплои Vercel получают адреса вида
                // learnizy-frontend-<hash>-<account>.vercel.app. Шаблон намеренно
                // привязан к имени проекта: "https://*.vercel.app" открыл бы API
                // любому приложению на Vercel.
                "https://learnizy-frontend-*.vercel.app"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }
}
