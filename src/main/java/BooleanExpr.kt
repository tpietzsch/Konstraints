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
	override fun toString() : String = "(" + a + "\u2227" + b + ")"
}

data class OrExpr<T>(val a : BooleanExpr<T>, val b : BooleanExpr<T>) : BooleanExpr<T>(){
	override fun toString() : String = "(" + a + "\u2228" + b + ")"
}

data class ImplExpr<T>(val a : BooleanExpr<T>, val b : BooleanExpr<T>) : BooleanExpr<T>(){
	override fun toString() : String = "(" + a + "\u27f6" + b + ")"
}

data class EquExpr<T>(val a : BooleanExpr<T>, val b : BooleanExpr<T>) : BooleanExpr<T>(){
	override fun toString() : String = "(" + a + "\u27f7" + b + ")"
}

// == Transform to CNF ==

typealias Disjunction<T> = HashSet<BooleanExpr<T>>

typealias Conjunction<T> = HashSet<Disjunction<T>>

fun BooleanExpr<*>.isLiteral() =
		when (this) {
			is Atom -> true
			is NotExpr -> this.a is Atom
			else -> false
		}

fun <T> Disjunction<T>.isClause() : Boolean = this.all { it.isLiteral() }

fun <T> Disjunction<T>.popNonLiteral() : BooleanExpr<T>? {
	val expr = this.find { !it.isLiteral() }
	if (expr != null) this.remove(expr)
	return expr;
}

fun <T> Conjunction<T>.popNonClause() : Disjunction<T>? {
	val disj = this.find { !it.isClause() }
	if (disj != null) this.remove(disj)
	return disj
}

fun <T> BooleanExpr<T>.toCNF() : Conjunction<T> {
	val conj = Conjunction(listOf(Disjunction(listOf(this))))
	while (true) {
		var disj = conj.popNonClause()
		if (disj == null)
			return conj;
		var expr : BooleanExpr<T> = disj.popNonLiteral()!!;
		when (expr) {
			is Atom -> throw IllegalStateException() // cannot happen, expr would be a literal
			is NotExpr -> {
				val a = expr.a;
				when (a) {
					is Atom -> throw IllegalStateException() // cannot happen, expr would be a literal
					is NotExpr -> { // double negation
						disj.add(a.a)
						conj.add(disj)
					}
					is AndExpr -> { // de Morgan
						disj.add(-a.a)
						disj.add(-a.b)
						conj.add(disj)
					}
					is OrExpr -> { // de Morgan
						val d1 = Disjunction(disj)
						val d2 = Disjunction(disj)
						d1.add(-a.a)
						d2.add(-a.b)
						conj.add(d1)
						conj.add(d2)
					}
				}
			}
			is AndExpr -> {
				val d1 = Disjunction(disj)
				val d2 = Disjunction(disj)
				d1.add(expr.a)
				d2.add(expr.b)
				conj.add(d1)
				conj.add(d2)
			}
			is OrExpr -> {
				disj.add(expr.a)
				disj.add(expr.b)
				conj.add(disj)
			}
			is ImplExpr -> {
				disj.add(-expr.a or expr.b)
				conj.add(disj)
			}
			is EquExpr -> {
				disj.add((expr.a impl expr.b) and (expr.b impl expr.a))
				conj.add(disj)
			}
		}
	}
}

// == Pretty-print CNF ==

fun <T> Conjunction<T>.prettyPrint() : String =
		this.joinToString(", ", "[", "]",
				transform = { c -> c.joinToString(", ", "[", "]") })
