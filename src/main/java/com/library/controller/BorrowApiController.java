package com.library.controller;

import com.library.dto.IssueRequest;
import com.library.model.BorrowRecord;
import com.library.model.Fine;
import com.library.service.BorrowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LIBRARIAN')")
public class BorrowApiController {

    private final BorrowService borrowService;

    public BorrowApiController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping("/borrow")
    public ResponseEntity<?> issueBook(@Valid @RequestBody IssueRequest request) {
        try {
            BorrowRecord record = borrowService.issueBook(
                    request.getStudentId(),
                    request.getBookId(),
                    request.getBorrowDays()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(record);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/return")
    public ResponseEntity<?> returnBook(@RequestBody Map<String, Long> payload) {
        Long borrowRecordId = payload.get("borrowRecordId");
        if (borrowRecordId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "borrowRecordId is required"));
        }

        try {
            BorrowRecord record = borrowService.returnBook(borrowRecordId);
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/borrow/renew")
    public ResponseEntity<?> renewBook(@RequestBody Map<String, Object> payload) {
        Number recordIdNum = (Number) payload.get("borrowRecordId");
        Number renewDaysNum = (Number) payload.get("renewDays");

        if (recordIdNum == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "borrowRecordId is required"));
        }

        Long borrowRecordId = recordIdNum.longValue();
        int renewDays = renewDaysNum != null ? renewDaysNum.intValue() : 7;

        try {
            BorrowRecord record = borrowService.renewBook(borrowRecordId, renewDays);
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/borrow/pay-fine/{fineId}")
    public ResponseEntity<?> payFine(@PathVariable Long fineId) {
        try {
            Fine fine = borrowService.payFine(fineId);
            return ResponseEntity.ok(fine);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<BorrowRecord>> getHistory() {
        return ResponseEntity.ok(borrowService.getAllBorrowRecords());
    }

    @GetMapping("/history/student/{studentId}")
    public ResponseEntity<List<BorrowRecord>> getStudentHistory(@PathVariable Long studentId) {
        return ResponseEntity.ok(borrowService.getStudentBorrowHistory(studentId));
    }

    @GetMapping("/borrow/active")
    public ResponseEntity<List<BorrowRecord>> getActiveBorrows() {
        return ResponseEntity.ok(borrowService.getActiveBorrows());
    }
}
