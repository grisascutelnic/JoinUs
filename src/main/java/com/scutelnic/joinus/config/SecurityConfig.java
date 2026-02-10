package com.scutelnic.joinus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import com.scutelnic.joinus.service.CustomUserDetailsService;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
    private final String rememberMeKey;

    public SecurityConfig(DataSource dataSource,
                          CustomUserDetailsService customUserDetailsService,
                          @Value("${app.security.remember-me.key}") String rememberMeKey) {
        this.dataSource = dataSource;
        this.customUserDetailsService = customUserDetailsService;
        this.rememberMeKey = rememberMeKey;
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
                                "/forgot-password",
                                "/css/**",
                                "/fonts/**",
                                "/js/**",
                                "/images/**",
                                "/uploads/**",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/activities").authenticated()
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
                .rememberMe(remember -> remember
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds(60 * 60 * 24 * 30)
                        .key(rememberMeKey)
                        .tokenRepository(persistentTokenRepository())
                        .userDetailsService(customUserDetailsService)
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
}
