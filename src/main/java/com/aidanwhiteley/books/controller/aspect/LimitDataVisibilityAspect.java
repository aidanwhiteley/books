package com.aidanwhiteley.books.controller.aspect;

import java.security.Principal;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;

/**
 * This aspect is responsible for limiting the data returned to a caller based who the caller is.
 *
 * It does this by advising methods in classes with the @LimitDataVisibility annotation that
 * return either a Book or Page<Book>.
 *
 * When it finds such methods running it changes the returned data by calling the setPermissionsAndContentForUser
 * method on the Book.
 *
 * To be able to give users with higher levels of access (or those that created a Book or Comment) more data
 * in the returned Book, the code needs to see a Principal parameter in the called method. This code
 * should "fail safe" in that if a new method is added and a Principal parameter is not part of the
 * method signature, then we assume the method is being called by an anonymous user and all
 * restrictions on the returned data are applied.
 */
@Aspect
@Component
public class LimitDataVisibilityAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(LimitDataVisibilityAspect.class);

    @Autowired
    private JwtAuthenticationUtils authUtils;

    @Pointcut("@within(com.aidanwhiteley.books.controller.aspect.LimitDataVisibility)")
    public void isAnnotated() {
    }

    @Pointcut("execution(public com.aidanwhiteley.books.domain.Book com.aidanwhiteley.books..*.*(..))")
    public void returnsBook() {
    }

    @Pointcut("execution(public org.springframework.data.domain.Page<com.aidanwhiteley.books.domain.Book> com.aidanwhiteley.books..*.*(..))")
    public void returnsPageOfBooks() {
    }

    @Pointcut("isAnnotated() && returnsBook()")
    public void limitBookData() {
    }

    @Pointcut("isAnnotated() && returnsPageOfBooks()")
    public void limitPageBookData() {
    }

    @Around("limitBookData()")
    public Object limitBookDataImpl(ProceedingJoinPoint joinPoint) throws Throwable {

        Object retVal = joinPoint.proceed();

        Principal principal = getPrincipal(joinPoint);
        Optional<User> user = authUtils.extractUserFromPrincipal(principal);

        if (retVal instanceof Book) {
            LOGGER.info("About to call setPermissionsAndContentForUser for " + joinPoint.getSignature().toString());
            ((Book) retVal).setPermissionsAndContentForUser(user.isPresent() ? user.get() : null);
        } else {
            LOGGER.error("Unexpected return type found by aspect");
        }

        return retVal;
    }

    @SuppressWarnings("unchecked")
	@Around("limitPageBookData()")
    public Object limitPageOfBookDataImpl(ProceedingJoinPoint joinPoint) throws Throwable {

        Object retVal = joinPoint.proceed();

        Principal principal = getPrincipal(joinPoint);
        Optional<User> user = authUtils.extractUserFromPrincipal(principal);

        if (retVal instanceof Page) {
            LOGGER.info("About to call setPermissionsAndContentForUser for " + joinPoint.getSignature().toString());
            User theUser = user.isPresent() ? user.get() : null;
            ((Page<Book>) retVal).getContent().forEach(s -> ((Book) s).setPermissionsAndContentForUser(theUser));
        } else {
            LOGGER.error("Unexpected return type found by aspect");
        }

        return retVal;
    }

    private Principal getPrincipal(ProceedingJoinPoint joinPoint) {

        Principal principal = null;
        Object[] args = joinPoint.getArgs();
        for (Object o : args) {
            if (o instanceof Principal) {
                LOGGER.info("Found Principal parameter for advised method of " + joinPoint.getSignature().toString());
                principal = (Principal) o;
            }
        }
        return principal;
    }

}
