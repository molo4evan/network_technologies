package server.errorhandling.exceptions

import server.errorhandling.ErrorHandler

open class ServerException(val error: ErrorHandler.ErrorType = ErrorHandler.ErrorType.OTHER): Exception()