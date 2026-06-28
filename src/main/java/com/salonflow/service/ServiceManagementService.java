package com.salonflow.service;

import com.salonflow.model.Salon;
import com.salonflow.model.ServiceCategory;
import com.salonflow.model.ServiceItem;
import com.salonflow.repository.ServiceCategoryRepository;
import com.salonflow.repository.ServiceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ServiceManagementService {

    private final ServiceItemRepository serviceItemRepository;
    private final ServiceCategoryRepository categoryRepository;

    public List<ServiceItem> getActiveServices(Salon salon) {
        return serviceItemRepository.findBySalonAndActiveTrueOrderByNameAsc(salon);
    }

    public List<ServiceItem> getAllServices(Salon salon) {
        return serviceItemRepository.findBySalonOrderByNameAsc(salon);
    }

    public List<ServiceCategory> getCategories(Salon salon) {
        return categoryRepository.findBySalonOrderByNameAsc(salon);
    }

    @Transactional
    public ServiceItem saveService(ServiceItem service) {
        return serviceItemRepository.save(service);
    }

    @Transactional
    public ServiceCategory saveCategory(ServiceCategory category) {
        return categoryRepository.save(category);
    }

    public Optional<ServiceItem> findServiceById(Long id) {
        return serviceItemRepository.findById(id);
    }

    public Optional<ServiceCategory> findCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional
    public void toggleServiceStatus(Long id) {
        serviceItemRepository.findById(id).ifPresent(s -> {
            s.setActive(!s.isActive());
            serviceItemRepository.save(s);
        });
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
