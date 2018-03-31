package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.domain.googlebooks.Item;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByRating;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import com.aidanwhiteley.books.repository.exceptions.CommentsStorageException;
import com.mongodb.client.result.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class BookRepositoryImpl implements BookRepositoryCustomMethods {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookRepositoryImpl.class);

    private static final String COUNT_OF_BOOKS = "countOfBooks";
    private static final String AUTHOR = "author";
    private static final String READER = "reader";
    private static final String GENRE = "genre";
    private static final String RATING = "rating";
    private static final String COMMENTS = "comments";

    private final MongoTemplate mongoTemplate;

    @Autowired
    public BookRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<BooksByGenre> countBooksByGenre() {
        Aggregation agg = newAggregation(
                group(GENRE).count().as(COUNT_OF_BOOKS),
                project(COUNT_OF_BOOKS).and(GENRE).previousOperation(),
                sort(Sort.Direction.DESC, COUNT_OF_BOOKS));

        //Convert the aggregation result into a List
        AggregationResults<BooksByGenre> groupResults
                = mongoTemplate.aggregate(agg, Book.class, BooksByGenre.class);
        return groupResults.getMappedResults();
    }

    @Override
    public List<BooksByRating> countBooksByRating() {
        Aggregation agg = newAggregation(
                group(RATING).count().as(COUNT_OF_BOOKS),
                project(COUNT_OF_BOOKS).and(RATING).previousOperation(),
                sort(Sort.Direction.DESC, COUNT_OF_BOOKS));

        AggregationResults<BooksByRating> groupResults
                = mongoTemplate.aggregate(agg, Book.class, BooksByRating.class);
        return groupResults.getMappedResults();
    }

    @Override
    public List<BooksByAuthor> countBooksByAuthor() {
        Aggregation agg = newAggregation(
                group(AUTHOR).count().as(COUNT_OF_BOOKS),
                project(COUNT_OF_BOOKS).and(AUTHOR).previousOperation(),
                sort(Sort.Direction.ASC, AUTHOR));

        AggregationResults<BooksByAuthor> groupResults
                = mongoTemplate.aggregate(agg, Book.class, BooksByAuthor.class);
        return groupResults.getMappedResults();
    }

    @Override
    public List<BooksByReader> countBooksByReader() {
        Aggregation agg = newAggregation(
                group("createdBy.fullName").count().as(COUNT_OF_BOOKS),
                project(COUNT_OF_BOOKS).and(READER).previousOperation(),
                sort(Sort.Direction.ASC, READER));

        AggregationResults<BooksByReader> groupResults
                = mongoTemplate.aggregate(agg, Book.class, BooksByReader.class);
        return groupResults.getMappedResults();
    }

    @Override
    public Book findCommentsForBook(String bookId) {
        Query query = new Query(Criteria.where("id").is(bookId));
        query.fields().include("_id").include(COMMENTS);
        return mongoTemplate.find(query, Book.class).get(0);
    }

    @Override
    public Book addCommentToBook(String bookId, Comment comment) {
        UpdateResult updateResult = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(bookId)),
                new Update().push(COMMENTS, comment), Book.class);

        if (updateResult.getMatchedCount() != 1) {
            LOGGER.error("Failed to add a comment to bookId {}. UpdateResult: {} ", bookId, updateResult);
            throw new CommentsStorageException("Failed to add a comment");
        }
        return findCommentsForBook(bookId);
    }

    @Override
    public void addGoogleBookItemToBook(String bookId, Item item) {
        Query query = new Query(Criteria.where("id").is(bookId));
        Update update = new Update();
        update.set("googleBookDetails", item);

        UpdateResult result = mongoTemplate.updateFirst(query, update, Book.class);

        if (result.getModifiedCount() != 1) {
            LOGGER.error("Expected 1 update for googleBookDetails in a Book for bookId {} but saw {}",
                    bookId, result.getModifiedCount());
        }
    }

    @Override
    public Book removeCommentFromBook(String bookId, String commentId, String removerName) {

        UpdateResult updateResult = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(bookId).and("comments.id").is(commentId)),
                new Update().set("comments.$.commentText", "").set("comments.$.deleted", true).set("comments.$.deletedBy", removerName),
                Book.class);

        if (updateResult.getMatchedCount() != 1) {
            LOGGER.error("Failed to remove commentId {} from bookId {}. UpdateResult: {} ", commentId, bookId, updateResult);
            throw new CommentsStorageException("Failed to remove a comment");
        }
        return findCommentsForBook(bookId);
    }

    @Override
    public Page<Book> searchForBooks(String searchPhrase, Pageable pageable) {

        TextCriteria criteria = TextCriteria.forDefaultLanguage()
                .matching(searchPhrase);
        Query query = TextQuery.queryText(criteria)
                .sortByScore().with(pageable);

        List<Book> books = mongoTemplate.find(query, Book.class);

        return PageableExecutionUtils.getPage(
                books,
                pageable,
                () -> mongoTemplate.count(query, Book.class));
    }
}
