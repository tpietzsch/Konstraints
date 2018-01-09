class ParseError : RuntimeException()

class ParseErrors(private val thesource : String) {

	private var _hadError = false

	val hadError : Boolean get() = _hadError

	fun error(pos : Int, message : String, where : String = "", length : Int = 1) : ParseError {
		println("Error$where: $message")

		println(thesource.replace("\t", " "))
		val sb = StringBuilder()
		repeat(pos, { sb.append(" ") })
		repeat(length, { sb.append("^") })
		println(sb)

		_hadError = true
		return ParseError()
	}

	fun error(token : Token, message : String) : ParseError {
		if (token.type === TokenType.EOF) {
			return error(token.from, message, " at end")
		} else {
			return error(token.from, message, " at '${token.lexeme}'", token.to - token.from)
		}
	}
}
