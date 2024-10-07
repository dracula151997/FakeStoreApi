package com.dracula.fakestoreapi.feature_login.domain

interface LoginRepository {
	suspend fun login(username: String, password: String): Login
}