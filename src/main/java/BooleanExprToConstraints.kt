import org.apache.commons.cli.Options
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.ParseException

fun main(args : Array<String>) {
	val options = Options()
	options.addOption("t", "tex", false, "latex output")
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

reserved names:
  "one", "any", "all", "of", "in"

grammar:
  expression     → implication ;
  implication    → binary ( ( "->" | "==" ) binary )* ;
  binary         → unary ( ( "&" | "|" ) unary )* ;
  unary          → "!" unary ;
                 | primary ;
  primary        → IDENTIFIER
                 | ( "one" | "all" ) IDENTIFIER "in" IDENTIFIER
                 | "(" expression ")" ;

expression examples:
  a & b
  (a | b) & !(a | c)
  (a & b) -> c
  (a & b) == c
  (any x in X) -> (a | b)
  (all x in X) == c

""")
	}

	val parser = DefaultParser()
	try {
		val line = parser.parse(options, args)

		if (line.hasOption("h")) printHelp()

		val tex = line.hasOption("t")
		val source = line.args.joinToString(" ")
		val interactive = true;//source.isBlank()

		run(source, interactive, tex)
	} catch (exp : ParseException) {
		System.err.println("Parsing failed.  Reason: " + exp.message)
		return;
	}
}

fun run(source : String, interactive : Boolean, tex : Boolean) {
	run(source, tex)
	if (interactive)
		while (true) {
			print("> ")
			run(readLine()!!, tex)
		}
}

private fun run(source : String, tex : Boolean = false) {
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

		if (tex) {
			println("Input:\n\\[\n  ${expr.toLatex()}\n\\]")
			println("CNF:\n\\[\n  ${cnf.toLatex()}\n\\]" )
			println("Constraints:\n\\[\n  \\begin{array}{rrcl}")
			constraints.forEach { println("    " + it.toLatex() + "\\\\") }
			println("  \\end{array}\n\\]")
			println()
		}
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

private fun <T> ClauseConstraint<T>.toLatex() : String {
	val squant = allQuant.joinToString(" ") { eas -> "\\left( \\forall ${eas.a} \\in ${eas.setOfA} \\right)" }

	// LHS
	val pv = lhs.posVar.joinToString(" + ");
	val nv = lhs.negVar.joinToString(" - ");
	val ps = lhs.posSet.joinToString(" + ") { eas -> "\\left( \\sum_{${eas.a} \\in ${eas.setOfA}} ${eas.a} \\right)" }
	val ns = lhs.negSet.joinToString(" - ") { eas -> "\\left( \\sum_{${eas.a} \\in ${eas.setOfA}} ${eas.a} \\right)" }

	val p = pv + (if (lhs.posSet.isEmpty() || lhs.posVar.isEmpty()) "" else " + ") + ps;
	val n = nv + (if (lhs.negSet.isEmpty() || lhs.negVar.isEmpty()) "" else " - ") + ns;
	val slhs = p + (if (n.isEmpty()) "" else " - ") + n;

	// RHS
	val nc = rhs.negSet.joinToString(" - ") { eas -> "\\left|${eas.setOfA}\\right|" }
	val srhs =
			if (rhs.const == 0) {
				if (rhs.negSet.isEmpty())
					"0"
				else
					" - " + nc;
			} else {
				rhs.const.toString() + (if (rhs.negSet.isEmpty()) "" else " - ") + nc;
			}

	return squant + (if (allQuant.isEmpty()) "& " else " :& ") + "${slhs} &\\ge & ${srhs}";
}

// extension functions CANNOT be polymorphic (statically resolved)
private fun <T> BooleanExpr<T>.toLatex() : String = when(this) {
	is Atom -> this.toLatex()
	is NotExpr -> this.toLatex()
	is AndExpr -> this.toLatex()
	is OrExpr -> this.toLatex()
	is ImplExpr -> this.toLatex()
	is EquExpr -> this.toLatex()
	is GenDisj -> this.toLatex()
	is GenConj -> this.toLatex()
	else -> ""
}
private fun <T> Atom<T>.toLatex() = toString()
private fun <T> NotExpr<T>.toLatex() = "\\neg ${a.toLatex()}"
private fun <T> AndExpr<T>.toLatex() = "\\left( ${a.toLatex()} \\land ${b.toLatex()} \\right)"
private fun <T> OrExpr<T>.toLatex() = "\\left( ${a.toLatex()} \\lor ${b.toLatex()} \\right)"
private fun <T> ImplExpr<T>.toLatex() = "\\left( ${a.toLatex()} \\longrightarrow ${b.toLatex()} \\right)"
private fun <T> EquExpr<T>.toLatex() = "\\left( ${a.toLatex()} \\longleftrightarrow ${b.toLatex()} \\right)"
private fun <T> GenDisj<T>.toLatex() = "\\left( \\bigvee_{${a} \\in ${setOfA}} ${a} \\right)"
private fun <T> GenConj<T>.toLatex() = "\\left( \\bigwedge_{${a} \\in ${setOfA}} ${a} \\right)"

private fun <T> Conjunction<T>.toLatex() : String =
		this.joinToString(", ", "\\left[", "\\right]" ) {
			clause -> clause.joinToString(", ", "\\left[", "\\right]") {
				expr -> expr.toLatex()
			}
		}
