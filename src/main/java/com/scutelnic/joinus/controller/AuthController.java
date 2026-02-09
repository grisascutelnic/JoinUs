package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.ForgotPasswordRequest;
import com.scutelnic.joinus.dto.RegisterRequest;
import com.scutelnic.joinus.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
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

    @PostMapping("/register")
    public String registerSubmit(
            @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Completeaza toate campurile corect.");
            return "redirect:/?register&registerError";
        }

        try {
            userService.register(registerRequest);
            return "redirect:/?login&registered";
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
}
