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
public class Item {

    private String id;
    private String selfLink;
    private VolumeInfo volumeInfo;
    private AccessInfo accessInfo;
}
