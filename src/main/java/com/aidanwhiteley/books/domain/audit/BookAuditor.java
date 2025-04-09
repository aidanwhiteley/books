package com.aidanwhiteley.books.domain.audit;

import com.aidanwhiteley.books.domain.Owner;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.repository.UserRepository;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

@Component
public class BookAuditor implements AuditorAware<Owner> {

    private final JwtAuthenticationUtils jwtAuthenticationUtils;

    private final UserRepository userRepository;

    public BookAuditor(JwtAuthenticationUtils jwtAuthenticationUtils, UserRepository userRepository) {
        this.jwtAuthenticationUtils = jwtAuthenticationUtils;
        this.userRepository = userRepository;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Optional<Owner> getCurrentAuditor() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            Optional<User> aUser = jwtAuthenticationUtils.
                    extractUserFromPrincipal((Principal) (authentication.getPrincipal()), true);

            return aUser.map(s -> new Owner(
                    userRepository.findAllByAuthenticationServiceIdAndAuthProvider(s.getAuthenticationServiceId(), s.getAuthProvider().toString()).getFirst())
            );
        } else {
            return Optional.empty();
        }
    }

}
