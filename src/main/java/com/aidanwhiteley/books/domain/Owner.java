package com.aidanwhiteley.books.domain;

import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import static com.aidanwhiteley.books.domain.User.Role.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
/*
 * A cut down copy of the User object that can be embedded in Book and Comment
 * objects to nest basic details about who created the Book or Comment.
 * 
 * Some of the data in instances of this object will go stale over time. That's
 * understood - it is being used to record details of the user at the point in
 * time Book or Comment is created - a trade off between accuracy at all times
 * (and disk space) and performance.
 */
public class Owner implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(Owner.class);

	private String authenticationServiceId;

	private String firstName;

	private String lastName;

	private String fullName;

	private String email;

	private String link;

	private String picture;

	private User.AuthenticationProvider authProvider;

	public Owner(User user) {
		this.authenticationServiceId = user.getAuthenticationServiceId();
		this.firstName = user.getFirstName();
		this.lastName = user.getLastName();
		this.fullName = user.getFullName();
		this.email = user.getEmail();
		this.link = user.getLink();
		this.picture = user.getPicture();
		this.authProvider = user.getAuthProvider();
	}

	public void setPermissionsAndContentForUser(User user) {

		if (null == user || user.getHighestRole() == ROLE_USER) {
			this.authenticationServiceId = "";
			this.firstName = "";
			this.lastName = "";
			this.fullName = "";
			this.email = "";
			this.link = "";
			this.picture = "";
			this.authProvider = null;
		} else if (user.getHighestRole() == ROLE_EDITOR) {
			this.authenticationServiceId = "";
			this.email = "";
			this.authProvider = null;
        } else //noinspection StatementWithEmptyBody
            if (user.getHighestRole() == ROLE_ADMIN) {
			// Return all
		} else {
			LOGGER.error("Unexpected user role found. This is a code logic error");
			throw new IllegalStateException("Unable to set owner data in a Book appropriate for user");
		}
	}
}
