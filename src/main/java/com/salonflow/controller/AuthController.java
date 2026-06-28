package com.salonflow.controller;

import com.salonflow.model.Salon;
import com.salonflow.service.SalonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final SalonService salonService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("salon", new Salon());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("salon") Salon salon,
                           BindingResult result,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        if (salonService.emailExists(salon.getEmail())) {
            model.addAttribute("emailError", "Email already registered.");
            return "auth/register";
        }
        Salon saved = salonService.registerSalon(salon);
        redirectAttributes.addFlashAttribute("successCode", saved.getSalonCode());
        redirectAttributes.addFlashAttribute("successMessage",
                "Registration successful! Your Salon Code is: " + saved.getSalonCode());
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("loginError", "Invalid email or password.");
        if (logout != null) model.addAttribute("logoutMessage", "You have been logged out.");
        return "auth/login";
    }
}
