package com.library.repository;

import com.library.model.Fine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {
    Optional<Fine> findByBorrowRecordId(Long borrowRecordId);
    List<Fine> findByPaymentStatus(String paymentStatus);

    @Query("SELECT COALESCE(SUM(f.amount), 0.0) FROM Fine f WHERE f.paymentStatus = :status")
    double sumByPaymentStatus(@Param("status") String status);

    @Query("SELECT COALESCE(SUM(f.amount), 0.0) FROM Fine f")
    double sumTotalFines();
}
