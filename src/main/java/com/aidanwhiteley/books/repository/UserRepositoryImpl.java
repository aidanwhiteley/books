package com.aidanwhiteley.books.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;
import com.aidanwhiteley.books.domain.User;
import com.mongodb.WriteResult;

public class UserRepositoryImpl implements UserRepositoryCustomMethods {

	@Autowired
	MongoTemplate mongoTemplate;

	@Override
	public int updateUserRoles(ClientRoles clientRoles) {
		
		List<User.Role> roles = new ArrayList<User.Role>();
		if (clientRoles.isAdmin()) {
			roles.add(User.Role.ROLE_ADMIN);
		}
		if (clientRoles.isEditor()) {
			roles.add(User.Role.ROLE_EDITOR);
		}

		Query query = new Query(Criteria.where("id").is(clientRoles.getId()));
		Update update = new Update();
		update.set("roles", roles);

		WriteResult result = mongoTemplate.updateFirst(query, update, User.class);

		if (result != null) {
			return result.getN();
		} else {
			return 0;
		}
	}

}
