// == Disjunction and Conjunction sets ==

/**
 * A disjunction of boolean expressions (possibly a clause, if all expressions are literals).
 */
class Disjunction<T> : HashSet<BooleanExpr<T>> {
	constructor(collection : Collection<BooleanExpr<T>>) : super(collection)
	constructor(element : BooleanExpr<T>) : super(listOf(element))
}

/**
 * A conjunction of disjunctions of boolean expressions (possibly a CNF, if all disjunctions are clauses).
 */
class Conjunction<T> : HashSet<Disjunction<T>> {
	constructor(collection : Collection<Disjunction<T>>) : super(collection)
	constructor(element : Disjunction<T>) : super(listOf(element))
}

// == Transform boolean expression to CNF ==

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
 * Removes redundant clauses from a CNF.
 * (A clause is redundant if it is a super-set of another clause.)
 */
fun <T> removeRedundant(cnf : Conjunction<T>) = Conjunction(
		cnf.filter { clause ->
			cnf.filterNot { it == clause }
					.none { clause.containsAll(it) }
		})

/**
 * Returns a conjunctive normal form (conjunction of clauses) of a boolean expression.
 */
fun <T> BooleanExpr<T>.toCNF() : Conjunction<T> {
	val conj = Conjunction(Disjunction(this))
	while (true) {
		var disj = conj.popNonClause()
		if (disj == null)
			return removeRedundant(conj);
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
					is ImplExpr -> {
						disj.add(a.a and -a.b)
						conj.add(disj)
					}
					is EquExpr -> {
						disj.add(a.a and a.b)
						disj.add(-a.a and -a.b)
						conj.add(disj)
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

