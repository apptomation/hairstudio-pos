package com.salonflow.controller;

import com.salonflow.model.*;
import com.salonflow.repository.SalonRepository;
import com.salonflow.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
@RequestMapping("/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final SalonRepository salonRepository;
    private final EmployeeService employeeService;
    private final ServiceManagementService serviceManagementService;
    private final TransactionService transactionService;

    private Salon getCurrentSalon(Authentication auth) {
        return salonRepository.findByEmail(auth.getName()).orElseThrow();
    }

    // Dashboard
    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        Salon salon = getCurrentSalon(auth);
        LocalDate today = LocalDate.now();

        model.addAttribute("salon", salon);
        model.addAttribute("todayRevenue", transactionService.getDailyRevenue(salon, today));
        model.addAttribute("todayCustomers", transactionService.getDailyCustomerCount(salon, today));
        model.addAttribute("recentTransactions", transactionService.getTransactionsByDate(salon, today));
        model.addAttribute("employeeCount", employeeService.getActiveEmployees(salon).size());
        model.addAttribute("serviceCount", serviceManagementService.getActiveServices(salon).size());
        model.addAttribute("topServices", transactionService.getTopServices(salon, today));
        return "owner/dashboard";
    }

    // ---- Employees ----
    @GetMapping("/employees")
    public String employees(Authentication auth, Model model) {
        Salon salon = getCurrentSalon(auth);
        model.addAttribute("employees", employeeService.getEmployeesBySalon(salon));
        model.addAttribute("newEmployee", new Employee());
        model.addAttribute("salon", salon);
        return "owner/employees";
    }

    @PostMapping("/employees/add")
    public String addEmployee(@ModelAttribute Employee employee,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        Salon salon = getCurrentSalon(auth);
        //to check if the pin is already used by another employee in the same salon
        Optional<Employee> existingEmployee = employeeService.getEmployeesBySalon(salon).stream()
                .filter(e -> e.getPin().equals(employee.getPin()))
                .findFirst();
        if (existingEmployee.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "PIN already in use by another employee.");
            return "redirect:/owner/employees";
        }
        employee.setSalon(salon);
        employee.setRole(Employee.Role.EMPLOYEE);
        employeeService.save(employee);
        redirectAttributes.addFlashAttribute("success", "Employee added successfully.");
        return "redirect:/owner/employees";
    }

    @PostMapping("/employees/{id}/deactivate")
    public String deactivateEmployee(@PathVariable Long id,@RequestParam(value = "action") String action,
                                     RedirectAttributes redirectAttributes) {
        if (action.equals("deactivate")) {
            employeeService.deactivate(id);
            redirectAttributes.addFlashAttribute("success", "Employee deactivated.");
        } else if (action.equals("delete")) {
            employeeService.findById(id).ifPresent(e -> {
               e.setActive(false);
                e.setStatus(Employee.Status.DELETED);
                employeeService.save(e);
            });
            redirectAttributes.addFlashAttribute("success", "Employee deleted.");
        }
        return "redirect:/owner/employees";
    }

    @PostMapping("/employees/{id}/edit")
    public String editEmployee(@PathVariable Long id,
                                @RequestParam String name,
                                @RequestParam String pin,
                                RedirectAttributes redirectAttributes, Authentication auth) {
        Salon salon = getCurrentSalon(auth);
        //to check if the pin is already used by another employee in the same salon
        Optional<Employee> existingEmployee = employeeService.getEmployeesBySalon(salon).stream()
                .filter(e -> e.getPin().equals(pin))
                .filter(e -> !e.getId().equals(id)) // Exclude the employee being edited
                .findFirst();
        if (existingEmployee.isPresent()) {
            redirectAttributes.addFlashAttribute("error", "PIN already in use by another employee.");
            return "redirect:/owner/employees";
        }
        employeeService.findById(id).ifPresent(e -> {
            e.setName(name);
            e.setPin(pin);
            e.setActive(true);
            employeeService.save(e);
        });
        redirectAttributes.addFlashAttribute("success", "Employee updated.");
        return "redirect:/owner/employees";
    }

    // ---- Services ----
    @GetMapping("/services")
    public String services(Authentication auth, Model model) {
        Salon salon = getCurrentSalon(auth);
        model.addAttribute("services", serviceManagementService.getAllServices(salon));
        model.addAttribute("categories", serviceManagementService.getCategories(salon));
        model.addAttribute("newService", new ServiceItem());
        model.addAttribute("newCategory", new ServiceCategory());
        model.addAttribute("salon", salon);
        return "owner/services";
    }

    @PostMapping("/services/add")
    public String addService(@ModelAttribute ServiceItem service,
                              @RequestParam(required = false) Long categoryId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        Salon salon = getCurrentSalon(auth);
        service.setSalon(salon);
        if (categoryId != null) {
            serviceManagementService.findCategoryById(categoryId)
                    .ifPresent(service::setCategory);
        }
        serviceManagementService.saveService(service);
        redirectAttributes.addFlashAttribute("success", "Service added successfully.");
        return "redirect:/owner/services";
    }

    @PostMapping("/services/{id}/edit")
    public String editService(@PathVariable Long id,
                               @RequestParam String name,
                               @RequestParam BigDecimal price,
                               @RequestParam(required = false) Long categoryId,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        Salon salon = getCurrentSalon(auth);
        serviceManagementService.findServiceById(id).ifPresent(s -> {
            s.setName(name);
            s.setPrice(price);
            if (categoryId != null) {
                serviceManagementService.findCategoryById(categoryId)
                        .ifPresent(s::setCategory);
            }
            serviceManagementService.saveService(s);
        });
        redirectAttributes.addFlashAttribute("success", "Service updated.");
        return "redirect:/owner/services";
    }

    @PostMapping("/services/{id}/toggle")
    public String toggleService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        serviceManagementService.toggleServiceStatus(id);
        redirectAttributes.addFlashAttribute("success", "Service status updated.");
        return "redirect:/owner/services";
    }

    @PostMapping("/categories/add")
    public String addCategory(@ModelAttribute ServiceCategory category,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        Salon salon = getCurrentSalon(auth);
        category.setSalon(salon);
        serviceManagementService.saveCategory(category);
        redirectAttributes.addFlashAttribute("success", "Category added.");
        return "redirect:/owner/services";
    }

    // ---- Reports ----
    @GetMapping("/reports/daily")
    public String dailyReport(@RequestParam(required = false) String date,
                               Authentication auth, Model model) {
        Salon salon = getCurrentSalon(auth);
        LocalDate reportDate = (date != null && !date.isEmpty())
                ? LocalDate.parse(date) : LocalDate.now();

        model.addAttribute("salon", salon);
        model.addAttribute("reportDate", reportDate);
        model.addAttribute("totalRevenue", transactionService.getDailyRevenue(salon, reportDate));
        model.addAttribute("totalCustomers", transactionService.getDailyCustomerCount(salon, reportDate));
        model.addAttribute("transactions", transactionService.getTransactionsByDate(salon, reportDate));
        model.addAttribute("employeeRevenue", transactionService.getEmployeeRevenue(salon, reportDate));
        model.addAttribute("topServices", transactionService.getTopServices(salon, reportDate));
        return "owner/report-daily";
    }

    @GetMapping("/reports/monthly")
    public String monthlyReport(@RequestParam(required = false) Integer year,
                                 @RequestParam(required = false) Integer month,
                                 Authentication auth, Model model) {
        Salon salon = getCurrentSalon(auth);
        YearMonth ym = (year != null && month != null)
                ? YearMonth.of(year, month) : YearMonth.now();

        // Build per-day data
        Map<LocalDate, BigDecimal> dailyData = new LinkedHashMap<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate day = ym.atDay(d);
            dailyData.put(day, transactionService.getDailyRevenue(salon, day));
        }

        model.addAttribute("salon", salon);
        model.addAttribute("yearMonth", ym);
        model.addAttribute("totalRevenue", transactionService.getMonthlyRevenue(salon, ym.getYear(), ym.getMonthValue()));
        model.addAttribute("dailyData", dailyData);
        return "owner/report-monthly";
    }

    @GetMapping("/transactions")
    public String allTransactions(Authentication auth, Model model) {
        Salon salon = getCurrentSalon(auth);
        model.addAttribute("salon", salon);
        model.addAttribute("transactions", transactionService.getAllTransactions(salon));
        return "owner/transactions";
    }

    @GetMapping("/reports/range")
    public String rangeReport(@RequestParam(required = false) String from,
                              @RequestParam(required = false) String to,
                              Authentication auth, Model model) {
        Salon salon = getCurrentSalon(auth);

        LocalDate toDate = (to != null && !to.isEmpty()) ? LocalDate.parse(to) : LocalDate.now();
        LocalDate fromDate = (from != null && !from.isEmpty())
                ? LocalDate.parse(from)
                : toDate.with(DayOfWeek.MONDAY);

        long totalCustomers = transactionService.getCustomerCountByRange(salon, fromDate, toDate);
        BigDecimal totalRevenue = transactionService.getRevenueByRange(salon, fromDate, toDate);

        model.addAttribute("salon", salon);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalCustomers", totalCustomers);
        model.addAttribute("dayCount", ChronoUnit.DAYS.between(fromDate, toDate) + 1);
        model.addAttribute("employeePerformance", transactionService.getEmployeePerformanceByRange(salon, fromDate, toDate));
        model.addAttribute("topServices", transactionService.getTopServicesByRange(salon, fromDate, toDate));
        model.addAttribute("transactions", transactionService.getTransactionsByRange(salon, fromDate, toDate));
        return "owner/report-range";
    }

    @GetMapping("/settings")
    public String settings(Authentication auth, Model model) {
        Salon salon = getCurrentSalon(auth);
        model.addAttribute("salon", salon);
        return "owner/settings";
    }

    @PostMapping("/settings/update")
    public String updateSettings(@ModelAttribute Salon updatedSalon,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        Salon salon = getCurrentSalon(auth);
        salon.setFullName(updatedSalon.getFullName());
        salon.setPhone(updatedSalon.getPhone());
        salon.setCity(updatedSalon.getCity());
        salon.setAddress(updatedSalon.getAddress());
        salonRepository.save(salon);
        redirectAttributes.addFlashAttribute("success", "Settings updated.");
        return "redirect:/owner/settings";
    }

    @PostMapping("/transactions/{id}/cancel")
    public String cancelTransaction(@PathVariable Long id, Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        Salon salon = getCurrentSalon(auth);
        transactionService.findById(id).ifPresent(tx -> {
            if (tx.getSalon().getId().equals(salon.getId())
                    && tx.getStatus() == Transaction.Status.COMPLETED) {
                transactionService.cancelTransaction(id);
            }
        });
        redirectAttributes.addFlashAttribute("success", "Transaction cancelled.");
        return "redirect:/owner/transactions";
    }
}
