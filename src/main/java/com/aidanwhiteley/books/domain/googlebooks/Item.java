package com.aidanwhiteley.books.domain.googlebooks;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Item {

    private String id;
    private String selfLink;
    private VolumeInfo volumeInfo;
    private AccessInfo accessInfo;
}
