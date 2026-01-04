package com.aidanwhiteley.books.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
class Auditable {

    @CreatedBy
    private Owner createdBy;

    @CreatedDate
    private LocalDateTime createdDateTime;

    @LastModifiedBy
    private Owner lastModifiedBy;

    @LastModifiedDate
    private LocalDateTime lastModifiedDateTime;

}