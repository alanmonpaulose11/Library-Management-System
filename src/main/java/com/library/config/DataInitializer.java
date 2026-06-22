package com.library.config;

import com.library.model.*;
import com.library.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final StudentRepository studentRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final FineRepository fineRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           BookRepository bookRepository,
                           StudentRepository studentRepository,
                           BorrowRecordRepository borrowRecordRepository,
                           FineRepository fineRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.studentRepository = studentRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.fineRepository = fineRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Seed Users
        if (userRepository.count() == 0) {
            userRepository.save(new User("admin", "admin@library.com", passwordEncoder.encode("admin123"), Role.ROLE_ADMIN));
            userRepository.save(new User("librarian", "librarian@library.com", passwordEncoder.encode("lib123"), Role.ROLE_LIBRARIAN));
            userRepository.save(new User("student", "student@library.com", passwordEncoder.encode("student123"), Role.ROLE_STUDENT));
            System.out.println("Initialized mock users: admin, librarian, student");
        }

        // 2. Seed Books
        if (bookRepository.count() == 0) {
            bookRepository.save(new Book("9780132350884", "Clean Code", "Robert C. Martin", "Prentice Hall", "Programming", 5));
            bookRepository.save(new Book("9780134685991", "Effective Java", "Joshua Bloch", "Addison-Wesley", "Programming", 3));
            bookRepository.save(new Book("9780201616224", "The Pragmatic Programmer", "Andy Hunt & Dave Thomas", "Addison-Wesley", "Programming", 4));
            bookRepository.save(new Book("9780596007126", "Head First Design Patterns", "Eric Freeman", "O'Reilly", "Design Patterns", 2));
            bookRepository.save(new Book("9780262033848", "Introduction to Algorithms", "Thomas H. Cormen", "MIT Press", "Algorithms", 3));
            System.out.println("Initialized 5 mock books");
        }

        // 3. Seed Students
        if (studentRepository.count() == 0) {
            studentRepository.save(new Student("Alice Johnson", "alice@edu.com", "+1234567890", "Computer Science", "3rd Year"));
            studentRepository.save(new Student("Bob Smith", "bob@edu.com", "+1987654321", "Electrical Engineering", "2nd Year"));
            studentRepository.save(new Student("Charlie Brown", "charlie@edu.com", "+1555019922", "Mechanical Engineering", "4th Year"));
            System.out.println("Initialized 3 mock students");
        }

        // 4. Seed Borrow Records and Fines
        if (borrowRecordRepository.count() == 0) {
            List<Student> students = studentRepository.findAll();
            List<Book> books = bookRepository.findAll();

            if (!students.isEmpty() && books.size() >= 3) {
                Student alice = students.get(0);
                Student bob = students.get(1);
                Student charlie = students.get(2);

                Book cleanCode = books.get(0);
                Book effectiveJava = books.get(1);
                Book pragmaticProg = books.get(2);

                // Record 1: Alice returned book on time
                BorrowRecord rec1 = new BorrowRecord(alice, cleanCode, LocalDate.now().minusDays(10), LocalDate.now().plusDays(4));
                rec1.setReturnDate(LocalDate.now().minusDays(2));
                borrowRecordRepository.save(rec1);
                
                // Adjust book available quantities for active borrows
                
                // Record 2: Bob has an active borrow, not overdue
                BorrowRecord rec2 = new BorrowRecord(bob, effectiveJava, LocalDate.now().minusDays(5), LocalDate.now().plusDays(9));
                effectiveJava.setAvailableQuantity(effectiveJava.getAvailableQuantity() - 1);
                bookRepository.save(effectiveJava);
                borrowRecordRepository.save(rec2);

                // Record 3: Charlie has an overdue borrow (issued 20 days ago, due 6 days ago, not returned)
                BorrowRecord rec3 = new BorrowRecord(charlie, pragmaticProg, LocalDate.now().minusDays(20), LocalDate.now().minusDays(6));
                pragmaticProg.setAvailableQuantity(pragmaticProg.getAvailableQuantity() - 1);
                bookRepository.save(pragmaticProg);
                BorrowRecord savedRec3 = borrowRecordRepository.save(rec3);

                // Add a fine for Charlie's overdue borrow
                Fine fine = new Fine(savedRec3, 6.00, "UNPAID"); // 6 days overdue * $1.00/day
                fineRepository.save(fine);

                // Record 4: Alice has a paid fine history
                BorrowRecord rec4 = new BorrowRecord(alice, cleanCode, LocalDate.now().minusDays(25), LocalDate.now().minusDays(15));
                rec4.setReturnDate(LocalDate.now().minusDays(10)); // returned 5 days late
                BorrowRecord savedRec4 = borrowRecordRepository.save(rec4);
                Fine finePaid = new Fine(savedRec4, 5.00, "PAID");
                fineRepository.save(finePaid);

                System.out.println("Initialized mock borrowing logs and fines");
            }
        }
    }
}
