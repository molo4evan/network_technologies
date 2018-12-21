package server.errorhandling.exceptions

import server.errorhandling.ErrorHandler

class BusyUserException: ServerException(ErrorHandler.ErrorType.USERNAME_IS_BUSY)