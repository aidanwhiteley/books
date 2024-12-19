package com.aidanwhiteley.books.controller;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

import static com.aidanwhiteley.books.domain.Book.Rating.GREAT;

@Controller
public class BookControllerHtmx {

    private final BookRepository bookRepository;

    public BookControllerHtmx(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping(value = "/home")
    public String index(Model model, Principal principal) {
        PageRequest pageObj = PageRequest.of(0, 30);
        Page<Book> page = bookRepository.findByRatingOrderByCreatedDateTimeDesc(pageObj, GREAT);

        List<Book> books = getBooksWithRequiredImages(page);
        model.addAttribute("books", books.stream().toList());
        model.addAttribute("rating", "great");

        return "home";
    }

    @GetMapping(value = {"/getBooksByRating"}, params = {"rating", "bookRating"})
    public String findByRating(Model model, @RequestParam String rating) {

        Book.Rating ipRating = Book.Rating.getRatingByString(rating);
        if (ipRating == null) {
            throw new IllegalArgumentException("Input rating " + rating + " not valid");
        }

        PageRequest pageObj = PageRequest.of(0, 30);
        Page<Book> page = bookRepository.findByRatingOrderByCreatedDateTimeDesc(pageObj, ipRating);

        List<Book> books = getBooksWithRequiredImages(page);
        model.addAttribute("books", books.stream().toList());
        model.addAttribute("rating", rating);

        return "components/swiper :: cloudy-swiper";
    }

    @GetMapping(value = "/recent")
    public String recentlyReviewed(Model model, Principal principal) {
        int currentPage = 0;
        PageRequest pageObj = PageRequest.of(currentPage, 7);
        Page<Book> page = bookRepository.findAllByOrderByCreatedDateTimeDesc(pageObj);
        model.addAttribute("pageOfBooks", page);
        return "recently-reviewed.html";
    }

    private static List<Book> getBooksWithRequiredImages(Page<Book> page) {
        return page.getContent().stream().filter(b ->
                        (b.getGoogleBookId() != null &&
                                !b.getGoogleBookId().isBlank() &&
                                (b.getGoogleBookDetails().getVolumeInfo().getImageLinks().getThumbnail() != null) &&
                                !b.getGoogleBookDetails().getVolumeInfo().getImageLinks().getThumbnail().isBlank()
                        )).toList();
    }
}
