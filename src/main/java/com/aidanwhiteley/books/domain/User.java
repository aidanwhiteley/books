package com.aidanwhiteley.books.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<Role> roles = new ArrayList<>();

    @Id
    @Setter(AccessLevel.PROTECTED)
    private String id;

    @NotNull
    @Length(min = 1, max = 255)
    private String authenticationServiceId;

    @Length(max = 100)
    private String firstName;

    @Length(max = 100)
    private String lastName;

    @Length(max = 200)
    private String fullName;
    
    private String email;

    @URL
    private String link;

    @Length(max = 500)
    private String picture;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @NotNull
    private LocalDateTime lastLogon;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @NotNull
    private LocalDateTime firstLogon;

    @NotNull
    private AuthenticationProvider authProvider;

    @Builder.Default
    private boolean adminEmailedAboutSignup = false;

    public boolean isFirstVisit() {
        return (firstLogon.truncatedTo(ChronoUnit.MINUTES).equals(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)));
    }

    public Role getHighestRole() {
        User.Role highestRole = User.Role.ROLE_USER;

        for (User.Role role : getRoles()) {
            if (role.getRoleNumber() > highestRole.getRoleNumber()) {
                highestRole = role;
            }
        }

        return highestRole;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
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

        private static final Map<Integer, Role> map = new HashMap<>();

        static {
            for (Role aRole : Role.values()) {
                map.put(aRole.getRoleNumber(), aRole);
            }
        }

        public static Role getRole(int role) {
            return map.get(role);
        }

    }

    public enum AuthenticationProvider {
        GOOGLE(0),
        FACEBOOK(1);

        private final int provider;

        AuthenticationProvider(int provider) {
            this.provider = provider;
        }
        
        public int getAuthProvider() {
        	return this.provider;
        }
    }
}
