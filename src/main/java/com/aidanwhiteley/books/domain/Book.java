package com.aidanwhiteley.books.domain;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.io.Serializable;
import java.time.LocalDate;

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
    private String title;
    private String author;
    private String genre;
    private String summary;
    private Rating rating;
    private LocalDate lastRead;
    private String similarTo;

    public enum Rating {
        TERRIBLE(1),
        POOR(2),
        OK(3),
        GOOD(4),
        GREAT(5);

        @SuppressWarnings("unused")
		private final int ratingLevel;

        Rating(int ratingLevel) {
            this.ratingLevel = ratingLevel;
        }
    }
}
