package com.library.service;

import com.library.model.Book;
import com.library.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    public List<Book> searchBooks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllBooks();
        }
        return bookRepository.searchBooks(query.trim());
    }

    public Book getBookById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ID: " + id));
    }

    @Transactional
    public Book createBook(Book book) {
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new IllegalArgumentException("Book with ISBN " + book.getIsbn() + " already exists.");
        }
        book.setAvailableQuantity(book.getQuantity());
        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(Long id, Book updatedBook) {
        Book existingBook = getBookById(id);

        if (!existingBook.getIsbn().equals(updatedBook.getIsbn()) && bookRepository.existsByIsbn(updatedBook.getIsbn())) {
            throw new IllegalArgumentException("Book with ISBN " + updatedBook.getIsbn() + " already exists.");
        }

        // Calculate new available quantity based on updated quantity
        int borrowedCount = existingBook.getQuantity() - existingBook.getAvailableQuantity();
        if (updatedBook.getQuantity() < borrowedCount) {
            throw new IllegalArgumentException("Cannot reduce quantity below the number of currently borrowed copies (" + borrowedCount + ").");
        }

        existingBook.setTitle(updatedBook.getTitle());
        existingBook.setAuthor(updatedBook.getAuthor());
        existingBook.setPublisher(updatedBook.getPublisher());
        existingBook.setCategory(updatedBook.getCategory());
        existingBook.setQuantity(updatedBook.getQuantity());
        existingBook.setAvailableQuantity(updatedBook.getQuantity() - borrowedCount);
        existingBook.setIsbn(updatedBook.getIsbn());

        return bookRepository.save(existingBook);
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = getBookById(id);
        int borrowedCount = book.getQuantity() - book.getAvailableQuantity();
        if (borrowedCount > 0) {
            throw new IllegalStateException("Cannot delete book. It has " + borrowedCount + " active borrows.");
        }
        bookRepository.delete(book);
    }

    public List<String> getCategories() {
        return bookRepository.findUniqueCategories();
    }

    public List<Book> getBooksByCategory(String category) {
        return bookRepository.findByCategoryIgnoreCase(category);
    }
}
