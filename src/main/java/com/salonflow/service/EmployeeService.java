package com.salonflow.service;

import com.salonflow.model.Employee;
import com.salonflow.model.Salon;
import com.salonflow.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public Optional<Employee> authenticateEmployee(Salon salon, String pin) {
        return employeeRepository.findBySalonAndPinAndActiveTrue(salon, pin);
    }

    public List<Employee> getEmployeesBySalon(Salon salon) {
        return employeeRepository.findBySalonOrderByNameAsc(salon)
                .stream()
                .filter(e -> !e.getStatus().equals(Employee.Status.DELETED))
                .toList();
    }

    public List<Employee> getActiveEmployees(Salon salon) {
        return employeeRepository.findBySalonAndActiveTrue(salon);
    }

    @Transactional
    public Employee save(Employee employee) {
        return employeeRepository.save(employee);
    }

    public Optional<Employee> findById(Long id) {
        return employeeRepository.findById(id);
    }

    @Transactional
    public void deactivate(Long id) {
        employeeRepository.findById(id).ifPresent(e -> {
            e.setActive(false);
            employeeRepository.save(e);
        });
    }
}
