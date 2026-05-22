import java.util.*;

// ── Recursive-Descent Parser ───────────────────────────────────────────────────
// Each method below corresponds directly to a grammar rule in GRAMMAR.md.
// See GRAMMAR.md §9 for the mapping table.
class Parser {
    private final List<Token> tokens;
    private int pos = 0;

    public Parser(List<Token> tokens) { this.tokens = tokens; }

    // Grammar: program ::= { statement } EOF
    public Program parse() {
        Program p = new Program();
        while (!atEnd()) {
            p.statements.add(statement());
        }
        return p;
    }

    // Grammar: statement ::= varDecl | assignment | ifStmt | forStmt | printStmt | exprStmt
    private Stmt statement() {
        if (match(TokenType.INT_TYPE, TokenType.BOOL_TYPE)) return varDecl();
        if (match(TokenType.IF))                            return ifElse();
        if (match(TokenType.FOR))                           return forStmt();   // <-- new
        if (match(TokenType.PRINT))                         return printStmt();
        if (check(TokenType.IDENT) && peekNext().type == TokenType.ASSIGN) return assignment();
        return exprStmt();
    }

    // Grammar: printStmt ::= "দেখান" "(" expression ")" [ ";" ]
    private Stmt printStmt() {
        int line = prev().line;
        consume(TokenType.LPAREN, "Expect '(' after 'দেখান'");
        Expr value = expression();
        consume(TokenType.RPAREN, "Expect ')' after value");
        match(TokenType.SEMICOLON);
        PrintStmt p = new PrintStmt();
        p.expression = value;
        p.line = line;
        return p;
    }

    // Grammar: varDecl ::= type IDENT "=" expression [ ";" ]
    private Stmt varDecl() {
        Token typeTok = prev();
        String type = typeTok.type == TokenType.INT_TYPE ? "int" : "bool";
        String name = (String) consume(TokenType.IDENT, "Expect variable name").value;
        consume(TokenType.ASSIGN, "Expect '=' after name");
        Expr val = expression();
        match(TokenType.SEMICOLON);
        VarDecl d = new VarDecl(); d.typeName = type; d.name = name; d.value = val; d.line = typeTok.line;
        return d;
    }

    // Grammar: assignment ::= IDENT "=" expression [ ";" ]
    private Stmt assignment() {
        Token nameTok = consume(TokenType.IDENT, "Expect variable name");
        consume(TokenType.ASSIGN, "Expect '='");
        Expr val = expression();
        match(TokenType.SEMICOLON);
        Assignment a = new Assignment(); a.name = (String)nameTok.value; a.value = val; a.line = nameTok.line;
        return a;
    }

    // Grammar: ifStmt ::= "যদি" "(" expression ")" "{" { statement } "}" [ "নাহলে" "{" { statement } "}" ]
    private Stmt ifElse() {
        int line = prev().line;
        consume(TokenType.LPAREN, "Expect '(' after 'যদি'");
        Expr cond = expression();
        consume(TokenType.RPAREN, "Expect ')' after condition");
        consume(TokenType.LBRACE, "Expect '{' to start block");
        List<Stmt> thenB = block();
        List<Stmt> elseB = null;
        if (match(TokenType.ELSE)) {
            consume(TokenType.LBRACE, "Expect '{' after 'নাহলে'");
            elseB = block();
        }
        IfElse i = new IfElse(); i.cond = cond; i.thenB = thenB; i.elseB = elseB; i.line = line;
        return i;
    }

