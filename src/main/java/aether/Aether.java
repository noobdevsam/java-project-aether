package aether;

import aether.ast.AST;
import aether.lexer.Lexer;
import aether.lexer.Token;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Aether {
    private static final Interpreter interpreter = new Interpreter();
    // Define your custom language extension here
    private static final String FILE_EXTENSION = ".aether";

    static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: java -cp target/classes aether.Aether [script" + FILE_EXTENSION + "]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        // Enforce the custom file extension constraint
        if (!path.endsWith(FILE_EXTENSION)) {
            System.err.println("Error: Invalid file format. Aether source files must end with '" + FILE_EXTENSION + "'.");
            System.exit(65); // EX_DATAERR: Input data was incorrect in some way
        }

        // 2. Define the base directory where your scripts are allowed
        Path baseDir = Paths.get("scripts").toAbsolutePath().normalize();

        // 3. Resolve the requested path against the base directory
        Path targetPath = baseDir.resolve(path).normalize();

        // 4. Vulnerability Check: Ensure the resolved path starts with the base directory
        if (!targetPath.startsWith(baseDir)) {
            throw new SecurityException("Access denied: File path outside of allowed directory.");
        }

        // 5. Proceed with execution
        byte[] bytes = Files.readAllBytes(targetPath);
        run(new String(bytes));
    }

    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("aether> ");
            String line = reader.readLine();
            if (line == null) break;
            run(line);
        }
    }

    private static void run(String source) {
        if (source.trim().isEmpty()) return;
        try {
            Lexer lexer = new Lexer(source);
            List<Token> tokens = lexer.scanTokens();
            Parser parser = new Parser(tokens);
            List<AST.Stmt> statements = parser.parse();

            if (statements == null || statements.contains(null)) {
                return;
            }

            interpreter.interpret(statements);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}