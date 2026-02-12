package com.scutelnic.joinus.config;

import com.scutelnic.joinus.controller.AuthController;
import com.scutelnic.joinus.service.OAuthAccountService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.CookieTheftException;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationException;
import com.scutelnic.joinus.service.CustomUserDetailsService;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import javax.sql.DataSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String REMEMBER_ME_KEY = "joinus-persistent-login-v1";
    private static final int REMEMBER_ME_TTL_SECONDS = 60 * 60 * 24 * 10;

    private static final String[] AUTH_QUERY_KEYS = {
            "login",
            "register",
            "logout",
            "error",
            "registered",
            "registerError"
    };

    private final DataSource dataSource;
    private final CustomUserDetailsService customUserDetailsService;
    private final OAuthAccountService oauthAccountService;

    public SecurityConfig(DataSource dataSource,
                          CustomUserDetailsService customUserDetailsService,
                          OAuthAccountService oauthAccountService) {
        this.dataSource = dataSource;
        this.customUserDetailsService = customUserDetailsService;
        this.oauthAccountService = oauthAccountService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/activities", "/ws/**")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index",
                                "/activities",
                                "/activities/**",
                                "/login",
                                "/register",
                                "/auth/google",
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/forgot-password",
                                "/css/**",
                                "/fonts/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/activities").authenticated()
                        .requestMatchers(HttpMethod.POST, "/users/*/reviews").authenticated()
                        .requestMatchers("/activities/new").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            String target = sanitizeRedirectUrl(request.getHeader("Referer"));
                            response.sendRedirect(target != null ? target : "/");
                        })
                        .failureUrl("/?login&error")
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(this::handleOAuth2Success)
                        .failureUrl("/?login&error")
                )
                .rememberMe(remember -> remember
                        .rememberMeServices(rememberMeServices())
                        .alwaysRemember(true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            String target = sanitizeRedirectUrl(request.getHeader("Referer"));
                            response.sendRedirect(target != null ? target : "/");
                        })
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PersistentTokenRepository persistentTokenRepository() {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }

    @Bean
    public RememberMeServices rememberMeServices() {
        LenientPersistentRememberMeServices services =
                new LenientPersistentRememberMeServices(REMEMBER_ME_KEY, customUserDetailsService, persistentTokenRepository());
        services.setParameter("remember-me");
        services.setAlwaysRemember(true);
        services.setTokenValiditySeconds(REMEMBER_ME_TTL_SECONDS);
        return services;
    }

    private void handleOAuth2Success(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendRedirect("/");
            return;
        }

        OAuth2User oauthUser = oauthToken.getPrincipal();
        String rawEmail = oauthUser.getAttribute("email");
        if (rawEmail == null || rawEmail.isBlank()) {
            response.sendRedirect("/?login&error");
            return;
        }

        String normalizedEmail = rawEmail.toLowerCase().trim();
        String fullName = oauthUser.getAttribute("name");
        String pictureUrl = oauthUser.getAttribute("picture");

        OAuthAccountService.GoogleUpsertResult upsertResult =
                oauthAccountService.upsertGoogleUser(normalizedEmail, fullName, pictureUrl);

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(normalizedEmail);
        UsernamePasswordAuthenticationToken localAuthentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
        localAuthentication.setDetails(authentication.getDetails());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(localAuthentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        rememberMeServices().loginSuccess(request, response, localAuthentication);

        String target = null;
        if (request.getSession(false) != null) {
            Object redirectCandidate = request.getSession(false).getAttribute(AuthController.OAUTH2_REDIRECT_TARGET_SESSION_KEY);
            if (redirectCandidate instanceof String redirectString) {
                target = sanitizeRedirectUrl(redirectString);
            }
            request.getSession(false).removeAttribute(AuthController.OAUTH2_REDIRECT_TARGET_SESSION_KEY);
        }

        if (upsertResult.created()) {
            response.sendRedirect("/profile?completeProfile&birthDateRequired");
            return;
        }

        response.sendRedirect(target != null ? target : "/");
    }

    @Bean
    public ApplicationRunner ensurePersistentLoginsTable(JdbcTemplate jdbcTemplate) {
        return args -> jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS persistent_logins (
                    username VARCHAR(64) NOT NULL,
                    series VARCHAR(64) PRIMARY KEY,
                    token VARCHAR(64) NOT NULL,
                    last_used TIMESTAMP NOT NULL
                )
                """);
    }

    private static String sanitizeRedirectUrl(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(referer);
            String query = uri.getQuery();
            if (query == null || query.isBlank()) {
                return uri.getPath() + (uri.getFragment() != null ? "#" + uri.getFragment() : "");
            }

            Map<String, String> params = Arrays.stream(query.split("&"))
                    .filter(part -> !part.isBlank())
                    .map(part -> part.split("=", 2))
                    .collect(Collectors.toMap(
                            pair -> pair[0],
                            pair -> pair.length > 1 ? pair[1] : "",
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));

            for (String key : AUTH_QUERY_KEYS) {
                params.remove(key);
            }

            String cleanedQuery = params.entrySet().stream()
                    .map(entry -> entry.getValue().isEmpty() ? entry.getKey() : entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("&"));

            StringBuilder cleaned = new StringBuilder();
            cleaned.append(uri.getPath() != null ? uri.getPath() : "/");
            if (!cleanedQuery.isEmpty()) {
                cleaned.append("?").append(cleanedQuery);
            }
            if (uri.getFragment() != null) {
                cleaned.append("#").append(uri.getFragment());
            }
            return cleaned.toString();
        } catch (URISyntaxException ex) {
            return "/";
        }
    }

    private static final class LenientPersistentRememberMeServices extends PersistentTokenBasedRememberMeServices {

        private LenientPersistentRememberMeServices(String key,
                                                    CustomUserDetailsService userDetailsService,
                                                    PersistentTokenRepository tokenRepository) {
            super(key, userDetailsService, tokenRepository);
        }

        @Override
        protected UserDetails processAutoLoginCookie(String[] cookieTokens,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
            try {
                return super.processAutoLoginCookie(cookieTokens, request, response);
            } catch (CookieTheftException ex) {
                throw new RememberMeAuthenticationException("Invalid remember-me token", ex);
            }
        }
    }

}
