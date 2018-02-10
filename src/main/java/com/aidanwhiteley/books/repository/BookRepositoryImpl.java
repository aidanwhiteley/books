package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.domain.Book;
import com.aidanwhiteley.books.domain.Comment;
import com.aidanwhiteley.books.repository.dtos.BooksByAuthor;
import com.aidanwhiteley.books.repository.dtos.BooksByGenre;
import com.aidanwhiteley.books.repository.dtos.BooksByRating;
import com.aidanwhiteley.books.repository.dtos.BooksByReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Repository
public class BookRepositoryImpl implements BookRepositoryCustomMethods {

    @Autowired
    MongoTemplate mongoTemplate;

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

        //Convert the aggregation result into a List
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

        //Convert the aggregation result into a List
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

        //Convert the aggregation result into a List
        AggregationResults<BooksByReader> groupResults
                = mongoTemplate.aggregate(agg, Book.class, BooksByReader.class);
        return groupResults.getMappedResults();
    }

    @Override
    public void addCommentToBook(String bookId, Comment comment) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(bookId)),
                new Update().push("comments", comment), Book.class);
    }

    @Override
    public Book findCommentsForBook(String bookId) {
        Query query = new Query(Criteria.where("id").is(bookId));
        query.fields().include("_id").include("comments");

        return mongoTemplate.find(query, Book.class).get(0);
    }
}
