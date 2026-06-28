package com.salonflow.service;

import com.salonflow.model.*;
import com.salonflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalonService {

    private final SalonRepository salonRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Salon registerSalon(Salon salon) {
        // Generate unique salon code
        String code = generateSalonCode(salon.getFullName());
        salon.setSalonCode(code);
        salon.setPassword(passwordEncoder.encode(salon.getPassword()));

        Salon saved = salonRepository.save(salon);

        // Auto-create owner employee record
        Employee owner = Employee.builder()
                .name(salon.getFullName())
                .pin("0000")
                .role(Employee.Role.OWNER)
                .salon(saved)
                .active(true)
                .build();
        employeeRepository.save(owner);

        return saved;
    }

    public Optional<Salon> findBySalonCode(String code) {
        return salonRepository.findBySalonCode(code);
    }

    public Optional<Salon> findByEmail(String email) {
        return salonRepository.findByEmail(email);
    }

    public boolean emailExists(String email) {
        return salonRepository.existsByEmail(email);
    }

    public Salon save(Salon salon) {
        return salonRepository.save(salon);
    }

    private String generateSalonCode(String salonName) {
        String prefix = salonName.replaceAll("[^A-Za-z]", "")
                .toUpperCase()
                .substring(0, Math.min(5, salonName.replaceAll("[^A-Za-z]", "").length()));
        String suffix = String.format("%03d", (int)(Math.random() * 900) + 100);
        String code = prefix + suffix;
        // Ensure uniqueness
        while (salonRepository.existsBySalonCode(code)) {
            suffix = String.format("%03d", (int)(Math.random() * 900) + 100);
            code = prefix + suffix;
        }
        return code;
    }
}
