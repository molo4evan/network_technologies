package server.errorhandling.exceptions

import server.errorhandling.ErrorHandler

class WrongTokenException: ServerException(ErrorHandler.ErrorType.WRONG_TOKEN)