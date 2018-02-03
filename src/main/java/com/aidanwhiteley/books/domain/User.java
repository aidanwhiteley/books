package com.aidanwhiteley.books.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.Id;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class User {

    @Id
    @Setter(AccessLevel.PROTECTED)
    private String id;

    @NotNull
    @Length(min = 1, max = 255)
    private String authenticationServiceId;

    private String firstName;

    private String lastName;

    private String fullName;

    private String email;

    private String link;

    private String picture;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @NotNull
    private LocalDateTime lastLogon;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @NotNull
    private LocalDateTime firstLogon;

    final private List<Role> roles = new ArrayList<>();

    private AuthenticationProvider authProvider;

    public boolean isFirstVisit() {
        return (firstLogon.equals(lastLogon));
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    // A user gets the ROLE_USER as soon as they log on via an authentication provider.
    // It does not mean they are "trusted" users of the application. That only happens
    // when an admin gives them the ROLE_EDITOR role.
    public enum Role {
        ROLE_USER(0),
        ROLE_EDITOR(1),
        ROLE_ADMIN(2);

        private final int role;
        Role(int role) {
            this.role = role;
        }

        public String getShortName() {
            return this.toString().split("ROLE_")[1];
        }
        public int getRoleNumber() {
            return this.role;
        }
    }

    public enum AuthenticationProvider {
        GOOGLE(0),
        FACEBOOK(1);

        private final int provider;
        AuthenticationProvider(int provider) {
            this.provider = provider;
        }
    }
}
