package com.dracula.fakestoreapi.feature_login.data

import com.dracula.fakestoreapi.feature_login.domain.Login
import com.dracula.fakestoreapi.feature_login.domain.LoginRepository

class LoginRepositoryImpl : LoginRepository {
	override suspend fun login(username: String, password: String): Login {
		TODO("Not yet implemented")
	}
}