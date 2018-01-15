package com.aidanwhiteley.books.domain;

import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Setter(AccessLevel.PROTECTED)
    private String id;

    @NotNull
    @Length(min = 1, max = 100)
    private String title;

    @NotNull
    @Length(min = 1, max = 75)
    private String author;

    @NotNull
    @Length(min = 1, max = 35)
    private String genre;

    @NotNull
    @Length(min = 1, max = 5000)
    private String summary;

    @NotNull
    private Rating rating;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @NotNull
    private LocalDateTime entered;

    @Length(max = 200)
    private String similarTo;
    
    private String googleBookId;

    private Item googleBookDetails;

    private String createdBy;

    public enum Rating {
        // Note Jackson default deserialisation is 0 based - changing values below
        // would mean that that default serialisation / deserialisation would need overriding.
        TERRIBLE(0),
        POOR(1),
        OK(2),
        GOOD(3),
        GREAT(4);

        @SuppressWarnings("unused")
        private final int ratingLevel;

        Rating(int ratingLevel) {
            this.ratingLevel = ratingLevel;
        }
    }
}
