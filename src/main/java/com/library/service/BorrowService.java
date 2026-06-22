package com.library.service;

import com.library.model.*;
import com.library.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;
    private final FineRepository fineRepository;

    private static final double DAILY_FINE_RATE = 1.00; // $1.00 per day overdue

    public BorrowService(BorrowRecordRepository borrowRecordRepository,
                         BookRepository bookRepository,
                         StudentRepository studentRepository,
                         FineRepository fineRepository) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
        this.fineRepository = fineRepository;
    }

    public List<BorrowRecord> getActiveBorrows() {
        return borrowRecordRepository.findByReturnDateIsNullOrderByDueDateAsc();
    }

    public List<BorrowRecord> getBorrowHistory() {
        return borrowRecordRepository.findByReturnDateIsNotNullOrderByReturnDateDesc();
    }

    public List<BorrowRecord> getStudentBorrowHistory(Long studentId) {
        return borrowRecordRepository.findByStudentIdOrderByIssueDateDesc(studentId);
    }

    public List<BorrowRecord> getAllBorrowRecords() {
        return borrowRecordRepository.findAll();
    }

    @Transactional
    public BorrowRecord issueBook(Long studentId, Long bookId, int borrowDays) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found with ID: " + studentId));
        
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + bookId));

        if (book.getAvailableQuantity() <= 0) {
            throw new IllegalStateException("Book is currently unavailable (all copies borrowed).");
        }

        // Decrement book availability
        book.setAvailableQuantity(book.getAvailableQuantity() - 1);
        bookRepository.save(book);

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(borrowDays > 0 ? borrowDays : 14);

        BorrowRecord record = new BorrowRecord(student, book, issueDate, dueDate);
        return borrowRecordRepository.save(record);
    }

    @Transactional
    public BorrowRecord returnBook(Long borrowRecordId) {
        BorrowRecord record = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Borrow record not found with ID: " + borrowRecordId));

        if (record.getReturnDate() != null) {
            throw new IllegalStateException("Book has already been returned.");
        }

        LocalDate returnDate = LocalDate.now();
        record.setReturnDate(returnDate);

        // Increment book availability
        Book book = record.getBook();
        book.setAvailableQuantity(book.getAvailableQuantity() + 1);
        bookRepository.save(book);

        BorrowRecord savedRecord = borrowRecordRepository.save(record);

        // Check if overdue and calculate fine
        if (returnDate.isAfter(record.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), returnDate);
            double fineAmount = overdueDays * DAILY_FINE_RATE;

            if (fineAmount > 0) {
                Optional<Fine> existingFineOpt = fineRepository.findByBorrowRecordId(borrowRecordId);
                if (existingFineOpt.isPresent()) {
                    Fine fine = existingFineOpt.get();
                    if ("UNPAID".equals(fine.getPaymentStatus())) {
                        fine.setAmount(fineAmount);
                        fineRepository.save(fine);
                    }
                } else {
                    Fine fine = new Fine(savedRecord, fineAmount, "UNPAID");
                    fineRepository.save(fine);
                }
            }
        }

        return savedRecord;
    }

    @Transactional
    public BorrowRecord renewBook(Long borrowRecordId, int renewDays) {
        BorrowRecord record = borrowRecordRepository.findById(borrowRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Borrow record not found with ID: " + borrowRecordId));

        if (record.getReturnDate() != null) {
            throw new IllegalStateException("Cannot renew a returned book.");
        }

        // Extend due date
        LocalDate newDueDate = record.getDueDate().plusDays(renewDays > 0 ? renewDays : 7);
        record.setDueDate(newDueDate);

        return borrowRecordRepository.save(record);
    }

    @Transactional
    public Fine payFine(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new IllegalArgumentException("Fine record not found with ID: " + fineId));
        fine.setPaymentStatus("PAID");
        return fineRepository.save(fine);
    }

    @Transactional
    public void calculateAndUpdatePendingFines() {
        LocalDate today = LocalDate.now();
        List<BorrowRecord> overdueActiveBorrows = borrowRecordRepository.findByDueDateBeforeAndReturnDateIsNull(today);

        for (BorrowRecord record : overdueActiveBorrows) {
            long overdueDays = ChronoUnit.DAYS.between(record.getDueDate(), today);
            double fineAmount = overdueDays * DAILY_FINE_RATE;

            if (fineAmount > 0) {
                Optional<Fine> fineOpt = fineRepository.findByBorrowRecordId(record.getId());
                if (fineOpt.isPresent()) {
                    Fine fine = fineOpt.get();
                    if ("UNPAID".equals(fine.getPaymentStatus())) {
                        fine.setAmount(fineAmount);
                        fineRepository.save(fine);
                    }
                } else {
                    Fine fine = new Fine(record, fineAmount, "UNPAID");
                    fineRepository.save(fine);
                }
            }
        }
    }
}
