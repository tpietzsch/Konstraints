// == boolean constraint(s) representing one clause ==

/**
 * A set [setOfA] and element [a] from the set.
 */
data class ElementAndSet<T>(val a : T, val setOfA : T)

/**
 * The left-hand side of a constraint (linear expression on boolean variables)
 */
data class LHS<T>(
		/**
		 * boolean variables with coefficient `+1`
		 */
		val posVar : MutableList<T> = ArrayList(),
		/**
		 * boolean variables with coefficient `-1`
		 */
		val negVar : MutableList<T> = ArrayList(),
		/**
		 * sum over set of boolean variables with coefficient `+1`
		 */
		val posSet : MutableList<ElementAndSet<T>> = ArrayList(),
		/**
		 * sum over set of boolean variables with coefficient `-1`
		 */
		val negSet : MutableList<ElementAndSet<T>> = ArrayList())

/**
 * The right-hand side of a constraint (constant, possibly involving set cardinalities)
 */
data class RHS<T>(
		/**
		 * constant term.
		 */
		var const : Int = 1,
		/**
		 * sets whose cardinality to subtract from [const].
		 */
		val negSet : MutableList<ElementAndSet<T>> = ArrayList())

/**
 * Boolean constraint(s) representing one clause: *([allQuant]) [lhs] ≥ [rhs]*
 */
data class ClauseConstraint<T>(
		/**
		 * Sets to all-quantify over (creates set of constraints).
		 */
		val allQuant : MutableList<ElementAndSet<T>> = ArrayList(),
		/**
		 * Left-hand side of the constraint.
		 */
		val lhs : LHS<T> = LHS(),
		/**
		 * Right-hand side of the constraint.
		 */
		val rhs : RHS<T> = RHS())

/**
 * Transform a CNF into a set of contraints
 */
fun <T> constraints(cnf : Conjunction<T>) : List<ClauseConstraint<T>> = cnf.map { it -> constraint(it) }

/**
 * Transform a clause into a contraint
 */
fun <T> constraint(clause : Disjunction<T>) : ClauseConstraint<T> {
	val posVar = mutableListOf<Atom<T>>()
	val negVar = mutableListOf<Atom<T>>()
	val posDisj = mutableListOf<GenDisj<T>>()
	val negDisj = mutableListOf<GenDisj<T>>()
	val posConj = mutableListOf<GenConj<T>>()
	val negConj = mutableListOf<GenConj<T>>()
	clause.forEach {
		when (it) {
			is NotExpr -> {
				when (it.a) {
					is GenDisj -> negDisj.add(it.a)
					is GenConj -> negConj.add(it.a)
					else -> negVar.add(it.a as Atom<T>)
				}
			}
			is GenDisj -> posDisj.add(it)
			is GenConj -> posConj.add(it)
			is Atom -> posVar.add(it)
			else -> throw IllegalStateException("not a clause")
		}
	}

	val constraint = ClauseConstraint<T>()
	posVar.forEach { constraint.lhs.posVar.add(it.a) }
	negVar.forEach {
		constraint.lhs.negVar.add(it.a)
		constraint.rhs.const -= 1
	}
	posDisj.forEach { constraint.lhs.posSet.add(ElementAndSet(it.a, it.setOfA)) }
	negDisj.forEach {
		constraint.allQuant.add(ElementAndSet(it.a, it.setOfA))
		constraint.lhs.negVar.add(it.a)
		constraint.rhs.const -= 1
	}
	posConj.forEach {
		constraint.allQuant.add(ElementAndSet(it.a, it.setOfA))
		constraint.lhs.posVar.add(it.a)
	}
	negConj.forEach {
		constraint.lhs.negSet.add(ElementAndSet(it.a, it.setOfA))
		constraint.rhs.negSet.add(ElementAndSet(it.a, it.setOfA))
	}

	return constraint
}

