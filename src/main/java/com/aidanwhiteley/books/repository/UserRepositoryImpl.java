package com.aidanwhiteley.books.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;
import com.aidanwhiteley.books.domain.User;
import com.mongodb.client.result.UpdateResult;

@Repository
public class UserRepositoryImpl implements UserRepositoryCustomMethods {

	private final MongoTemplate mongoTemplate;

	@Autowired
	public UserRepositoryImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public long updateUserRoles(ClientRoles clientRoles) {
		
		List<User.Role> roles = new ArrayList<>();
		if (clientRoles.isAdmin()) {
			roles.add(User.Role.ROLE_ADMIN);
		}
		if (clientRoles.isEditor()) {
			roles.add(User.Role.ROLE_EDITOR);
		}

		Query query = new Query(Criteria.where("id").is(clientRoles.getId()));
		Update update = new Update();
		update.set("roles", roles);

		UpdateResult result = mongoTemplate.updateFirst(query, update, User.class);

		//noinspection ConstantConditions
		if (result != null) {
			return result.getModifiedCount();
		} else {
			return 0;
		}
	}

}
