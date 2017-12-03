import java.util.ArrayList

import TokenType.*

enum class TokenType {
	// Single-character tokens.
	LEFT_PAREN,
	RIGHT_PAREN,
	NOT, AND, OR,

	// Two-character tokens.
	EQUIVALENT,
	IMPLIES,

	// Literals.
	IDENTIFIER,

	EOF
}

data class Token(val type : TokenType, val lexeme : String, val from : Int, val to : Int) {
	override fun toString() : String = String() + type + " " + lexeme
}

fun scan(source : String) : List<Token> = Scanner(source).scanTokens()

private class Scanner(val source : String) {
	private val tokens = ArrayList<Token>()
	private var start = 0
	private var current = 0
	private var line = 1

	fun scanTokens() : List<Token> {
		while (!isAtEnd()) {
			// We are at the beginning of the next lexeme.
			start = current;
			scanToken();
		}

		tokens.add(Token(EOF, "", source.length, source.length));
		return tokens;
	}

	private fun scanToken() {
		var c = advance();

		when (c) {
			'(' -> addToken(LEFT_PAREN)
			')' -> addToken(RIGHT_PAREN)
			'!' -> addToken(NOT)
			'&' -> addToken(AND)
			'|' -> addToken(OR)

			' ', '\r', '\t' -> {
			}

			'-' -> {
				if (match('>'))
					addToken(IMPLIES)
				else
					error((current - 1), "Unexpected character.")
			}

			'=' -> {
				if (match('='))
					addToken(EQUIVALENT)
				else
					error((current - 1), "Unexpected character.")
			}

			else -> {
				when {
					isAlpha(c) -> identifier()
					else -> error((current - 1), "Unexpected character.")
				}
			}
		}
	}

	private fun advance() : Char = source[current++]

	private fun isAtEnd() : Boolean = current >= source.length

	private fun peek() = if (isAtEnd()) '\u0000' else source[current]

	private fun match(expected : Char) : Boolean {
		if (isAtEnd()) return false
		if (source[current] != expected) return false

		current++
		return true
	}

	private fun addToken(type : TokenType) {
		val text = source.substring(start, current)
		tokens.add(Token(type, text, start, current))
	}

	private fun identifier() {
		while (isAlphaNumeric(peek())) advance()
		addToken(IDENTIFIER)
	}

	private fun isDigit(c : Char) : Boolean = c in '0' .. '9'

	private fun isAlpha(c : Char) : Boolean = c in 'a' .. 'z' || c in 'A' .. 'Z' || c == '_'

	private fun isAlphaNumeric(c : Char) = isAlpha(c) || isDigit(c)
}
