package com.aidanwhiteley.books.domain;

import com.aidanwhiteley.books.domain.googlebooks.Item;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.SafeHtml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.aidanwhiteley.books.domain.User.Role.*;
import static org.hibernate.validator.constraints.SafeHtml.WhiteListType.NONE;

@SuppressWarnings("DefaultAnnotationParam")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
@Document
public class Book extends Auditable implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Book.class);

    @Id
    @Setter(AccessLevel.PROTECTED)
    private String id;

    // Setter added for Mongo serialisation problems with Spring Boot 2.1.0
    @Setter
    private List<Comment> comments = new ArrayList<>();

    @NotNull
    @Length(min = 1, max = 100)
    @Setter
    @SafeHtml(whitelistType = NONE)       // Output encoding is the primary XSS defence but we still block HTML input
    private String title;

    @NotNull
    @Length(min = 1, max = 75)
    @SafeHtml(whitelistType = NONE)
    private String author;

    @NotNull
    @Length(min = 1, max = 35)
    @Setter
    @SafeHtml(whitelistType = NONE)
    private String genre;

    @NotNull
    @Length(min = 1, max = 20000)
    @SafeHtml(whitelistType = NONE)
    private String summary;

    @NotNull
    private Rating rating;

    private String googleBookId;

    @Setter
    private Item googleBookDetails;

    // The following three transient fields are intended as "helpers" to enable
    // the client side to create links to functionality that will pass the
    // server side method level security.
    @Transient
    @Setter(AccessLevel.NONE)
    private boolean allowUpdate;

    @Transient
    @Setter(AccessLevel.NONE)
    private boolean allowDelete;

    @Transient
    @Setter(AccessLevel.NONE)
    private boolean allowComment;

    @Builder
    // See https://reinhard.codes/2015/09/16/lomboks-builder-annotation-and-inheritance/ for why this seems necessary
    private Book(Owner createdBy, LocalDateTime createdDateTime, Owner lastModifiedBy, LocalDateTime lastModifiedDateTime,
                 String id, @NotNull String title, @NotNull String author, @NotNull String genre, @NotNull String summary, @NotNull Rating rating, String googleBookId, Item googleBookDetails) {
        super(createdBy, createdDateTime, lastModifiedBy, lastModifiedDateTime);
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.summary = summary;
        this.rating = rating;
        this.googleBookId = googleBookId;
        this.googleBookDetails = googleBookDetails;
    }

    public void setPermissionsAndContentForUser(User user) {
        this.allowComment = false;
        this.allowDelete = false;
        this.allowUpdate = false;

        // noinspection StatementWithEmptyBody
        if (null == user || user.getHighestRole() == ROLE_USER) {
            // No permissions
        } else if (user.getHighestRole() == ROLE_EDITOR) {
            this.allowComment = true;
            if (isOwner(user)) {
                this.allowDelete = true;
                this.allowUpdate = true;
            }
        } else if (user.getHighestRole() == ROLE_ADMIN) {
            this.allowComment = true;
            this.allowDelete = true;
            this.allowUpdate = true;
        } else {
            LOGGER.error("Unexpected user role found. This is a code logic error");
            throw new IllegalStateException("Unable to set content in a Book appropriate for user");
        }

        this.comments.forEach(c -> c.setPermissionsAndContentForUser(user));

        if (this.getCreatedBy() != null) {
            this.getCreatedBy().setPermissionsAndContentForUser(user);
        }
    }

    public boolean isOwner(User user) {
        // noinspection SimplifiableIfStatement
        if (user == null || this.getCreatedBy() == null) {
            return false;
        } else {
            return (user.getAuthenticationServiceId().equals(this.getCreatedBy().getAuthenticationServiceId())
                    && user.getAuthProvider() == this.getCreatedBy().getAuthProvider());
        }
    }

    public enum Rating {
        // Note Jackson default deserialisation is 0 based - changing values
        // below would mean that that default serialisation / deserialisation
        // would
        // need overriding.
        TERRIBLE(0), POOR(1), OK(2), GOOD(3), GREAT(4);

        private final int ratingLevel;

        Rating(int ratingLevel) {
            this.ratingLevel = ratingLevel;
        }

        public static Rating getRatingByString(String aRating) {
            for (Rating rating : Rating.values()) {
                if (rating.name().equalsIgnoreCase(aRating)) {
                    return rating;
                }
            }
            return null;
        }

        public int getRatingLevel() {
            return this.ratingLevel;
        }
    }
}