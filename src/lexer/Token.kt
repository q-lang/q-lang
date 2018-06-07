package lexer

data class Token(
        val symbol: String,
        val text: String,
        val row: Int,
        val col: Int)
