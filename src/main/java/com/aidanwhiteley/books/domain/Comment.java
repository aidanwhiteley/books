package com.aidanwhiteley.books.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@EqualsAndHashCode
@ToString
public class Comment {

    // Mandatory but not marked as @NotNull as set by controller
    private Owner owner;

    @Id
    private String id = UUID.randomUUID().toString();

    @NotNull
    @Length(min = 1, max = 1000)
    private String comment;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @NotNull
    private LocalDateTime entered = LocalDateTime.now();

    private boolean deleted = false;

    private String deletedBy;

    public Comment() {
    }

    public Comment(String comment, Owner owner) {
        this.comment = comment;
        this.owner = owner;
    }

    public boolean isOwner(User user) {
        return (user.getAuthenticationServiceId().equals(this.owner.getAuthenticationServiceId()) && user.getAuthProvider() == this.owner.getAuthProvider()
        );
    }
}
