import java.util.*;

enum TokenType {
    INT_TYPE, BOOL_TYPE, NUMBER, TRUE, FALSE, IF, ELSE, IDENT,
    PLUS, MINUS, STAR, SLASH, PERCENT, EQ, NEQ, LT, GT, LTE, GTE,
    ASSIGN, LPAREN, RPAREN, LBRACE, RBRACE, SEMICOLON, EOF, PRINT
}

class Token {
    TokenType type;
    Object value;
    int line, col;

    Token(TokenType type, Object value, int line, int col) {
        this.type = type; this.value = value; this.line = line; this.col = col;
    }

    public String toString() {
        return String.format("Token(%s%s, line=%d, col=%d)", type, (value != null ? " " + value : ""), line, col);
    }
}

class Lexer {
    private final String src;
    private int pos = 0, line = 1, col = 1;
    private static final Map<String, TokenType> KEYWORDS = Map.of(
        "পূর্ণসংখ্যা", TokenType.INT_TYPE, "বুলিয়ান", TokenType.BOOL_TYPE,
        "সত্য", TokenType.TRUE, "মিথ্যা", TokenType.FALSE, "যদি", TokenType.IF, "নাহলে", TokenType.ELSE, "দেখান", TokenType.PRINT
    );

    public Lexer(String src) { this.src = src; }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (pos < src.length()) {
            char ch = peek();
            if (Character.isWhitespace(ch)) { advance(); continue; }
            if (ch == '#') { while (pos < src.length() && peek() != '\n') advance(); continue; }

            int startL = line, startC = col;
            if (isBanglaDigit(ch)) tokens.add(scanNumber(startL, startC));
            else if (isBanglaLetter(ch) || ch == '_') tokens.add(scanWord(startL, startC));
            else tokens.add(scanOperator(startL, startC));
        }
        tokens.add(new Token(TokenType.EOF, null, line, col));
        return tokens;
    }

    private Token scanOperator(int l, int c) {
        char ch = advance();
        switch (ch) {
            case '=': return match('=') ? new Token(TokenType.EQ, null, l, c) : new Token(TokenType.ASSIGN, null, l, c);
            case '!': if (match('=')) return new Token(TokenType.NEQ, null, l, c); throw new RuntimeException("Unexpected !");
            case '<': return match('=') ? new Token(TokenType.LTE, null, l, c) : new Token(TokenType.LT, null, l, c);
            case '>': return match('=') ? new Token(TokenType.GTE, null, l, c) : new Token(TokenType.GT, null, l, c);
            case '+': return new Token(TokenType.PLUS, null, l, c);
            case '-': return new Token(TokenType.MINUS, null, l, c);
            case '*': return new Token(TokenType.STAR, null, l, c);
            case '/': return new Token(TokenType.SLASH, null, l, c);
            case '%': return new Token(TokenType.PERCENT, null, l, c);
            case '(': return new Token(TokenType.LPAREN, null, l, c);
            case ')': return new Token(TokenType.RPAREN, null, l, c);
            case '{': return new Token(TokenType.LBRACE, null, l, c);
            case '}': return new Token(TokenType.RBRACE, null, l, c);
            case ';': return new Token(TokenType.SEMICOLON, null, l, c);
            default: throw new RuntimeException("Unknown char: " + ch);
        }
    }

    private Token scanNumber(int l, int c) {
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && isBanglaDigit(peek())) sb.append(advance());
        return new Token(TokenType.NUMBER, banglaToInt(sb.toString()), l, c);
    }

    private Token scanWord(int l, int c) {
    StringBuilder sb = new StringBuilder();
    // Keep reading while we see Bangla letters, digits, or underscores
    while (pos < src.length() && (isBanglaLetter(peek()) || isBanglaDigit(peek()) || peek() == '_')) {
        sb.append(advance());
    }
    String word = sb.toString();
    
    // 1. Look up the word in our keyword map
    TokenType type = KEYWORDS.get(word);
    
    // 2. If it's not a keyword, it's a variable name (Identifier)[cite: 1]
    if (type == null) {
        return new Token(TokenType.IDENT, word, l, c);
    }
    
    // 3. If it is a keyword, handle special values for True/False[cite: 1]
    Object value = null;
    if (type == TokenType.TRUE) value = true;
    if (type == TokenType.FALSE) value = false;
    
    return new Token(type, value, l, c);
    }

    private boolean isBanglaDigit(char ch) { return ch >= '\u09E6' && ch <= '\u09EF'; }
    private boolean isBanglaLetter(char ch) { return ch >= '\u0980' && ch <= '\u09FF'; }
    private int banglaToInt(String s) {
        StringBuilder res = new StringBuilder();
        for (char c : s.toCharArray()) res.append(c - '\u09E6');
        return Integer.parseInt(res.toString());
    }
    private char peek() { return pos < src.length() ? src.charAt(pos) : '\0'; }
    private char advance() { char ch = src.charAt(pos++); if (ch == '\n') { line++; col = 1; } else col++; return ch; }
    private boolean match(char exp) { if (peek() == exp) { advance(); return true; } return false; }
}