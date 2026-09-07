package com.chathuninimesha.coreflow.model

typealias Mapper<Input, Output> = (Input) -> Output

sealed class WorkResult<T> {

    class LoadingResult<T> : WorkResult<T>()

    data class SuccessResult<T>(
        val data: T
    ) : WorkResult<T>()

    data class ErrorResult<T>(
        val error: DataError
    ) : WorkResult<T>()

    fun <R> map(mapper: Mapper<T, R>? = null): WorkResult<R> = when (this) {
        is LoadingResult -> LoadingResult()
        is ErrorResult -> ErrorResult(this.error)
        is SuccessResult -> {
            if (mapper == null) {
                throw IllegalStateException("Mapper should not be null for SuccessResult")
            }
            SuccessResult(mapper(this.data))
        }
    }
}
