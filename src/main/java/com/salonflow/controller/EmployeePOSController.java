package com.salonflow.controller;

import com.salonflow.model.*;
import com.salonflow.service.*;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/pos")
@RequiredArgsConstructor
public class EmployeePOSController {

    private final SalonService salonService;
    private final EmployeeService employeeService;
    private final ServiceManagementService serviceManagementService;
    private final TransactionService transactionService;

    /**
     * Entry point: /pos?salon=BEAUTY001
     * Reads salon code from URL, validates it, stores in session, redirects to PIN page.
     */
    @GetMapping
    public String posEntry(@RequestParam(required = false) String business,
                           HttpSession session,
                           Model model) {
        // If already logged in as employee, go straight to POS screen
        if (session.getAttribute("posEmployee") != null) {
            return "redirect:/pos/screen";
        }

        if (business == null || business.isBlank()) {
            model.addAttribute("error", "No salon code provided in URL.");
            return "employee/salon-error";
        }

        Optional<Salon> found = salonService.findBySalonCode(business.toUpperCase().trim());
        if (found.isEmpty()) {
            model.addAttribute("error", "Salon code '" + business + "' not found.");
            return "employee/salon-error";
        }

        // Valid salon — store in session and go to PIN page
        session.setAttribute("posSalon", found.get());
        return "redirect:/pos/pin";
    }

    // PIN login page
    @GetMapping("/pin")
    public String pinPage(HttpSession session, Model model) {
        Salon salon = (Salon) session.getAttribute("posSalon");
        if (salon == null) return "redirect:/pos/salon";
        model.addAttribute("salonName", salon.getFullName());
        model.addAttribute("salonCode", salon.getSalonCode());
        return "employee/pin-login";
    }

    @PostMapping("/pin")
    public String submitPin(@RequestParam String pin,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Salon salon = (Salon) session.getAttribute("posSalon");
        if (salon == null) return "redirect:/pos/salon";

        Optional<Employee> employee = employeeService.authenticateEmployee(salon, pin);
        if (employee.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Incorrect PIN. Please try again.");
            return "redirect:/pos/pin";
        }

        session.setAttribute("posEmployee", employee.get());
        return "redirect:/pos/screen";
    }

    // POS Screen
    @GetMapping("/screen")
    public String posScreen(HttpSession session, Model model) {
        Employee employee = (Employee) session.getAttribute("posEmployee");
        Salon salon = (Salon) session.getAttribute("posSalon");
        if (employee == null || salon == null) return "redirect:/pos/salon";

        List<ServiceItem> services = serviceManagementService.getActiveServices(salon);
        List<ServiceCategory> categories = serviceManagementService.getCategories(salon);

        model.addAttribute("employee", employee);
        model.addAttribute("salon", salon);
        model.addAttribute("services", services);
        model.addAttribute("categories", categories);
        return "employee/pos-screen";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam List<Long> serviceIds,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        Employee employee = (Employee) session.getAttribute("posEmployee");
        Salon salon = (Salon) session.getAttribute("posSalon");
        if (employee == null || salon == null) return "redirect:/pos/salon";

        if (serviceIds == null || serviceIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select at least one service.");
            return "redirect:/pos/screen";
        }

        Transaction tx = transactionService.createTransaction(employee.getId(), salon, serviceIds);
        redirectAttributes.addFlashAttribute("success", "Transaction completed! Total: $" + tx.getTotalAmount());
        return "redirect:/pos/screen";
    }

    // Switch employee — keep salon in session, clear employee
    @GetMapping("/switch")
    public String switchEmployee(HttpSession session) {
        session.removeAttribute("posEmployee");
        return "redirect:/pos/pin";
    }

    // Exit — clear everything, go back to salon URL
    @GetMapping("/exit")
    public String exitPos(HttpSession session) {
        Salon salon = (Salon) session.getAttribute("posSalon");
        String code = salon != null ? salon.getSalonCode() : "";
        session.removeAttribute("posEmployee");
        session.removeAttribute("posSalon");
        return "redirect:/pos?business=" + code;
    }

    // Keep old /pos/salon route working (for backward compat)
    @GetMapping("/salon")
    public String legacySalonPage() {
        return "employee/salon-select";
    }

    @PostMapping("/salon")
    public String legacySalonSubmit(@RequestParam String salonCode,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes,Model model) {
        if (salonCode == null || salonCode.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Please enter a salon code.");
            return "redirect:/pos/salon";
        } else {
            Optional<Salon> found = salonService.findBySalonCode(salonCode.toUpperCase().trim());
            if (found.isEmpty()) {
                model.addAttribute("error", "Salon code '" + salonCode + "' not found.");
                return "employee/salon-error";
            }

            // Valid salon — store in session and go to PIN page
            session.setAttribute("posSalon", found.get());
            return "redirect:/pos/pin";
            //return "redirect:/pos?salon=" + salonCode.trim().toUpperCase();
        }
    }
}