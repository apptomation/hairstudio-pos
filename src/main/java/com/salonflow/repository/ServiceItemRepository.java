package com.salonflow.repository;

import com.salonflow.model.Salon;
import com.salonflow.model.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {
    List<ServiceItem> findBySalonAndActiveTrueOrderByNameAsc(Salon salon);
    List<ServiceItem> findBySalonOrderByNameAsc(Salon salon);
    List<ServiceItem> findByCategoryId(Long categoryId);
}
