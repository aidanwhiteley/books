package com.aidanwhiteley.books.domain.googlebooks;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;

@SuppressWarnings("WeakerAccess")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AccessInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String viewability;
    private boolean embeddable;
    private boolean publicDomain;
}
