import TokenType.*

/*
Grammar:

expression     → binary ;
binary         → unary ( ( "&" | "|" ) unary )* ;
unary          → "!" unary ;
               | primary ;
primary        → IDENTIFIER
               | "(" expression ")" ;
*/

fun parse( tokens : List<Token> ) : BooleanExpr<String> = Parser(tokens).parse()

private class ParseError : RuntimeException()

private class Parser(private val tokens : List<Token>) {
	private var current = 0

	fun parse() : BooleanExpr<String> {
		try {
			val expr = binary();
			if ( !isAtEnd() )
				throw parseError(peek(), "Unexpected token after end of expression.")
			return expr
		} catch (e : ParseError) {
			return Atom("ERROR")
		}
	}

	fun expression() : BooleanExpr<String> {
		return binary()
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
		if (match(BANG)) {
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

		throw parseError(peek(), "Expected identifier or '('.")
	}

	private fun consume(type : TokenType, message : String) : Token {
		if (check(type)) return advance()

		throw parseError(peek(), message)
	}

	private fun parseError(token : Token, message : String) : ParseError {
		error(token, message)
		return ParseError()
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
