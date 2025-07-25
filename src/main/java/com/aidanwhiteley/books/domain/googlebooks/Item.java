package com.aidanwhiteley.books.domain.googlebooks;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
