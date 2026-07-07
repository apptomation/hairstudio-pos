package com.salonflow.service;

import com.salonflow.model.*;
import com.salonflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ServiceItemRepository serviceItemRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public Transaction createTransaction(Long employeeId, Salon salon, List<Long> serviceIds) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        List<ServiceItem> services = serviceItemRepository.findAllById(serviceIds);

        BigDecimal total = services.stream()
                .map(ServiceItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Transaction transaction = Transaction.builder()
                .employee(employee)
                .salon(salon)
                .totalAmount(total)
                .paymentMethod(Transaction.PaymentMethod.CASH)
                .status(Transaction.Status.COMPLETED)
                .createdAt(LocalDateTime.now(ZoneId.of("America/New_York")))
                .build();

        Transaction saved = transactionRepository.save(transaction);

        List<TransactionItem> items = new ArrayList<>(services.stream().map(s ->
                TransactionItem.builder()
                        .transaction(saved)
                        .service(s)
                        .serviceName(s.getName())
                        .price(s.getPrice())
                        .quantity(1)
                        .build()
        ).toList());

        saved.setItems(items);
        return transactionRepository.save(saved);
    }

    @Transactional
    public void cancelTransaction(Long id) {
        transactionRepository.findById(id).ifPresent(tx -> {
            tx.setStatus(Transaction.Status.CANCELLED);
            transactionRepository.save(tx);
        });
    }
    public List<Transaction> getTransactionsByDate(Salon salon, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);
        return transactionRepository.findBySalonAndStatusNotAndCreatedAtBetweenOrderByCreatedAtDesc(salon,Transaction.Status.CANCELLED, start, end);
    }

    public List<Transaction> getAllTransactions(Salon salon) {
        return transactionRepository.findBySalonOrderByCreatedAtDesc(salon);
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    // --- Report data ---

    public BigDecimal getDailyRevenue(Salon salon, LocalDate date) {
        return transactionRepository.sumRevenueByDateRange(
                salon, date.atStartOfDay(), date.atTime(23, 59, 59));
    }

    public long getDailyCustomerCount(Salon salon, LocalDate date) {
        return transactionRepository.countByDateRange(
                salon, date.atStartOfDay(), date.atTime(23, 59, 59));
    }

    public BigDecimal getMonthlyRevenue(Salon salon, int year, int month) {
        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1).atTime(23, 59, 59);
        return transactionRepository.sumRevenueByDateRange(salon, start, end);
    }

    public Map<String, BigDecimal> getEmployeeRevenue(Salon salon, LocalDate date) {
        List<Employee> employees = employeeRepository.findBySalonAndActiveTrue(salon);
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Employee emp : employees) {
            BigDecimal rev = transactionRepository.sumRevenueByEmployee(
                    emp.getId(), date.atStartOfDay(), date.atTime(23, 59, 59));
            result.put(emp.getName(), rev);
        }
        return result;
    }

    public List<Object[]> getTopServices(Salon salon, LocalDate date) {
        return transactionRepository.findTopServicesByDateRange(
                salon, date.atStartOfDay(), date.atTime(23, 59, 59));
    }

    // --- Date range report data ---

    public BigDecimal getRevenueByRange(Salon salon, LocalDate from, LocalDate to) {
        return transactionRepository.sumRevenueByDateRange(
                salon, from.atStartOfDay(), to.atTime(23, 59, 59));
    }

    public long getCustomerCountByRange(Salon salon, LocalDate from, LocalDate to) {
        return transactionRepository.countByDateRange(
                salon, from.atStartOfDay(), to.atTime(23, 59, 59));
    }

    public List<Object[]> getEmployeePerformanceByRange(Salon salon, LocalDate from, LocalDate to) {
        return transactionRepository.findEmployeePerformanceByDateRange(
                salon, from.atStartOfDay(), to.atTime(23, 59, 59));
    }

    public List<Object[]> getTopServicesByRange(Salon salon, LocalDate from, LocalDate to) {
        return transactionRepository.findTopServicesByDateRange(
                salon, from.atStartOfDay(), to.atTime(23, 59, 59));
    }

    public List<Transaction> getTransactionsByRange(Salon salon, LocalDate from, LocalDate to) {
        return transactionRepository.findBySalonAndStatusNotAndCreatedAtBetweenOrderByCreatedAtDesc(
                salon, Transaction.Status.CANCELLED, from.atStartOfDay(), to.atTime(23, 59, 59));
    }
}
