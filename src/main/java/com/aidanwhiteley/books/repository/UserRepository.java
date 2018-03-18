package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserRepository extends MongoRepository<User, String>, UserRepositoryCustomMethods {

    List<User> findAllByAuthenticationServiceIdAndAuthProvider(String authenticationServiceId, String authenticationProvider);
    List<User> findAllByAdminEmailedAboutSignupIsFalse();
}
