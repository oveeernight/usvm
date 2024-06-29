package org.usvm.programs

import org.usvm.language.*
import org.usvm.language.builders.*

object StructProgram : ProgramDecl() {
    object S : StructDecl() {
        val a by IntType
    }

    val checkRefEquality by method(S.type, S.type, returnType = BooleanType) { a, b ->
        branch(a eq b) {
            ret(false.expr)
        }
        ret(true.expr)
    }

    val checkImplicitRefEquality by method(S.type, S.type, returnType = IntType) { a, b ->
        branch(a.isNull or b.isNull) {
            ret(0.expr)
        }
        branch(a[S.a] eq 1.expr) {
            ret(1.expr)
        }
        b[S.a] = 1.expr
        branch(a[S.a] eq 1.expr) {
            ret(2.expr)
        }
        ret(3.expr)
    }

    object  T : StructDecl() {
        val f by S.type
    }

    val checkHeapRefSplitting by method(T.type, T.type, returnType = IntType) { a, b ->
        val n by 5000.expr
        val concrete = StructCreation(Struct("c", emptySet()), emptyList())
        val array by ArrayType(T.type)(size = 1.expr)
        var idx by 0.expr
        loop(idx lt n) {
            a[T.f] = concrete
            val read1 = b[T.f]
            branch((read1 eq b[T.f]).not()) {
                ret((-1).expr)
            }
            b[T.f] = b
            val read2 = a[T.f]
            branch((read2 eq a[T.f]).not()) {
                ret((-2).expr)
            }
            idx += 1.expr
        }
        branch(a[T.f] eq b) {
            ret(2.expr)
        }
        ret(3.expr)
    }
}
