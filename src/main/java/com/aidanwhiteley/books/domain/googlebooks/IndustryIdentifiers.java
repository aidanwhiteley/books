package com.aidanwhiteley.books.domain.googlebooks;

import lombok.AllArgsConstructor;
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
public class IndustryIdentifiers {

    public static final String TYPE_ISBN_10 = "ISBN_10";
    public static final String TYPE_ISBN_13 = "ISBN_13";

    private String type;
    private String identifier;
}
