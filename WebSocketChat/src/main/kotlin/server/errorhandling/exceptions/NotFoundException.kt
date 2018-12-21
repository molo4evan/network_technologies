package server.errorhandling.exceptions

import server.errorhandling.ErrorHandler

class NotFoundException: ServerException(ErrorHandler.ErrorType.NOT_FOUND)