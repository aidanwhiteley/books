package com.aidanwhiteley.books.domain.googlebooks;

import lombok.*;

import java.io.Serializable;

@SuppressWarnings("WeakerAccess")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ImageLinks implements Serializable {

    private static final long serialVersionUID = 1L;

    private String smallThumbnail;
    private String thumbnail;
    private String small;
    private String medium;
    private String large;
    private String extraLarge;
}
