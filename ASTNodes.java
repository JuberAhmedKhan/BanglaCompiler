import java.util.*;

interface Node {}
interface Expr extends Node {}
interface Stmt extends Node {}

abstract class ASTNode implements Node {
    int line;
}

// ── Top-level ──────────────────────────────────────────────────────────────────
// Grammar: program ::= { statement } EOF
class Program implements Node {
    List<Stmt> statements = new ArrayList<>();
}

// ── Statements ─────────────────────────────────────────────────────────────────
// Grammar: varDecl ::= type IDENT "=" expression [ ";" ]
class VarDecl extends ASTNode implements Stmt {
    String typeName, name;
    Expr value;
}

// Grammar: assignment ::= IDENT "=" expression [ ";" ]
class Assignment extends ASTNode implements Stmt {
    String name;
    Expr value;
}

// Grammar: ifStmt ::= "যদি" "(" expression ")" "{" { statement } "}" [ "নাহলে" "{" { statement } "}" ]
class IfElse extends ASTNode implements Stmt {
    Expr cond;
    List<Stmt> thenB;
    List<Stmt> elseB;
}

// Grammar: forStmt ::= "প্রতিটি" "(" IDENT "=" expression "থেকে" expression [ "ধাপ" expression ] ")" "{" { statement } "}"
class ForLoop extends ASTNode implements Stmt {
    String varName;   // loop variable (declared implicitly as int)
    Expr start;       // start value
    Expr end;         // end value (inclusive)
    Expr step;        // step value (optional, null means default +1)
    List<Stmt> body;
}

// Grammar: printStmt ::= "দেখান" "(" expression ")" [ ";" ]
class PrintStmt extends ASTNode implements Stmt {
    Expr expression;
}

// ── Expressions ────────────────────────────────────────────────────────────────
// Grammar: comparison ::= term { compOp term }
// Grammar: term       ::= factor { ( "+" | "-" ) factor }
// Grammar: factor     ::= primary { ( "*" | "/" | "%" ) primary }
class BinaryOp extends ASTNode implements Expr {
    String op;
    Expr left, right;
}

// Grammar: primary ::= NUMBER
class NumberLit extends ASTNode implements Expr { int value; }

// Grammar: primary ::= "সত্য" | "মিথ্যা"
class BoolLit extends ASTNode implements Expr { boolean value; }

// Grammar: primary ::= IDENT
class Identifier extends ASTNode implements Expr { String name; }

// (Unary kept for Visitor interface completeness)
class UnaryOp extends ASTNode implements Expr { String op; Expr operand; }

// ── Visitor interface ──────────────────────────────────────────────────────────
interface Visitor<R> {
    R visit(Program n);
    R visit(VarDecl n);
    R visit(Assignment n);
    R visit(IfElse n);
    R visit(ForLoop n);    // <-- new
    R visit(BinaryOp n);
    R visit(UnaryOp n);
    R visit(NumberLit n);
    R visit(BoolLit n);
    R visit(Identifier n);
    R visit(PrintStmt n);
}
