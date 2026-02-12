package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.ForgotPasswordRequest;
import com.scutelnic.joinus.dto.RegisterRequest;
import com.scutelnic.joinus.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class AuthController {

    public static final String OAUTH2_REDIRECT_TARGET_SESSION_KEY = "oauth2_redirect_target";

    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    private static final String[] AUTH_QUERY_KEYS = {
            "login",
            "register",
            "logout",
            "error",
            "registered",
            "registerError"
    };

    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String registered,
            @RequestParam(required = false) String logout
    ) {
        StringBuilder redirect = new StringBuilder("redirect:/?login");
        if (error != null) {
            redirect.append("&error");
        }
        if (registered != null) {
            redirect.append("&registered");
        }
        if (logout != null) {
            redirect.append("&logout");
        }
        return redirect.toString();
    }

    @GetMapping("/register")
    public String register(@RequestParam(required = false) String error) {
        StringBuilder redirect = new StringBuilder("redirect:/?register");
        if (error != null) {
            redirect.append("&registerError");
        }
        return redirect.toString();
    }

    @GetMapping("/auth/google")
    public String googleAuthStart(HttpServletRequest request) {
        String target = sanitizeRedirectUrl(request.getHeader("Referer"));
        if (target != null
                && !target.startsWith("/login")
                && !target.startsWith("/register")
                && !target.startsWith("/oauth2")
                && !target.startsWith("/auth/google")) {
            request.getSession(true).setAttribute(OAUTH2_REDIRECT_TARGET_SESSION_KEY, target);
        }
        return "redirect:/oauth2/authorization/google";
    }

    @PostMapping("/register")
    public String registerSubmit(
            @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Completeaza toate campurile corect.");
            return "redirect:/?register&registerError";
        }

        try {
            userService.register(registerRequest);
            String normalizedEmail = registerRequest.getEmail().toLowerCase().trim();
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, registerRequest.getPassword())
            );
            SecurityContext context = SecurityContextHolder.getContext();
            context.setAuthentication(authentication);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );

            return "redirect:/profile?completeProfile&birthDateRequired";
        } catch (IllegalArgumentException ex) {
            return "redirect:/?register&registerError";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPassword(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(
            @Valid @ModelAttribute("forgotPasswordRequest") ForgotPasswordRequest request,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Introdu un email valid.");
            return "forgot-password";
        }

        return "redirect:/forgot-password?sent";
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
