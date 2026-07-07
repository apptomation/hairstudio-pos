package com.salonflow.repository;

import com.salonflow.model.Salon;
import com.salonflow.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Used for reports & dashboard — excludes CANCELLED
    List<Transaction> findBySalonAndStatusNotAndCreatedAtBetweenOrderByCreatedAtDesc(
            Salon salon, Transaction.Status status, LocalDateTime start, LocalDateTime end);

    // Used for the all-transactions list — includes CANCELLED
    List<Transaction> findBySalonOrderByCreatedAtDesc(Salon salon);

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t WHERE t.salon = :salon AND t.createdAt BETWEEN :start AND :end AND t.status = 'COMPLETED'")
    BigDecimal sumRevenueByDateRange(@Param("salon") Salon salon,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.salon = :salon AND t.createdAt BETWEEN :start AND :end AND t.status = 'COMPLETED'")
    long countByDateRange(@Param("salon") Salon salon,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.totalAmount), 0) FROM Transaction t WHERE t.employee.id = :employeeId AND t.createdAt BETWEEN :start AND :end AND t.status = 'COMPLETED'")
    BigDecimal sumRevenueByEmployee(@Param("employeeId") Long employeeId,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    @Query("SELECT ti.serviceName, COUNT(ti) as cnt FROM TransactionItem ti " +
            "JOIN ti.transaction t WHERE t.salon = :salon AND t.createdAt BETWEEN :start AND :end " +
            "AND t.status = 'COMPLETED' " +
            "GROUP BY ti.serviceName ORDER BY cnt DESC")
    List<Object[]> findTopServicesByDateRange(@Param("salon") Salon salon,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query("SELECT t.employee.name, COUNT(t), COALESCE(SUM(t.totalAmount), 0) " +
           "FROM Transaction t WHERE t.salon = :salon AND t.createdAt BETWEEN :start AND :end " +
           "AND t.status = 'COMPLETED' " +
           "GROUP BY t.employee.id, t.employee.name ORDER BY SUM(t.totalAmount) DESC")
    List<Object[]> findEmployeePerformanceByDateRange(@Param("salon") Salon salon,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);
}
