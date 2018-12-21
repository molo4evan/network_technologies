package server.errorhandling.exceptions

import server.errorhandling.ErrorHandler

class NoTokenException: ServerException(ErrorHandler.ErrorType.NO_TOKEN)