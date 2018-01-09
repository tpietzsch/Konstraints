// == boolean constraint representing one clause ==

/**
 * The left-hand side of a constraint (linear expression on boolean variables)
 */
data class LHS<T>(
		/**
		 * boolean variables with coefficient `+1`
		 */
		val posVar : List<T>,
		/**
		 * boolean variables with coefficient `-1`
		 */
		val negVar : List<T>)


/**
 * Boolean constraint representing one clause: *[lhs] ≥ [rhs]*
 */
data class ClauseConstraint<T>(
		/**
		 * Left-hand side of the constraint.
		 */
		val lhs : LHS<T>,
		/**
		 * Right-hand side of the constraint (constant term).
		 */
		var rhs : Int)

/**
 * Transform a CNF into a set of contraints
 */
fun <T> constraints(cnf : Conjunction<T>) : List<ClauseConstraint<T>> = cnf.map { it -> constraint(it) }

/**
 * Transform a clause into a contraint
 */
fun <T> constraint(clause : Disjunction<T>) : ClauseConstraint<T> {
	val posVar = mutableListOf<T>()
	val negVar = mutableListOf<T>()
	clause.forEach {
		when (it) {
			is NotExpr -> negVar.add((it.a as Atom<T>).a)
			is Atom -> posVar.add(it.a)
			else -> throw IllegalStateException("not a clause")
		}
	}

//	val (positive, negative) = clause.partition { it is Atom }
//	val posVar = positive.map { (it as Atom).a }
//	val negVar = negative.map { ((it as NotExpr).a as Atom).a }

	return ClauseConstraint(LHS(posVar, negVar), 1 - negVar.size)
}

