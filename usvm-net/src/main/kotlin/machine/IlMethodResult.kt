package org.usvm.machine

sealed interface IlMethodResult {
    object NoCall : IlMethodResult
}
