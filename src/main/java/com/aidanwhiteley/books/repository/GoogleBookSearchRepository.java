package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.controller.dtos.GoogleBookSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GoogleBookSearchRepository extends MongoRepository<GoogleBookSearch, String> {

    Page<GoogleBookSearch> findAllByTitleAuthor(Pageable page, String title, String author);
}
