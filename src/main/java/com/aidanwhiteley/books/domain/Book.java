package com.aidanwhiteley.books.domain;

import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.aidanwhiteley.books.domain.User.Role.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Book.class);

    private final List<Comment> comments = new ArrayList<>();

    @Id
    @Setter(AccessLevel.PROTECTED)
    private String id;

    @NotNull
    @Length(min = 1, max = 100)
    @Setter
    private String title;

    @NotNull
    @Length(min = 1, max = 75)
    private String author;

    @NotNull
    @Length(min = 1, max = 35)
    @Setter
    private String genre;

    @NotNull
    @Length(min = 1, max = 20000)
    private String summary;

    @NotNull
    private Rating rating;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @NotNull
    private LocalDateTime entered;

    private String googleBookId;

    @Setter
    private Item googleBookDetails;

    // Not marked a @NotNull as validation is done on the
    // input object from the client and this data is
    // set on the server side after validation.
    @Setter
    private Owner createdBy;

    // The following three transient fields are intended as "helpers" to enable
    // the client side to create links to functionality that will pass the server side
    // method level security.
    @Transient
    @Setter(AccessLevel.NONE)
    private boolean allowUpdate;

    @Transient
    @Setter(AccessLevel.NONE)
    private boolean allowDelete;

    @Transient
    @Setter(AccessLevel.NONE)
    private boolean allowComment;

    public void setPermissionsAndContentForUser(User user) {
        this.allowComment = false;
        this.allowDelete = false;
        this.allowUpdate = false;

        //noinspection StatementWithEmptyBody
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

        if (this.createdBy != null) {
            this.createdBy.setPermissionsAndContentForUser(user);
        }
    }

    public boolean isOwner(User user) {
        //noinspection SimplifiableIfStatement
        if (user == null || this.createdBy == null) {
            return false;
        } else {
            return (user.getAuthenticationServiceId().equals(this.createdBy.getAuthenticationServiceId())
                    && user.getAuthProvider() == this.getCreatedBy().getAuthProvider());
        }
    }

    public enum Rating {
        // Note Jackson default deserialisation is 0 based - changing values
        // below would mean that that default serialisation / deserialisation would
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