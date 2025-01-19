package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.repository.dtos.GoogleBookSearch;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GoogleBookSearchRepository extends MongoRepository<GoogleBookSearch, String> {

    List<GoogleBookSearch> findAll();
    List<GoogleBookSearch> findAllByTitleAndAuthor(String title, String author);
}
