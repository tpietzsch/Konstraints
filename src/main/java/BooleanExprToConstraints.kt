fun main(args : Array<String>) {
	val source = args.joinToString(" ")
	run(source)
	while (true) {
		print("> ")
		run(readLine()!!)
	}
}

private fun run(source : String) {
	try {
		val expr = parse(source)
		val cnf = expr.toCNF()
		val constraints = constraints(cnf)

		println("Input: \n    " + expr.toString())
		println("CNF: \n    " + cnf.prettyPrint())
		println("Constraints")
		constraints.forEach { println("    " + it.toText()) }
	} catch (e : ParseError) {
	}
}

// === printing ===

private fun <T> ClauseConstraint<T>.toText() : String {
	val squant = allQuant.joinToString(" ") { eas -> "(∀ ${eas.a} ∈ ${eas.setOfA})" }

	// LHS
	val pv = lhs.posVar.joinToString(" + ");
	val nv = lhs.negVar.joinToString(" - ");
	val ps = lhs.posSet.joinToString(" + ") { eas -> "(∑ ${eas.a} ∈ ${eas.setOfA})" }
	val ns = lhs.negSet.joinToString(" - ") { eas -> "(∑ ${eas.a} ∈ ${eas.setOfA})" }

	val p = pv + (if (lhs.posSet.isEmpty() || lhs.posVar.isEmpty()) "" else " + ") + ps;
	val n = nv + (if (lhs.negSet.isEmpty() || lhs.negVar.isEmpty()) "" else " - ") + ns;
	val slhs = p + (if (n.isEmpty()) "" else " - ") + n;

	// RHS
	val nc = rhs.negSet.joinToString(" - ") { eas -> "‖${eas.setOfA}‖" }
	val srhs =
			if (rhs.const == 0) {
				if (rhs.negSet.isEmpty())
					"0"
				else
					" - " + nc;
			} else {
				rhs.const.toString() + (if (rhs.negSet.isEmpty()) "" else " - ") + nc;
			}

	return squant + (if (allQuant.isEmpty()) "" else " : ") + "${slhs} ≥ ${srhs}";
}