    // Grammar: forStmt ::= "প্রতিটি" "(" IDENT "=" expression "থেকে" expression [ "ধাপ" expression ] ")" "{" { statement } "}"
    private Stmt forStmt() {
        int line = prev().line;
        consume(TokenType.LPAREN, "Expect '(' after 'প্রতিটি'");
        String varName = (String) consume(TokenType.IDENT, "Expect loop variable name").value;
        consume(TokenType.ASSIGN, "Expect '=' after loop variable");
        Expr start = expression();
        consume(TokenType.TO, "Expect 'থেকে' after start value");
        Expr end = expression();
        Expr step = null;
        if (match(TokenType.STEP)) {
            step = expression();
        }
        consume(TokenType.RPAREN, "Expect ')' after for-loop header");
        consume(TokenType.LBRACE, "Expect '{' to start for-loop body");
        List<Stmt> body = block();

        ForLoop f = new ForLoop();
        f.varName = varName; f.start = start; f.end = end; f.step = step; f.body = body; f.line = line;
        return f;
    }

    // Helper: { statement } until "}"
    private List<Stmt> block() {
        List<Stmt> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !atEnd()) {
            stmts.add(statement());
        }
        consume(TokenType.RBRACE, "Expect '}' after block");
        return stmts;
    }

    private Stmt exprStmt() {
        Expr e = expression();
        match(TokenType.SEMICOLON);
        return (Stmt) e;
    }

    // ── Expression parsing (recursive descent by precedence) ──────────────────
    // Grammar: expression ::= comparison
    private Expr expression() { return comparison(); }

    // Grammar: comparison ::= term { compOp term }
    private Expr comparison() {
        Expr e = term();
        while (match(TokenType.EQ, TokenType.NEQ, TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE)) {
            String op = getOpString(prev().type);
            BinaryOp b = new BinaryOp(); b.left = e; b.op = op; b.right = term(); e = b;
        }
        return e;
    }

    // Grammar: term ::= factor { ( "+" | "-" ) factor }
    private Expr term() {
        Expr e = factor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            BinaryOp b = new BinaryOp(); b.left = e; b.op = prev().type == TokenType.PLUS ? "+" : "-";
            b.right = factor(); e = b;
        }
        return e;
    }

    // Grammar: factor ::= primary { ( "*" | "/" | "%" ) primary }
    private Expr factor() {
        Expr e = primary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            BinaryOp b = new BinaryOp(); b.left = e; b.op = getOpString(prev().type);
            b.right = primary(); e = b;
        }
        return e;
    }

    // Grammar: primary ::= NUMBER | "সত্য" | "মিথ্যা" | IDENT | "(" expression ")"
    private Expr primary() {
        if (match(TokenType.NUMBER)) { NumberLit n = new NumberLit(); n.value = (int)prev().value; return n; }
        if (match(TokenType.TRUE, TokenType.FALSE)) { BoolLit b = new BoolLit(); b.value = (boolean)prev().value; return b; }
        if (match(TokenType.IDENT)) { Identifier i = new Identifier(); i.name = (String)prev().value; return i; }
        if (match(TokenType.LPAREN)) { Expr e = expression(); consume(TokenType.RPAREN, "Missing ')'"); return e; }
        throw new RuntimeException("Unexpected token: " + curr() + " at line " + curr().line);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private boolean atEnd()      { return curr().type == TokenType.EOF; }
    private Token curr()         { return tokens.get(pos); }
    private Token prev()         { return tokens.get(pos - 1); }
    private Token peekNext()     { return (pos + 1 < tokens.size()) ? tokens.get(pos + 1) : tokens.get(tokens.size()-1); }
    private boolean check(TokenType t) { return !atEnd() && curr().type == t; }
    private Token advance()      { if (!atEnd()) pos++; return prev(); }
    private boolean match(TokenType... types) { for (TokenType t : types) { if (check(t)) { advance(); return true; } } return false; }
    private Token consume(TokenType t, String m) { if (check(t)) return advance(); throw new RuntimeException(m + " (got " + curr() + ")"); }

    private String getOpString(TokenType t) {
        return switch (t) {
            case EQ      -> "=="; case NEQ    -> "!="; case LT  -> "<";
            case GT      -> ">";  case LTE    -> "<="; case GTE -> ">=";
            case STAR    -> "*";  case SLASH  -> "/";  case PERCENT -> "%";
            default      -> "+";
        };
    }
}
