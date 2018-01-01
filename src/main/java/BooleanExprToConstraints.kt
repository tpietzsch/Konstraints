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

data class ElementAndSet<T>(val a : T, val setOfA : T )

data class LHS<T>( val posVar : MutableList<T> = ArrayList(), val negVar : MutableList<T> = ArrayList(), val posSet : MutableList<ElementAndSet<T>> = ArrayList(), val negSet : MutableList<ElementAndSet<T>> = ArrayList())

data class RHS<T>( var const : Int = 1, val negSet : MutableList<ElementAndSet<T>> = ArrayList() )

data class ConstraintLine<T>( val allQuant : MutableList<ElementAndSet<T>> = ArrayList(), val lhs : LHS<T> = LHS(), val rhs : RHS<T> = RHS() )

fun <T> ConstraintLine<T>.toText() : String
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
	println(cnfConstraintLines(cnf))
}

fun BooleanExpr<*>.isGenLiteral() =
		when (this) {
			is NotExpr -> this.a is GenDisj || this.a is GenConj
			is GenDisj -> true
			is GenConj -> true
			else -> false
		}

fun <T> cnfConstraintLines(cnf : Conjunction<T>) = cnf.map { clauseConstraintLines(it) }.joinToString("\n")

fun <T> clauseConstraintLines(clause : Disjunction<T>) : String {
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
		}
	}

	val line = ConstraintLine<T>()
	posVar.forEach { line.lhs.posVar.add( it.a ) }
	negVar.forEach {
		line.lhs.negVar.add( it.a )
		line.rhs.const -= 1
	}
	posDisj.forEach { line.lhs.posSet.add(ElementAndSet(it.a, it.setOfA)) }
	negDisj.forEach {
		line.allQuant.add(ElementAndSet(it.a, it.setOfA))
		line.lhs.negVar.add(it.a)
		line.rhs.const -= 1
	}
	posConj.forEach {
		line.allQuant.add(ElementAndSet(it.a, it.setOfA))
		line.lhs.posVar.add(it.a)
	}
	negConj.forEach {
		line.lhs.negSet.add(ElementAndSet(it.a, it.setOfA))
		line.rhs.negSet.add(ElementAndSet(it.a, it.setOfA))
	}

	return line.toText();
	// TODO... return line, translate to text elsewhere
}


// =================

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

