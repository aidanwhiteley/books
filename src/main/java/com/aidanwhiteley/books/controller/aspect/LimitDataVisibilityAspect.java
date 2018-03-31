package com.aidanwhiteley.books.controller.aspect;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.User;
import com.aidanwhiteley.books.util.JwtAuthenticationUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Optional;

/**
 * This aspect is responsible for limiting the data returned to a caller based
 * who the caller is.
 *
 * It does this by advising methods in classes with the @LimitDataVisibility
 * annotation that return either a Book or Page<Book>.
 *
 * When it finds such methods running it changes the returned data by calling
 * the setPermissionsAndContentForUser method on the Book.
 *
 * To be able to give users with higher levels of access (or those that created
 * a Book or Comment) more data in the returned Book, the code needs to see a
 * Principal parameter in the called method. This code should "fail safe" in
 * that if a new method is added and a Principal parameter is not part of the
 * method signature, then we assume the method is being called by an anonymous
 * user and all restrictions on the returned data are applied.
 *
 * TODO - this doesnt need to be an "around" advice - it could just be an
 * "after".
 */
@SuppressWarnings({"EmptyMethod", "unused"})
@Aspect
@Component
public class LimitDataVisibilityAspect {

	private static final Logger LOGGER = LoggerFactory.getLogger(LimitDataVisibilityAspect.class);

	private final JwtAuthenticationUtils authUtils;

	@Autowired
	public LimitDataVisibilityAspect(JwtAuthenticationUtils jwtAuthenticationUtils) {
		this.authUtils = jwtAuthenticationUtils;
	}

	@Pointcut("@within(com.aidanwhiteley.books.controller.aspect.LimitDataVisibility)")
	public void isAnnotated() {
		// Just used for point cut - no implementation
	}

	@Pointcut("execution(public com.aidanwhiteley.books.domain.Book com.aidanwhiteley.books..*.*(..))")
	public void returnsBook() {
		// Just used for point cut - no implementation
	}

	@Pointcut("execution(public org.springframework.data.domain.Page<com.aidanwhiteley.books.domain.Book> com.aidanwhiteley.books..*.*(..))")
	public void returnsPageOfBooks() {
		// Just used for point cut - no implementation
	}

	@Pointcut("isAnnotated() && returnsBook()")
	public void limitBookData() {
		// Just used for point cut - no implementation
	}

	@Pointcut("isAnnotated() && returnsPageOfBooks()")
	public void limitPageBookData() {
		// Just used for point cut - no implementation
	}

	@Around("limitBookData()")
	public Object limitBookDataImpl(ProceedingJoinPoint joinPoint) throws Throwable {

		Object retVal = joinPoint.proceed();

		Principal principal = getPrincipal(joinPoint);

		// Note - we only look at data from the JWT to build the User here - we
		// are
		// only interested in the users roles and they are in the JWT.
		Optional<User> user = authUtils.extractUserFromPrincipal(principal, true);

		if (retVal instanceof Book) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("About to call setPermissionsAndContentForUser for {}", joinPoint.getSignature());
			}
			((Book) retVal).setPermissionsAndContentForUser(user.orElse(null));
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
		Optional<User> user = authUtils.extractUserFromPrincipal(principal, true);

		if (retVal instanceof Page) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("About to call setPermissionsAndContentForUser for {}", joinPoint.getSignature());
			}
			User theUser = user.orElse(null);
			((Page<Book>) retVal).getContent().forEach(s -> s.setPermissionsAndContentForUser(theUser));
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
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("Found Principal parameter for advised method of {}", joinPoint.getSignature());
				}
				principal = (Principal) o;
			}
		}
		return principal;
	}

}
