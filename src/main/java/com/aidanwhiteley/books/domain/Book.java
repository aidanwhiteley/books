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
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder
public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    final private List<Comment> comments = new ArrayList<>();

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
    @Length(min = 1, max = 20000)
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
    // Not marked a @NotNull as validation is done on the
    // input object from the client and this data is 
    // set on the server side after validation.
    private Owner createdBy;

    /**
     * Remove data (particularly details of who created a review or a comment) from book data if the user isnt known.
     *
     * @param book
     * @return a Book with some PII data removed.
     */
    public static Book removeDataIfUnknownUser(Book book) {
        return new BookBuilder().
                id(book.id).
                googleBookDetails(book.googleBookDetails).
                googleBookId(book.googleBookId).
                author(book.author).
                entered(book.entered).
                genre(book.genre).
                rating(book.rating).
                summary(book.summary).
                title(book.title).
                build();
    }

    public static Book removeDataIfEditor(Book book) {

        return new BookBuilder().
                id(book.id).
                googleBookDetails(book.googleBookDetails).
                googleBookId(book.googleBookId).
                author(book.author).
                entered(book.entered).
                genre(book.genre).
                rating(book.rating).
                summary(book.summary).
                title(book.title).
                createdBy(Owner.getOwnerDataForEditorView(book.createdBy)).
                build();
    }

    public boolean isOwner(User user) {
        return (user.getAuthenticationServiceId().equals(this.createdBy.getAuthenticationServiceId()) && user.getAuthProvider() == this.getCreatedBy().getAuthProvider()
        );
    }

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

        public static Rating getRatingByString(String aRating) {
            for (Rating rating : Rating.values()) {
                if (rating.name().equalsIgnoreCase(aRating)) {
                    return rating;
                }
            }
            return null;
        }

        public int getRatingLevel() {
            return this.ratingLevel;
        }

    }
}
