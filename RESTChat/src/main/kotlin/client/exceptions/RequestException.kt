package client.exceptions

import client.core.HttpClient

class RequestException(method: String, val errorCode: Int): Exception() {
    val errorMessage: String

    init {
        val errMsg = when (errorCode) {
            HttpClient.ErrorType.NOT_SUPPORTED.code -> "Server is not supported request format of this application."
            HttpClient.ErrorType.NO_TOKEN.code -> "Server can't find authorization token."
            HttpClient.ErrorType.USERNAME_IS_BUSY.code -> "This username is busy."
            HttpClient.ErrorType.WRONG_TOKEN.code -> "Authentication error. Server can't recognize authentication token."
            HttpClient.ErrorType.NOT_FOUND.code -> "No user with such ID."
            HttpClient.ErrorType.WRONG_METHOD.code -> "Server is not supporting such action."
            HttpClient.ErrorType.OTHER.code -> "Unspecified server error."
            else -> "Unknown server status code."
        }
        errorMessage = "ERROR ($method): $errMsg"
    }
}