package com.library.repository;

import com.library.model.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByStudentIdOrderByIssueDateDesc(Long studentId);
    List<BorrowRecord> findByBookId(Long bookId);
    List<BorrowRecord> findByReturnDateIsNullOrderByDueDateAsc();
    List<BorrowRecord> findByReturnDateIsNotNullOrderByReturnDateDesc();
    
    // Find active overdue borrowings
    List<BorrowRecord> findByDueDateBeforeAndReturnDateIsNull(LocalDate date);

    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.returnDate IS NULL")
    long countActiveBorrows();

    @Query("SELECT COUNT(br) FROM BorrowRecord br WHERE br.dueDate < :today AND br.returnDate IS NULL")
    long countOverdueBorrows(@Param("today") LocalDate today);

    // Most popular books: Book id and counts
    @Query("SELECT br.book, COUNT(br) as borrowCount FROM BorrowRecord br GROUP BY br.book ORDER BY borrowCount DESC")
    List<Object[]> findPopularBooks();

    // Active students: Student id and counts
    @Query("SELECT br.student, COUNT(br) as borrowCount FROM BorrowRecord br GROUP BY br.student ORDER BY borrowCount DESC")
    List<Object[]> findActiveStudents();
}
