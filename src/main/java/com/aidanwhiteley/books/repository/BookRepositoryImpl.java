package com.aidanwhiteley.books.repository;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.TextQuery;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByRating;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import com.mongodb.client.result.UpdateResult;

@Repository
public class BookRepositoryImpl implements BookRepositoryCustomMethods {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookRepositoryImpl.class);

    private final MongoTemplate mongoTemplate;

    @Autowired
    public BookRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<BooksByGenre> countBooksByGenre() {
        Aggregation agg = newAggregation(
                group("genre").count().as("countOfBooks"),
                project("countOfBooks").and("genre").previousOperation(),
                sort(Sort.Direction.DESC, "countOfBooks"));

        //Convert the aggregation result into a List
        AggregationResults<BooksByGenre> groupResults
                = mongoTemplate.aggregate(agg, Book.class, BooksByGenre.class);
        return groupResults.getMappedResults();
    }

    @Override
    public List<BooksByRating> countBooksByRating() {
        Aggregation agg = newAggregation(
                group("rating").count().as("countOfBooks"),
                project("countOfBooks").and("rating").previousOperation(),
                sort(Sort.Direction.DESC, "countOfBooks"));

        AggregationResults<BooksByRating> groupResults
                = mongoTemplate.aggregate(agg, Book.class, BooksByRating.class);
        return groupResults.getMappedResults();
    }

    @Override
    public List<BooksByAuthor> countBooksByAuthor() {
        Aggregation agg = newAggregation(
                group("author").count().as("countOfBooks"),
                project("countOfBooks").and("author").previousOperation(),
                sort(Sort.Direction.ASC, "author"));

        AggregationResults<BooksByAuthor> groupResults
                = mongoTemplate.aggregate(agg, Book.class, BooksByAuthor.class);
        return groupResults.getMappedResults();
    }

    @Override
    public List<BooksByReader> countBooksByReader() {
        Aggregation agg = newAggregation(
                group("createdBy.fullName").count().as("countOfBooks"),
                project("countOfBooks").and("reader").previousOperation(),
                sort(Sort.Direction.ASC, "reader"));

        AggregationResults<BooksByReader> groupResults
                = mongoTemplate.aggregate(agg, Book.class, BooksByReader.class);
        return groupResults.getMappedResults();
    }

    @Override
    public Book findCommentsForBook(String bookId) {
        Query query = new Query(Criteria.where("id").is(bookId));
        query.fields().include("_id").include("comments");
        return mongoTemplate.find(query, Book.class).get(0);
    }

    @Override
    public Book addCommentToBook(String bookId, Comment comment) {
        UpdateResult updateResult = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(bookId)),
                new Update().push("comments", comment), Book.class);

        if (updateResult.getMatchedCount() != 1) {
            LOGGER.error("Failed to add a comment to bookId {}. UpdateResult: {} ", bookId, updateResult);
            throw new RuntimeException("Failed to add a comment");
        }
        return findCommentsForBook(bookId);
    }

    @Override
    public Book removeCommentFromBook(String bookId, String commentId, String removerName) {

        UpdateResult updateResult = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(bookId).and("comments.id").is(commentId)),
                new Update().set("comments.$.comment", "").set("comments.$.deleted", true).set("comments.$.deletedBy", removerName),
                Book.class);

        if (updateResult.getMatchedCount() != 1) {
            LOGGER.error("Failed to remove commentId {} from bookId {}. UpdateResult: {} ", commentId, bookId, updateResult);
            throw new RuntimeException("Failed to remove a comment");
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
