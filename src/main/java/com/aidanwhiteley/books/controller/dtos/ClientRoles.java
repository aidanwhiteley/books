package com.aidanwhiteley.books.controller.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class ClientRoles {

    private String id;
    private boolean admin;
    private boolean editor;
}
