import kotlin.collections.Grouping
import java.time.temporal.TemporalAdjusters.previous
import sun.tools.jstat.Literal
import javax.print.DocFlavor.STRING
import org.apache.log4j.NDC.peek

fun main(args : Array<String>) {
	val source = args.joinToString(" ")
	run(source)

	while (true) {
		print("> ")
		run(readLine()!!)
	}
}

private fun run(source: String) {
	val tokens = scan(source)

	// For now, just print the tokens.
	for (token in tokens) {
		println(token)
	}

	println()

	println(parse(tokens))
}

fun error(line : Int, message : String) {
	report(line, "", message)
}

fun error(token : Token, message : String) {
	if (token.type === TokenType.EOF) {
		report(token.line, " at end", message)
	} else {
		report(token.line, " at '" + token.lexeme + "'", message)
	}
}

private var hadError = false

private fun report(line : Int, where : String, message : String) {
	System.err.println(
			"[line $line] Error$where: $message")
	hadError = true
}

