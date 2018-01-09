// == Boolean expressions ==

sealed class BooleanExpr<T>() {
	operator fun unaryMinus() = NotExpr<T>(this)

	infix fun and(b : BooleanExpr<T>) = AndExpr<T>(this, b)

	infix fun or(b : BooleanExpr<T>) = OrExpr<T>(this, b)

	infix fun impl(b : BooleanExpr<T>) = ImplExpr<T>(this, b)

	infix fun equ(b : BooleanExpr<T>) = EquExpr<T>(this, b)
}

data class Atom<T>(val a : T) : BooleanExpr<T>() {
	override fun toString() : String = a.toString()
}

data class NotExpr<T>(val a : BooleanExpr<T>) : BooleanExpr<T>(){
	override fun toString() : String = "¬" + a
}

data class AndExpr<T>(val a : BooleanExpr<T>, val b : BooleanExpr<T>) : BooleanExpr<T>(){
	override fun toString() : String = "(" + a + " \u2227 " + b + ")"
}

data class OrExpr<T>(val a : BooleanExpr<T>, val b : BooleanExpr<T>) : BooleanExpr<T>(){
	override fun toString() : String = "(" + a + " \u2228 " + b + ")"
}

data class ImplExpr<T>(val a : BooleanExpr<T>, val b : BooleanExpr<T>) : BooleanExpr<T>(){
	override fun toString() : String = "(" + a + " \u27f6 " + b + ")"
}

data class EquExpr<T>(val a : BooleanExpr<T>, val b : BooleanExpr<T>) : BooleanExpr<T>(){
	override fun toString() : String = "(" + a + " \u27f7 " + b + ")"
}

fun main(args : Array<String>) {
	val expr = -Atom("a") and Atom("b") impl -Atom("c")
	println(expr)
}
