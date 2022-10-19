package com.rocktech.userbooks;

import com.rocktech.betterreads.book.Book;
import com.rocktech.betterreads.book.BookRepository;
import com.rocktech.user.BooksByUser;
import com.rocktech.user.BooksByUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Controller
public class UserBooksController {

    @Autowired
    UserBooksRepository userBooksRepository;

    @Autowired
    BooksByUserRepository booksByUserRepository;

    @Autowired
    BookRepository bookRepository;

    @PostMapping("/add-user-book")
    public ModelAndView addBookForUser(@RequestBody MultiValueMap<String, String> formData,
                                       @AuthenticationPrincipal OAuth2User principal) {
        String userId = principal.getAttribute("login");
        if (userId == null)
            return null;

        String bookId = Objects.requireNonNull(formData.getFirst("bookId"));
        Optional<Book> optionalBook = bookRepository.findById(bookId);

        if (optionalBook.isEmpty()) {
            return new ModelAndView("redirect:/");
        }
        Book book = optionalBook.get();

        UserBooksPrimaryKey key = new UserBooksPrimaryKey();
        key.setUserId(userId);
        key.setBookId(bookId);

        int rating = Integer.parseInt(Objects.requireNonNull(formData.getFirst("rating")));
        String status = Objects.requireNonNull(formData.getFirst("readingStatus"));

        UserBooks userBooks = new UserBooks();
        userBooks.setKey(key);
        userBooks.setStartedDate(LocalDate.parse(Objects.requireNonNull(formData.getFirst("startDate"))));
        userBooks.setCompletedDate(LocalDate.parse(Objects.requireNonNull(formData.getFirst("completedDate"))));
        userBooks.setRating(rating);
        userBooks.setReadingStatus(status);

        userBooksRepository.save(userBooks);

        BooksByUser booksByUser = new BooksByUser();
        booksByUser.setId(userId);
        booksByUser.setBookId(bookId);
        booksByUser.setBookName(book.getName());
        booksByUser.setCoverIds(book.getCoverIds());
        booksByUser.setAuthorNames(book.getAuthorNames());
        booksByUser.setReadingStatus(status);
        booksByUser.setRating(rating);
        booksByUserRepository.save(booksByUser);

        return new ModelAndView("redirect:/books/" + bookId);
    }
}
