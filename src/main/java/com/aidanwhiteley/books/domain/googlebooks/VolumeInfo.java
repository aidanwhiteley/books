package com.aidanwhiteley.books.domain.googlebooks;

import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("WeakerAccess")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class VolumeInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String title;
    private String subtitle;
    private List<String> authors = new ArrayList<>();
    private String description;
    private ImageLinks imageLinks;
    private String previewLink;
}
