package com.aidanwhiteley.books.domain.googlebooks;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BookSearchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalItems;
    private List<Item> items = new ArrayList<>();
}
