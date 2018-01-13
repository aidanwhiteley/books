package com.aidanwhiteley.books.domain.googlebooks;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class AccessInfo {

    private String viewability;
    private boolean embeddable;
    private boolean publicDomain;
}
