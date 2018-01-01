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

// == generalized conjunction and disjunction of atoms ==

data class GenDisj<T>(val a : T, val setOfA : T) : BooleanExpr<T>() {
	override fun toString() : String = "{\u22c1" + a.toString() + " in " + setOfA.toString() + "}"
}

data class GenConj<T>(val a : T, val setOfA : T) : BooleanExpr<T>() {
	override fun toString() : String = "{\u22c0" + a.toString() + " in " + setOfA.toString() + "}"
}

fun main(args : Array<String>) {
	val expr = -(GenDisj("a", "A") or GenConj("b", "B"))
	println(expr)
	println(expr.toCNF().prettyPrint())
}

// == Disjunction and Conjunction sets ==

class Disjunction<T> : HashSet<BooleanExpr<T>> {
	constructor(collection : Collection<out BooleanExpr<T>>) : super(collection)
	constructor(element : BooleanExpr<T>) : super(listOf(element))
}

class Conjunction<T> : HashSet<Disjunction<T>> {
	constructor(collection : Collection<out Disjunction<T>>) : super(collection)
	constructor(element : Disjunction<T>) : super(listOf(element))
}

// == Transform to CNF ==

/**
 * Returns `true` iff expression is
 * - an atom,
 * - a set disjunction,
 * - or a set conjunction.
 */
fun BooleanExpr<*>.isGenAtom() =
		when (this) {
			is Atom -> true
			is GenDisj -> true
			is GenConj -> true
			else -> false
		}

/**
 * Returns `true` iff expression is a [generalized atom][isGenAtom] or a negated [generalized atom][isGenAtom].
 */
fun BooleanExpr<*>.isLiteral() =
		when (this) {
			is NotExpr -> this.a.isGenAtom()
			else -> this.isGenAtom()
		}

/**
 * Returns `true` iff disjunction is a clause, i.e., contains only literals.
 */
fun <T> Disjunction<T>.isClause() : Boolean = this.all { it.isLiteral() }

/**
 * Removes and returns an element of a disjunction that is not a literal.
 * Returns `null` if the disjunction contains no non-literal.
 */
fun <T> Disjunction<T>.popNonLiteral() : BooleanExpr<T>? {
	val expr = this.find { !it.isLiteral() }
	if (expr != null) this.remove(expr)
	return expr;
}

/**
 * Removes and returns an element of a conjunction that is not a clause.
 * Returns `null` if the conjunction contains no non-clause.
 */
fun <T> Conjunction<T>.popNonClause() : Disjunction<T>? {
	val disj = this.find { !it.isClause() }
	if (disj != null) this.remove(disj)
	return disj
}

/**
 * Returns the conjunctive normal form (conjunction of clauses) of expression.
 */
fun <T> BooleanExpr<T>.toCNF() : Conjunction<T> {
	val conj = Conjunction(Disjunction(this))
	while (true) {
		var disj = conj.popNonClause()
		if (disj == null)
			return conj;
		var expr : BooleanExpr<T> = disj.popNonLiteral()!!;
		when (expr) {
			is Atom -> throw IllegalStateException() // cannot happen, expr would be a literal
			is GenDisj -> throw IllegalStateException() // cannot happen, expr would be a literal
			is GenConj -> throw IllegalStateException() // cannot happen, expr would be a literal
			is NotExpr -> {
				val a = expr.a;
				when (a) {
					is Atom -> throw IllegalStateException() // cannot happen, expr would be a literal
					is GenDisj -> throw IllegalStateException() // cannot happen, expr would be a literal
					is GenConj -> throw IllegalStateException() // cannot happen, expr would be a literal
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
