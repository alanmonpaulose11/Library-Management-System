package com.library.service;

import com.library.model.*;
import com.library.repository.*;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final FineRepository fineRepository;
    private final BorrowService borrowService;

    public ReportService(BookRepository bookRepository,
                         StudentRepository studentRepository,
                         BorrowRecordRepository borrowRecordRepository,
                         FineRepository fineRepository,
                         BorrowService borrowService) {
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.fineRepository = fineRepository;
        this.borrowService = borrowService;
    }

    public Map<String, Object> getDashboardStats() {
        // Automatically sync overdue fines before displaying dashboard
        borrowService.calculateAndUpdatePendingFines();

        Map<String, Object> stats = new HashMap<>();

        long totalBooks = bookRepository.findAll().stream().mapToLong(Book::getQuantity).sum();
        long availableBooks = bookRepository.findAll().stream().mapToLong(Book::getAvailableQuantity).sum();
        long issuedBooks = borrowRecordRepository.countActiveBorrows();
        long totalStudents = studentRepository.count();
        long overdueBooks = borrowRecordRepository.countOverdueBorrows(LocalDate.now());

        double paidFines = fineRepository.sumByPaymentStatus("PAID");
        double unpaidFines = fineRepository.sumByPaymentStatus("UNPAID");
        double totalFines = fineRepository.sumTotalFines();

        stats.put("totalBooks", totalBooks);
        stats.put("availableBooks", availableBooks);
        stats.put("issuedBooks", issuedBooks);
        stats.put("totalStudents", totalStudents);
        stats.put("overdueBooks", overdueBooks);
        stats.put("paidFines", paidFines);
        stats.put("unpaidFines", unpaidFines);
        stats.put("totalFines", totalFines);

        return stats;
    }

    public List<Map<String, Object>> getMostBorrowedBooks() {
        List<Object[]> popular = borrowRecordRepository.findPopularBooks();
        List<Map<String, Object>> list = new ArrayList<>();
        
        // Take top 10
        int limit = Math.min(popular.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = popular.get(i);
            Book book = (Book) row[0];
            Long count = (Long) row[1];
            
            Map<String, Object> map = new HashMap<>();
            map.put("book", book);
            map.put("borrowCount", count);
            list.add(map);
        }
        return list;
    }

    public List<Map<String, Object>> getActiveStudents() {
        List<Object[]> active = borrowRecordRepository.findActiveStudents();
        List<Map<String, Object>> list = new ArrayList<>();
        
        // Take top 10
        int limit = Math.min(active.size(), 10);
        for (int i = 0; i < limit; i++) {
            Object[] row = active.get(i);
            Student student = (Student) row[0];
            Long count = (Long) row[1];
            
            Map<String, Object> map = new HashMap<>();
            map.put("student", student);
            map.put("borrowCount", count);
            list.add(map);
        }
        return list;
    }

    public Map<String, Long> getMonthlyBorrowStats() {
        List<BorrowRecord> records = borrowRecordRepository.findAll();
        Map<String, Long> stats = new TreeMap<>(); // Sorted keys: "2026-01", etc.
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        for (BorrowRecord r : records) {
            String month = r.getIssueDate().format(formatter);
            stats.put(month, stats.getOrDefault(month, 0L) + 1);
        }
        return stats;
    }

    public ByteArrayInputStream exportToExcel() throws IOException {
        String[] bookHeaders = {"ID", "ISBN", "Title", "Author", "Publisher", "Category", "Total Qty", "Available Qty"};
        String[] borrowHeaders = {"Record ID", "Student Name", "Student Email", "Book Title", "Issue Date", "Due Date", "Returned Date"};

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Sheet 1: Books Inventory
            Sheet bookSheet = workbook.createSheet("Books Inventory");
            
            // Header font and style
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create book header row
            Row headerRow = bookSheet.createRow(0);
            for (int col = 0; col < bookHeaders.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(bookHeaders[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // Populate books data
            List<Book> books = bookRepository.findAll();
            int rowIdx = 1;
            for (Book book : books) {
                Row row = bookSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(book.getId());
                row.createCell(1).setCellValue(book.getIsbn());
                row.createCell(2).setCellValue(book.getTitle());
                row.createCell(3).setCellValue(book.getAuthor());
                row.createCell(4).setCellValue(book.getPublisher());
                row.createCell(5).setCellValue(book.getCategory());
                row.createCell(6).setCellValue(book.getQuantity());
                row.createCell(7).setCellValue(book.getAvailableQuantity());
            }

            // Auto-resize columns
            for (int col = 0; col < bookHeaders.length; col++) {
                bookSheet.autoSizeColumn(col);
            }

            // Sheet 2: Borrowing History
            Sheet borrowSheet = workbook.createSheet("Borrow History");
            Row borrowHeaderRow = borrowSheet.createRow(0);
            for (int col = 0; col < borrowHeaders.length; col++) {
                Cell cell = borrowHeaderRow.createCell(col);
                cell.setCellValue(borrowHeaders[col]);
                cell.setCellStyle(headerCellStyle);
            }

            List<BorrowRecord> borrows = borrowRecordRepository.findAll();
            rowIdx = 1;
            for (BorrowRecord br : borrows) {
                Row row = borrowSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(br.getId());
                row.createCell(1).setCellValue(br.getStudent().getName());
                row.createCell(2).setCellValue(br.getStudent().getEmail());
                row.createCell(3).setCellValue(br.getBook().getTitle());
                row.createCell(4).setCellValue(br.getIssueDate().toString());
                row.createCell(5).setCellValue(br.getDueDate().toString());
                row.createCell(6).setCellValue(br.getReturnDate() != null ? br.getReturnDate().toString() : "Not Returned");
            }

            for (int col = 0; col < borrowHeaders.length; col++) {
                borrowSheet.autoSizeColumn(col);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportToPdf() {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Typography styles
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.GRAY);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);

            // Document Header
            Paragraph title = new Paragraph("Library Management System Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            Paragraph date = new Paragraph("Generated on: " + LocalDate.now().toString(), subTitleFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            // Add basic statistics paragraph
            Map<String, Object> stats = getDashboardStats();
            String statsSummary = String.format("Summary: Total Books: %s | Available Books: %s | Active Borrows: %s | Overdue: %s | Fine Collected: $%s",
                    stats.get("totalBooks"), stats.get("availableBooks"), stats.get("issuedBooks"), stats.get("overdueBooks"), stats.get("paidFines"));
            Paragraph summaryParagraph = new Paragraph(statsSummary, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLUE));
            summaryParagraph.setAlignment(Element.ALIGN_LEFT);
            summaryParagraph.setSpacingAfter(15);
            document.add(summaryParagraph);

            // Section 1: Overdue Books & Active Borrows
            Paragraph activeHeading = new Paragraph("Active Borrowing Details", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK));
            activeHeading.setSpacingAfter(10);
            document.add(activeHeading);

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new int[]{1, 3, 3, 2, 2, 2});

            // Set headers
            String[] headers = {"ID", "Student", "Book", "Issue Date", "Due Date", "Status"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, tableHeaderFont));
                cell.setBackgroundColor(Color.BLUE);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            List<BorrowRecord> activeBorrows = borrowRecordRepository.findByReturnDateIsNullOrderByDueDateAsc();
            LocalDate today = LocalDate.now();

            for (BorrowRecord br : activeBorrows) {
                table.addCell(new PdfPCell(new Phrase(String.valueOf(br.getId()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(br.getStudent().getName(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(br.getBook().getTitle(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(br.getIssueDate().toString(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(br.getDueDate().toString(), bodyFont)));

                String status = br.getDueDate().isBefore(today) ? "OVERDUE" : "OK";
                PdfPCell statusCell = new PdfPCell(new Phrase(status, bodyFont));
                if ("OVERDUE".equals(status)) {
                    statusCell.setBackgroundColor(new Color(255, 230, 230)); // Red shade
                }
                table.addCell(statusCell);
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayInputStream(out.toByteArray());
    }
}
