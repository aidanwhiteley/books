package com.aidanwhiteley.books.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import com.aidanwhiteley.books.domain.User;

public interface UserRepository extends MongoRepository<User, String> {

    List<User> findAllByAuthenticationServiceIdAndAuthProvider(@Param("authenticationServiceId") String authenticationServiceId,
                                                                @Param("authenticationProvider") String authenticationProvider);
}
