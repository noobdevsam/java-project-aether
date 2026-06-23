package aether;

import aether.lexer.Lexer;
import aether.lexer.Token;
import aether.ast.AST;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Aether {
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: java -cp target/classes aether.Aether [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
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
