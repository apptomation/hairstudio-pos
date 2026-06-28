package com.salonflow.config;

import com.salonflow.model.*;
import com.salonflow.repository.*;
import com.salonflow.service.SalonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SalonRepository salonRepository;
    private final SalonService salonService;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    public void run(String... args) {
        if (salonRepository.count() > 0) return; // Already seeded

        log.info("🌱 Seeding demo data...");

        // Create demo salon
        Salon salon = Salon.builder()
                .fullName("Glam Beauty Studio")
                .email("owner@glambeauty.com")
                .password("password123") // will be encoded by registerSalon
                .phone("+961 70 000 000")
                .country("Lebanon")
                .city("Beirut")
                .address("Hamra Street, 12th Floor")
                .build();

        Salon saved = salonService.registerSalon(salon);
        log.info("✅ Demo salon created: {} (Code: {})", saved.getFullName(), saved.getSalonCode());

        // Add employees
        employeeRepository.save(Employee.builder()
                .name("Jana").pin("1234").role(Employee.Role.EMPLOYEE).salon(saved).active(true).build());
        employeeRepository.save(Employee.builder()
                .name("Ali").pin("5678").role(Employee.Role.EMPLOYEE).salon(saved).active(true).build());
        employeeRepository.save(Employee.builder()
                .name("Sarah").pin("9012").role(Employee.Role.EMPLOYEE).salon(saved).active(true).build());

        // Create categories
        ServiceCategory hair = categoryRepository.save(ServiceCategory.builder()
                .name("Hair Services").salon(saved).build());
        ServiceCategory beauty = categoryRepository.save(ServiceCategory.builder()
                .name("Beauty Services").salon(saved).build());

        // Create services
        serviceItemRepository.save(ServiceItem.builder().name("Hair Cut").price(new BigDecimal("20")).category(hair).salon(saved).active(true).build());
        serviceItemRepository.save(ServiceItem.builder().name("Beard Trim").price(new BigDecimal("15")).category(hair).salon(saved).active(true).build());
        serviceItemRepository.save(ServiceItem.builder().name("Hair Color").price(new BigDecimal("60")).category(hair).salon(saved).active(true).build());
        serviceItemRepository.save(ServiceItem.builder().name("Nails").price(new BigDecimal("40")).category(beauty).salon(saved).active(true).build());
        serviceItemRepository.save(ServiceItem.builder().name("Facial").price(new BigDecimal("50")).category(beauty).salon(saved).active(true).build());
        serviceItemRepository.save(ServiceItem.builder().name("Waxing").price(new BigDecimal("35")).category(beauty).salon(saved).active(true).build());

        log.info("✅ Demo data seeded! Login: owner@glambeauty.com / password123");
        log.info("📌 Salon Code: {} | Employee PINs: Jana=1234, Ali=5678, Sarah=9012", saved.getSalonCode());
    }
}
