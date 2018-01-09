import TokenType.*

/*
Grammar:

expression     → implication ;
implication    → binary ( ( "->" | "==" ) binary )* ;
binary         → unary ( ( "&" | "|" ) unary )* ;
unary          → "!" unary ;
               | primary ;
primary        → IDENTIFIER
               | "(" expression ")" ;
*/

fun parse(source : String) : BooleanExpr<String> {
	val parseErrors = ParseErrors(source)
	val tokens = scan(source, parseErrors)
	if (parseErrors.hadError)
		throw ParseError()
	return Parser(tokens, parseErrors).parse()
}

private class Parser(private val tokens : List<Token>, private val parseErrors : ParseErrors) {
	private var current = 0

	fun parse() : BooleanExpr<String> {
		val expr = expression();
		if (!isAtEnd())
			throw parseErrors.error(peek(), "Unexpected token after end of expression.")
		return expr
	}

	fun expression() : BooleanExpr<String> {
		return implication()
	}

	private fun implication() : BooleanExpr<String> {
		var expr = binary()

		while (match(IMPLIES, EQUIVALENT)) {
			val operator = previous()
			val right = binary()
			expr = when (operator.type) {
				IMPLIES -> ImplExpr(expr, right)
				EQUIVALENT -> EquExpr(expr, right)
				else -> throw IllegalStateException()
			}
		}

		return expr
	}

	private fun binary() : BooleanExpr<String> {
		var expr = unary()

		while (match(AND, OR)) {
			val operator = previous()
			val right = unary()
			expr = when (operator.type) {
				AND -> AndExpr(expr, right)
				OR -> OrExpr(expr, right)
				else -> throw IllegalStateException()
			}
		}

		return expr
	}

	private fun unary() : BooleanExpr<String> {
		if (match(NOT)) {
			val right = unary()
			return NotExpr(right)
		} else
			return primary()
	}

	private fun primary() : BooleanExpr<String> {
		if (match(IDENTIFIER)) {
			return Atom(previous().lexeme)
		}

		if (match(LEFT_PAREN)) {
			val expr = expression()
			consume(RIGHT_PAREN, "Expect ')' after expression.")
			return expr
		}

		throw parseErrors.error(peek(), "Expected identifier or '('.")
	}

	private fun consume(type : TokenType, message : String) : Token {
		if (check(type)) return advance()

		throw parseErrors.error(peek(), message)
	}

	private fun match(vararg types : TokenType) : Boolean {
		for (type in types) {
			if (check(type)) {
				advance()
				return true
			}
		}

		return false
	}

	private fun check(tokenType : TokenType) : Boolean {
		if (isAtEnd())
			return false
		else
			return peek().type == tokenType
	}

	private fun advance() : Token {
		if (!isAtEnd())
			current++
		return previous()
	}

	private fun isAtEnd() = peek().type === EOF

	private fun peek() = tokens[current]

	private fun previous() = tokens[current - 1]
}
