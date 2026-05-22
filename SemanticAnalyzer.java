import java.util.*;

class SemanticAnalyzer implements Visitor<String> {

    private final Deque<Map<String, String>> scopes = new ArrayDeque<>();

    public SemanticAnalyzer() {
        scopes.push(new HashMap<>()); // global scope
    }

    public void analyze(Program p) { visit(p); }

    // ── Dispatcher ─────────────────────────────────────────────────────────────
    private String dispatch(Node n) {
        if (n == null)                  return "void";
        if (n instanceof Program)       return visit((Program) n);
        if (n instanceof VarDecl)       return visit((VarDecl) n);
        if (n instanceof Assignment)    return visit((Assignment) n);
        if (n instanceof IfElse)        return visit((IfElse) n);
        if (n instanceof ForLoop)       return visit((ForLoop) n); 
        if (n instanceof BinaryOp)      return visit((BinaryOp) n);
        if (n instanceof NumberLit)     return visit((NumberLit) n);
        if (n instanceof BoolLit)       return visit((BoolLit) n);
        if (n instanceof Identifier)    return visit((Identifier) n);
        if (n instanceof PrintStmt)     return visit((PrintStmt) n);
        return "void";
    }

    @Override
    public String visit(Program n) {
        for (Stmt s : n.statements) dispatch(s);
        return "void";
    }

    // Grammar: varDecl type-checking
    @Override
    public String visit(VarDecl n) {
        if (scopes.peek().containsKey(n.name))
            throw new RuntimeException("Semantic Error: Variable '" + n.name + "' already declared at line " + n.line);
        String valueType = dispatch(n.value);
        if (!valueType.equals(n.typeName))
            throw new RuntimeException("Type Mismatch: Cannot assign " + valueType + " to " + n.typeName + " at line " + n.line);
        scopes.peek().put(n.name, n.typeName);
        return n.typeName;
    }

    @Override
    public String visit(Assignment n) {
        String type = lookupVariable(n.name);
        if (type == null)
            throw new RuntimeException("Semantic Error: Undeclared variable '" + n.name + "' at line " + n.line);
        String valueType = dispatch(n.value);
        if (!type.equals(valueType))
            throw new RuntimeException("Type Mismatch: Cannot assign " + valueType + " to " + type + " at line " + n.line);
        return type;
    }

    @Override
    public String visit(IfElse n) {
        String condType = dispatch(n.cond);
        if (!condType.equals("bool"))
            throw new RuntimeException("Type Error: 'if' condition must be boolean at line " + n.line);
        scopes.push(new HashMap<>());
        for (Stmt s : n.thenB) dispatch(s);
        scopes.pop();
        if (n.elseB != null) {
            scopes.push(new HashMap<>());
            for (Stmt s : n.elseB) dispatch(s);
            scopes.pop();
        }
        return "void";
    }

    // Grammar: forStmt – loop variable is implicitly int; start/end/step must be int
    @Override
    public String visit(ForLoop n) {
        // Validate start, end, step expressions are all int
        String startType = dispatch(n.start);
        String endType   = dispatch(n.end);
        if (!startType.equals("int"))
            throw new RuntimeException("Type Error: for-loop start must be int at line " + n.line);
        if (!endType.equals("int"))
            throw new RuntimeException("Type Error: for-loop end must be int at line " + n.line);
        if (n.step != null) {
            String stepType = dispatch(n.step);
            if (!stepType.equals("int"))
                throw new RuntimeException("Type Error: for-loop step must be int at line " + n.line);
        }

        // Create a new scope; declare loop variable as int inside it
        scopes.push(new HashMap<>());
        scopes.peek().put(n.varName, "int");
        for (Stmt s : n.body) dispatch(s);
        scopes.pop();
        return "void";
    }

    @Override
    public String visit(BinaryOp n) {
        String leftType  = dispatch(n.left);
        String rightType = dispatch(n.right);
        if (!leftType.equals(rightType))
            throw new RuntimeException("Type Mismatch: '" + n.op + "' operands must match at line " + n.line);
        if (n.op.matches("==|!=|<|>|<=|>=")) return "bool";
        return "int";
    }

    @Override
    public String visit(Identifier n) {
        String type = lookupVariable(n.name);
        if (type == null)
            throw new RuntimeException("Semantic Error: Variable '" + n.name + "' is not defined.");
        return type;
    }

    @Override
    public String visit(PrintStmt n) { dispatch(n.expression); return "void"; }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private String lookupVariable(String name) {
        for (Map<String, String> scope : scopes) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }

    @Override public String visit(UnaryOp n)  { return "void"; }
    @Override public String visit(NumberLit n) { return "int"; }
    @Override public String visit(BoolLit n)   { return "bool"; }
}
