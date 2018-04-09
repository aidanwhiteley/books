package com.aidanwhiteley.books.domain;

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
import java.util.UUID;

import static com.aidanwhiteley.books.domain.User.Role.*;

@Setter
@Getter
@EqualsAndHashCode
@ToString
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(Comment.class);

    // Mandatory but not marked as @NotNull as set by controller
    private Owner owner;

    @Id
    private String id = UUID.randomUUID().toString();

    @NotNull
    @Length(min = 1, max = 1000)
    private String commentText;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @NotNull
    private LocalDateTime entered = LocalDateTime.now();

    private boolean deleted = false;

    private String deletedBy;

    // The following  transient field is intended as a "helper" to enable the client
    // side to create links to functionality that will pass the server side method level security.
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
    
    public Comment(String commentText, Owner owner, LocalDateTime entered) {
        this.commentText = commentText;
        this.owner = owner;
        this.entered = entered;
    }

    public boolean isOwner(User user) {
        return (user.getAuthenticationServiceId().equals(this.owner.getAuthenticationServiceId()) &&
                user.getAuthProvider() == this.owner.getAuthProvider());
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
