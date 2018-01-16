package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {

    List<User> findAllByAuthenticationServiceIdAndAuthProvider(@Param("authenticationServiceId") String authenticationServiceId,
                                                                @Param("authenticationProvider") String authenticationProvider);
}
