package com.aidanwhiteley.books.domain.googlebooks;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@SuppressWarnings("WeakerAccess")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AccessInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String viewability;
    private boolean embeddable;
    private boolean publicDomain;
}
