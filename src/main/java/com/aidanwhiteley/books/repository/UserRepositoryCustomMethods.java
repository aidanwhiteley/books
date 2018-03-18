package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;
import com.aidanwhiteley.books.domain.User;

public interface UserRepositoryCustomMethods {
	
	long updateUserRoles(ClientRoles clientRoles);

	void updateUserAdminNotified(User user);
}
