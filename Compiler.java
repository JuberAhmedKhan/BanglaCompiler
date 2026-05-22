import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

// ── Custom exception types ─────────────────────────────────────────────────────
class LexicalError  extends RuntimeException { public LexicalError(String msg)  { super(msg); } }
class ParseError    extends RuntimeException { public ParseError(String msg)    { super(msg); } }
class SemanticError extends RuntimeException { public SemanticError(String msg) { super(msg); } }

// ── Compiler entry point ───────────────────────────────────────────────────────
public class Compiler {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java Compiler <source_file.bng> [output_file.py]");
            System.exit(1);
        }

        String sourcePath = args[0];
        String outputPath = (args.length > 1) ? args[1] : "target_code.py";

        try {
            System.out.println("--- Starting Compilation: " + sourcePath + " ---");

            
            System.out.println("[1/4] Tokenizing...");
            String sourceCode = new String(Files.readAllBytes(Paths.get(sourcePath)));
            Lexer lexer = new Lexer(sourceCode);
            List<Token> tokens = lexer.tokenize();

            
            System.out.println("[2/4] Parsing Syntax...");
            Parser parser = new Parser(tokens);
            Program ast = parser.parse();

            
            System.out.println("[3/4] Checking Semantics...");
            SemanticAnalyzer analyzer = new SemanticAnalyzer();
            analyzer.analyze(ast);

            
            System.out.println("[4/4] Generating Python Code → " + outputPath);
            CodeGenerator generator = new CodeGenerator();
            String pythonCode = generator.generate(ast);
            Files.write(Paths.get(outputPath), pythonCode.getBytes());

            System.out.println("--- Compilation Successful! ---");
            System.out.println("Run your code with:  python3 " + outputPath);

        } catch (IOException e) {
            System.err.println("File Error: " + e.getMessage());
        } catch (LexicalError | ParseError | SemanticError e) {
            System.err.println("\n" + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Compiler error:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
