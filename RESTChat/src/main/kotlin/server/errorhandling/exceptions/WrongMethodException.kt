package server.errorhandling.exceptions

import server.errorhandling.ErrorHandler

class WrongMethodException: ServerException(ErrorHandler.ErrorType.WRONG_METHOD)