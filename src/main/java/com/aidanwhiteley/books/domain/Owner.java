package com.aidanwhiteley.books.domain;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
/**
 * A cut down copy of the User object that can be embedded in Book and Comment objects to nest
 * basic details about who created the Book or Comment.
 *
 * Some of the data in instances of this object will go stale over time. That's understood - it
 * is being used to record details of the user at the point in time Book or Comment is created - a
 * trade off between accuracy at all times (and disk space) and performance.
 */
public class Owner {

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


}
