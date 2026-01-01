package com.aidanwhiteley.books.domain;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.aidanwhiteley.books.domain.User.Role.*;

@Data
@Document
@EqualsAndHashCode(callSuper = false)
public class Comment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Comment.class);

    // Mandatory but not marked as @NotNull as set by controller
    private Owner owner;

    @Id
    private String id = UUID.randomUUID().toString();

    @Length(min = 1, max = 1000)
    private String commentText;

    @NotNull
    private LocalDateTime entered = LocalDateTime.now();

    private boolean deleted = false;

    private String deletedBy;

    // The following transient field is intended as a "helper" to enable the
    // client side to create links to functionality that will pass the server
    // side method level security.
    // As this is set via an AOP advice on the JSON APIs, it is not set / used for
    // the HTMX version of the UI.
    @Transient
    @Setter(AccessLevel.NONE)
    private boolean allowDelete;

    @SuppressWarnings("WeakerAccess")
    public Comment() {
    }

    public Comment(String commentText, Owner owner) {
        this.commentText = commentText;
        this.owner = owner;
    }

    public Comment(String commentText, Owner owner, @NotNull LocalDateTime entered) {
        this.commentText = commentText;
        this.owner = owner;
        this.entered = entered;
    }

    public boolean isOwner(User user) {
        return (user.getAuthenticationServiceId().equals(this.owner.getAuthenticationServiceId())
                && user.getAuthProvider() == this.owner.getAuthProvider());
    }

    public void setPermissionsAndContentForUser(User user) {
        this.allowDelete = false;

        if (null == user || user.getHighestRole() == ROLE_USER) {
            this.deletedBy = "";
        } else if (user.getHighestRole() == ROLE_EDITOR) {
            if (isOwner(user)) {
                this.allowDelete = true;
            }
        } else if (user.getHighestRole() == ROLE_ADMIN) {
            this.allowDelete = true;
        } else {
            LOGGER.error("Unexpected user role found. This is a code logic error");
            throw new IllegalStateException("Unable to set content in a Comment appropriate for user");
        }

        if (owner != null) {
            owner.setPermissionsAndContentForUser(user);
        }
    }
}
