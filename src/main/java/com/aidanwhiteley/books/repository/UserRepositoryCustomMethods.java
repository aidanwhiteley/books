package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;

public interface UserRepositoryCustomMethods {
	
	long updateUserRoles(ClientRoles clientRoles);
}
