package com.salonflow.controller;

import com.salonflow.model.Salon;
import com.salonflow.repository.SalonRepository;
import com.salonflow.repository.TransactionRepository;
import com.salonflow.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SalonRepository salonRepository;
    private final EmployeeService employeeService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Salon> salons = salonRepository.findAll();
        model.addAttribute("salons", salons);
        model.addAttribute("totalSalons", salons.size());
        model.addAttribute("activeSalons", salons.stream().filter(Salon::isActive).count());
        return "admin/dashboard";
    }

    @GetMapping("/salon/{id}")
    public String salonDetail(@PathVariable Long id, Model model) {
        Salon salon = salonRepository.findById(id).orElseThrow();
        model.addAttribute("salon", salon);
        model.addAttribute("employees", employeeService.getEmployeesBySalon(salon));
        return "admin/salon-detail";
    }

    @PostMapping("/salon/{id}/toggle")
    public String toggleSalon(@PathVariable Long id) {
        salonRepository.findById(id).ifPresent(s -> {
            s.setActive(!s.isActive());
            salonRepository.save(s);
        });
        return "redirect:/admin/dashboard";
    }
}