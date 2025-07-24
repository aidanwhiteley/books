package com.aidanwhiteley.books.domain.googlebooks;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class VolumeInfo {

    private String title;
    private String subtitle;
    private List<String> authors = new ArrayList<>();
    private String description;
    private List<IndustryIdentifiers> industryIdentifiers = new ArrayList<>();
    private ImageLinks imageLinks;
    private String previewLink;
}
