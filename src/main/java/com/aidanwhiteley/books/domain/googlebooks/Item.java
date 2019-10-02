package com.aidanwhiteley.books.domain.googlebooks;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class Item implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String selfLink;
    private VolumeInfo volumeInfo;
    private AccessInfo accessInfo;
}
