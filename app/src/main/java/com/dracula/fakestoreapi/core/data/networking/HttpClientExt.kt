package com.dracula.fakestoreapi.core.data.networking

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import io.ktor.utils.io.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Extension function for [HttpClient] to perform a GET request with optional query parameters.
 *
 * @param route The endpoint route for the GET request.
 * @param queryParameters A map of query parameters to include in the request. Defaults to an empty map.
 * @return A [Result] object containing either the successful response body or a [DataError.Network] error.
 * @throws UnresolvedAddressException if the address cannot be resolved.
 * @throws SerializationException if there is an error during serialization.
 * @throws CancellationException if the coroutine is cancelled.
 */
suspend inline fun <reified Response : Any> HttpClient.get(
	route: String,
	queryParameters: Map<String, Any?> = emptyMap(),
): Result<Response, DataError.Network> {
	return safeCall {
		get {
			url(constructRoute(route = route))
			queryParameters.forEach { (key, value) ->
				if (value != null) {
					parameter(key, value.toString())
				}
			}
		}
	}
}

/**
 * Executes a network call safely, catching and handling various exceptions.
 *
 * @param execute A lambda function that performs the network call and returns an [HttpResponse].
 * @return A [Result] object containing either the successful response body or a [DataError.Network] error.
 * @throws CancellationException if the coroutine is cancelled.
 */
suspend inline fun <reified T> safeCall(
	execute: () -> HttpResponse,
): Result<T, DataError.Network> {
	val response = try {
		execute()
	} catch (e: UnresolvedAddressException) {
		e.printStackTrace()
		return Result.Error(DataError.Network.NO_INTERNET)
	} catch (e: SerializationException) {
		e.printStackTrace()
		return Result.Error(DataError.Network.SERIALIZATION_ERROR)
	} catch (e: Exception) {
		if (e is CancellationException) throw e
		e.printStackTrace()
		return Result.Error(DataError.Network.UNKNOWN)
	}
	return responseToResult(response = response)
}

/**
 * Converts an [HttpResponse] to a [Result] object, handling various HTTP status codes.
 *
 * @param response The [HttpResponse] to convert.
 * @return A [Result] object containing either the successful response body or a [DataError.Network] error.
 * @throws SerializationException if there is an error during serialization.
 */
suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError.Network> {
    return when (response.status.value) {
        in 200..299 -> {
            val data = response.body<T>()
            Result.Success(data)
        }
        401 -> {
            Result.Error(DataError.Network.UNAUTHORIZED)
        }
        408 -> Result.Error(DataError.Network.REQUEST_TIMEOUT)
        409 -> Result.Error(DataError.Network.CONFLICT)
        413 -> Result.Error(DataError.Network.PAYLOAD_TOO_LARGE)
        429 -> Result.Error(DataError.Network.TOO_MANY_REQUESTS)
        in 500..599 -> Result.Error(DataError.Network.SERVER_ERROR)
        else -> Result.Error(DataError.Network.UNKNOWN)
    }
}

/**
 * Constructs a complete route URL based on the given route string.
 *
 * @param route The endpoint route to be constructed.
 * @return A complete URL string based on the provided route.
 */
fun constructRoute(route: String): String {
    return when {
        route.contains(BuildConfig.BASE_URL) -> route
        route.startsWith("/") -> BuildConfig.BASE_URL + route
        else -> BuildConfig.BASE_URL + "/" + route
    }
}