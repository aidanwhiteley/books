package com.aidanwhiteley.books.service.dtos;

import com.aidanwhiteley.books.domain.googlebooks.Item;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleBookSearchResult {

    private Item item;
    private int index;
    private boolean hasMore;
    private boolean hasPrevious;
}
