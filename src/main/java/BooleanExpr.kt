// == Boolean expressions ==

sealed class BooleanExpr<T>() {
	override fun toString() : String =
			when (this) {
				is Atom -> this.a.toString()
				is NotExpr -> "¬" + this.a
				is AndExpr -> "(" + this.a + "\u2227" + this.b + ")"
				is OrExpr -> "(" + this.a + "\u2228" + this.b + ")"
			}

	operator fun unaryMinus() = NotExpr<T>(this)

	infix fun and(b : BooleanExpr<T>) = AndExpr<T>(this, b)

	infix fun or(b : BooleanExpr<T>) = OrExpr<T>(this, b)
}

class Atom<T>(val a : T) : BooleanExpr<T>()

class NotExpr<T>(val a : BooleanExpr<T>) : BooleanExpr<T>()

class AndExpr<T>(val a : BooleanExpr<T>, val b : BooleanExpr<T>) : BooleanExpr<T>()

class OrExpr<T>(val a : BooleanExpr<T>, val b : BooleanExpr<T>) : BooleanExpr<T>()

// == Transform to CNF ==

typealias Disjunction<T> = ArrayList<BooleanExpr<T>>

typealias Conjunction<T> = ArrayList<Disjunction<T>>

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
		}
	}
}

// == Pretty-print CNF ==

fun <T> Conjunction<T>.prettyPrint() : String =
		this.joinToString(", ", "[", "]",
				transform = { c -> c.joinToString(", ", "[", "]") })

fun main(args : Array<String>) {
	val a = Atom("a")
	val b = Atom("b")
	val c = Atom("c")
	val d = Atom("d")
//	val expr = -(-a and b) or -(c and -d)
//	val expr = -(c and -d)
	val expr = (c or (a and b))
	println(expr)
	println()
	println(expr.toCNF().prettyPrint())
}
