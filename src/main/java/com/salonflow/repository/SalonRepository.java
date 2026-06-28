package com.salonflow.repository;

import com.salonflow.model.Salon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SalonRepository extends JpaRepository<Salon, Long> {
    Optional<Salon> findBySalonCode(String salonCode);
    Optional<Salon> findByEmail(String email);
    boolean existsBySalonCode(String salonCode);
    boolean existsByEmail(String email);
}
