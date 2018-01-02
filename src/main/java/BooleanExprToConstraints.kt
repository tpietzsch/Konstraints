fun main(args : Array<String>) {
	val source = args.joinToString(" ")
	run(source)

	while (true) {
		print("> ")
		run(readLine()!!)
	}
}

private fun run(source : String) {
	thesource = source
	hadError = false;

	val tokens = scan(source)
	if (hadError)
		return

	val expr = parse(tokens)
	if (hadError)
		return

	val cnf = expr.toCNF()

	println(expr.toString())
	println(cnf.prettyPrint())
	println(removeRedundant(cnf).prettyPrint())

	printit(removeRedundant(cnf))
//	println(cnfConstraints(cnf))
}

// === printing ===

fun <T> ClauseConstraint<T>.toText() : String
{
	val squant = allQuant.joinToString( " ") { eas -> "(∀ ${eas.a} ∈ ${eas.setOfA})" }

	// LHS
	val pv = lhs.posVar.joinToString(" + ");
	val nv = lhs.negVar.joinToString(" - ");
	val ps = lhs.posSet.joinToString( " + " ) { eas -> "(∑ ${eas.a} ∈ ${eas.setOfA})" }
	val ns = lhs.negSet.joinToString( " - " ) { eas -> "(∑ ${eas.a} ∈ ${eas.setOfA})" }

	val p = pv + (if (lhs.posSet.isEmpty() || lhs.posVar.isEmpty()) "" else " + ") + ps;
	val n = nv + (if (lhs.negSet.isEmpty() || lhs.negVar.isEmpty()) "" else " - ") + ns;
	val slhs = p + (if (n.isEmpty()) "" else " - " ) + n;

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

	return squant + ( if (allQuant.isEmpty()) "" else " : " ) + "${slhs} ≥ ${srhs}";
}

fun printit(cnf : Conjunction<String>) {
	println(constraintLines(cnf))
}

fun BooleanExpr<*>.isGenLiteral() =
		when (this) {
			is NotExpr -> this.a is GenDisj || this.a is GenConj
			is GenDisj -> true
			is GenConj -> true
			else -> false
		}

fun <T> constraintLines(cnf : Conjunction<T>) = cnf.map { constraintLines(it) }.joinToString("\n")

fun <T> constraintLines(clause : Disjunction<T>) = clause.toConstraint().toText()

// =================

fun <T> cnfConstraints(cnf : Conjunction<T>) = cnf.map { clauseConstraints(it) }.joinToString("\n")

fun <T> clauseConstraints(clause : Disjunction<T>) : String {
	val (positive, negative) = clause.partition { it is Atom }
	val s1 = positive.joinToString(" + ");
	val s2 = negative.joinToString(" - ", transform = { e -> (e as NotExpr<T>).a.toString() })
	return s1 + (if (negative.isEmpty()) "" else " - " + s2) + " ≥ ${1 - negative.size}"
}

var thesource : String = ""

fun error(pos : Int, message : String, where : String = "", length : Int = 1) {
	println("Error$where: $message")

	println(thesource.replace("\t", " "))
	val sb = StringBuilder()
	repeat(pos, { sb.append(" ") })
	repeat(length, { sb.append("^") })
	println(sb)

	hadError = true
}

fun error(token : Token, message : String) {
	if (token.type === TokenType.EOF) {
		error(token.from, message, " at end")
	} else {
		error(token.from, message, " at '${token.lexeme}'", token.to - token.from)
	}
}

private var hadError = false

