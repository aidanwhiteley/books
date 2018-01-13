package com.aidanwhiteley.books.domain.googlebooks;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class BookSearchResult {

    private int totalItems;
    private List<Item> items = new ArrayList<>();
}
