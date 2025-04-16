package org.usvm.machine.interpreter

import org.jacodb.api.net.ilinstances.IlMethod
import org.jacodb.api.net.ilinstances.IlType
import org.usvm.types.TypesResult
import org.usvm.types.UTypeStream


interface IlTypeSelector {
    fun choose(typeStream: UTypeStream<out IlType>): Collection<IlType>
}

class IlFixedInheritorsNumberTypeSelector(
    private val inheritorsNumberToChoose: Int = DEFAULT_INHERITORS_NUMBER_TO_CHOOSE,
    inheritorsNumberToSelectFrom: Int = DEFAULT_INHERITORS_NUMBER_TO_SCORE
): IlTypeSelector {
    private val typesPrioritization = IlTypeStreamPrioritization(inheritorsNumberToChoose)
    override fun choose(typeStream: UTypeStream<out IlType>): Collection<IlType> =
        typesPrioritization.take(typeStream, inheritorsNumberToChoose)
    companion object {
        const val DEFAULT_INHERITORS_NUMBER_TO_CHOOSE: Int = 4
        const val DEFAULT_INHERITORS_NUMBER_TO_SCORE: Int = 100
    }
}

class IlTypeStreamPrioritization(private val typesToScore: Int) {
    fun take(typeStream: UTypeStream<out IlType>, limit: Int): Collection<IlType> =
        fetchTypes(typeStream)
            .sortedByDescending { it.score() }
            .take(limit)

    private fun fetchTypes(typeStream: UTypeStream<out IlType>) : Collection<IlType> =
        typeStream.take(typesToScore)
            .let {
                when (it) {
                    TypesResult.EmptyTypesResult -> emptyList()
                    is TypesResult.SuccessfulTypesResult -> it.types
                    is TypesResult.TypesResultWithExpiredTimeout -> it.collectedTypes
                }
            }

    private fun IlType.score() : Double {
        var score = 0.0
        return score
    }
}
