package com.library.controller;

import com.library.service.BookService;
import com.library.service.StudentService;
import com.library.service.BorrowService;
import com.library.service.ReportService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.Map;

@Controller
public class ViewController {

    private final BookService bookService;
    private final StudentService studentService;
    private final BorrowService borrowService;
    private final ReportService reportService;

    public ViewController(BookService bookService,
                          StudentService studentService,
                          BorrowService borrowService,
                          ReportService reportService) {
        this.bookService = bookService;
        this.studentService = studentService;
        this.borrowService = borrowService;
        this.reportService = reportService;
    }

    @ModelAttribute
    public void addUserDetails(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            model.addAttribute("username", auth.getName());
            // Map e.g. "ROLE_ADMIN" to "ADMIN" for simpler UI display
            String roleWithPrefix = auth.getAuthorities().iterator().next().getAuthority();
            model.addAttribute("role", roleWithPrefix);
            model.addAttribute("roleShort", roleWithPrefix.replace("ROLE_", ""));
        }
    }

    @GetMapping("/login")
    public String login() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return "redirect:/";
        }
        return "register";
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        Map<String, Object> stats = reportService.getDashboardStats();
        model.addAllAttributes(stats);
        model.addAttribute("activeBorrows", borrowService.getActiveBorrows());
        return "dashboard";
    }

    @GetMapping("/books")
    public String books(Model model) {
        model.addAttribute("books", bookService.getAllBooks());
        model.addAttribute("categories", bookService.getCategories());
        return "books";
    }

    @GetMapping("/students")
    public String students(Model model) {
        model.addAttribute("students", studentService.getAllStudents());
        return "students";
    }

    @GetMapping("/borrowing")
    public String borrowing(Model model) {
        model.addAttribute("students", studentService.getAllStudents());
        model.addAttribute("books", bookService.getAllBooks());
        model.addAttribute("activeBorrows", borrowService.getActiveBorrows());
        model.addAttribute("borrowHistory", borrowService.getBorrowHistory());
        return "borrowing";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        model.addAttribute("popularBooks", reportService.getMostBorrowedBooks());
        model.addAttribute("activeStudents", reportService.getActiveStudents());
        model.addAttribute("monthlyStats", reportService.getMonthlyBorrowStats());
        return "reports";
    }
}
