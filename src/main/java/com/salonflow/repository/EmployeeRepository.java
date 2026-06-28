package com.salonflow.repository;

import com.salonflow.model.Employee;
import com.salonflow.model.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findBySalonAndPinAndActiveTrue(Salon salon, String pin);
    List<Employee> findBySalonOrderByNameAsc(Salon salon);
    List<Employee> findBySalonAndActiveTrue(Salon salon);
}
