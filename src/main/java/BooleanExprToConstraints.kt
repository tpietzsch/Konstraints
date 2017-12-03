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
	println(cnfConstraints(cnf))
}

fun <T> removeRedundant(cnf : Conjunction<T>) = Conjunction(
		cnf.filter { clause ->
			cnf.filterNot { it == clause }
					.none { clause.containsAll(it) }
		})

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

