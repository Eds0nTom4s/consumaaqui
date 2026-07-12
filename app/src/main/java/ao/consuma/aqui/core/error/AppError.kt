package ao.consuma.aqui.core.error

sealed interface AppError {
    data object Unknown : AppError
    data object Network : AppError
    data object Validation : AppError
    data object Unauthorized : AppError
    data object NotFound : AppError
    data object Conflict : AppError
    data object RateLimited : AppError
    data object Server : AppError
}
