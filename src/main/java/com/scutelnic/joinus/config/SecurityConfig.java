package com.scutelnic.joinus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/activities")
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index",
                                "/activities",
                                "/activities/new",
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
