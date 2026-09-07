package com.chathuninimesha.coreflow.model

sealed class DataError {
    object CorruptedData : DataError()
    object StorageFailure : DataError()
    object NotFound : DataError()
    object Validation : DataError()
}

sealed class StoreResult<out T> {
    data class Success<T>(val data: T) : StoreResult<T>()
    data class Failure(val error: DataError) : StoreResult<Nothing>()
}
