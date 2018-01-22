package com.aidanwhiteley.books.repository;

import com.aidanwhiteley.books.controller.dtos.ClientRoles;

public interface UserRepositoryCustomMethods {
	
	int updateUserRoles(ClientRoles clientRoles);

}
