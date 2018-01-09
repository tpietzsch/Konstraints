import org.apache.commons.cli.Options
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.ParseException

fun main(args : Array<String>) {
	val options = Options()
	options.addOption("i", "interactive", false, "interactive")
	options.addOption("h", "help", false, "show help")

	fun printHelp() {
		val formatter = HelpFormatter()
		formatter.printHelp("kons [options] [expression]", "", options, """
operators:
  "!"    negation
  "&"    conjunction
  "|"    discjuntion
  "->"   implication
  "=="   equivalency

grammar:
  expression     → implication ;
  implication    → binary ( ( "->" | "==" ) binary )* ;
  binary         → unary ( ( "&" | "|" ) unary )* ;
  unary          → "!" unary ;
                 | primary ;
  primary        → IDENTIFIER
                 | "(" expression ")" ;

expression examples:
  a & b
  (a | b) & !(a | c)
  (a & b) -> c
  (a & b) == c
""")
	}

	val parser = DefaultParser()
	try {
		val line = parser.parse(options, args)

		val source = line.args.joinToString(" ")
		val interactive = line.hasOption("i")
		val help = line.hasOption("h") || (source.isBlank() && !interactive)
		if (help) printHelp()

		run(source, interactive)
	} catch (exp : ParseException) {
		System.err.println("Parsing failed.  Reason: " + exp.message)
		return;
	}
}

fun run(source : String, interactive : Boolean) {
	run(source)
	if (interactive)
		while (true) {
			print("> ")
			run(readLine()!!)
		}
}

private fun run(source : String) {
	if (source.isBlank())
		return
	try {
		val expr = parse(source)
		val cnf = expr.toCNF()
		val constraints = constraints(cnf)

		println("Input: \n    " + expr.toString())
		println("CNF: \n    " + cnf.prettyPrint())
		println("Constraints")
		constraints.forEach { println("    " + it.toText()) }
		println()
	} catch (e : ParseError) {
	}
}

// === printing ===

private fun <T> ClauseConstraint<T>.toText() : String {
	val pv = lhs.posVar.joinToString(" + ");
	val nv = lhs.negVar.joinToString(" - ");
	return "${pv}${if (nv.isEmpty()) "" else  " - "}${nv} ≥ ${rhs}";
}
