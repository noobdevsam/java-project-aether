package aether;

import aether.ast.*;
import aether.lexer.Token;
import aether.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * A recursive descent parser that parses a stream of Aether tokens
 * into an Abstract Syntax Tree (AST) representing the program structure.
 */
public class Parser {

    /**
     * Special exception to unwind the parser stack upon encountering syntax errors.
     */
    private static class ParseError extends RuntimeException {
    }

    /** The list of scanned tokens to be parsed. */
    private final List<Token> tokens;

    /** The index pointing to the current token being analyzed. */
    private int current = 0;

    /**
     * Constructs a Parser with the given token stream.
     *
     * @param tokens the list of tokens to parse
     */
    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /**
     * Starts parsing the token stream into a list of AST statement nodes.
     *
     * @return a list of statement nodes representing the parsed program
     */
    public List<AST.Stmt> parse() {
        List<AST.Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    /**
     * Parses a declaration (variable declaration or standard statement).
     * Attempts syntax recovery if a ParseError occurs.
     *
     * @return the parsed statement node, or null if an error was caught and synchronized
     */
    private AST.Stmt declaration() {
        try {
            if (match(TokenType.MANIFEST)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    /**
     * Parses a variable declaration: "manifest" IDENTIFIER ("=" expression)? ";"
     *
     * @return the VarDecl statement node
     */
    private AST.Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

        AST.Expr initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new VarDecl(name, initializer);
    }

    /**
     * Parses a single statement (if, print, while, block, or expression statement).
     *
     * @return the statement node
     */
    private AST.Stmt statement() {
        if (match(TokenType.FLUX)) return ifStatement();
        if (match(TokenType.REVEAL)) return printStatement();
        if (match(TokenType.CYCLE)) return whileStatement();
        if (match(TokenType.LEFT_BRACE)) return new Block(block());

        return expressionStatement();
    }

    /**
     * Parses a block: "{" declaration* "}"
     *
     * @return a list of nested statements
     */
    private List<AST.Stmt> block() {
        List<AST.Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    /**
     * Parses an if/conditional statement: "flux" "(" expression ")" statement ("else" statement)?
     *
     * @return the Flux statement node
     */
    private AST.Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'flux'.");
        AST.Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after flux condition.");

        AST.Stmt thenBranch = statement();
        AST.Stmt elseBranch = null;
        if (match(TokenType.ELSE)) {
            elseBranch = statement();
        }

        return new Flux(condition, thenBranch, elseBranch);
    }

    /**
     * Parses a print statement: "reveal" expression ";"
     *
     * @return the Reveal statement node
     */
    private AST.Stmt printStatement() {
        AST.Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Reveal(value);
    }

    /**
     * Parses a loop statement: "cycle" "(" expression ")" statement
     *
     * @return the Cycle statement node
     */
    private AST.Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'cycle'.");
        AST.Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after cycle condition.");
        AST.Stmt body = statement();

        return new Cycle(condition, body);
    }

    /**
     * Parses an expression statement: expression ";"
     *
     * @return the ExpressionStmt node
     */
    private AST.Stmt expressionStatement() {
        AST.Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new ExpressionStmt(expr);
    }

    /**
     * Parses an expression. In our hierarchy, starts at assignment.
     *
     * @return the expression node
     */
    private AST.Expr expression() {
        return assignment();
    }

    /**
     * Parses an assignment: IDENTIFIER "=" assignment | equality
     *
     * @return the assignment or equality node
     */
    private AST.Expr assignment() {
        AST.Expr expr = equality();

        if (match(TokenType.ASSIGN)) {
            Token equals = previous();
            AST.Expr value = assignment();

            if (expr instanceof Variable(Token name)) {
                return new Assign(name, value);
            }

            error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    /**
     * Parses equality comparisons: comparison (("!=" | "==") comparison)*
     *
     * @return the parsed expression
     */
    private AST.Expr equality() {
        AST.Expr expr = comparison();

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            AST.Expr right = comparison();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parses comparisons: term ((">" | ">=" | "<" | "<=") term)*
     *
     * @return the parsed expression
     */
    private AST.Expr comparison() {
        AST.Expr expr = term();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            AST.Expr right = term();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parses terms: factor (("-" | "+") factor)*
     *
     * @return the parsed expression
     */
    private AST.Expr term() {
        AST.Expr expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            AST.Expr right = factor();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parses factors: unary (("/" | "*") unary)*
     *
     * @return the parsed expression
     */
    private AST.Expr factor() {
        AST.Expr expr = unary();

        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            AST.Expr right = unary();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    /**
     * Parses unary operators: ("!" | "-") unary | primary
     *
     * @return the parsed expression
     */
    private AST.Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            AST.Expr right = unary();
            return new Unary(operator, right);
        }

        return primary();
    }

    /**
     * Parses primary expressions (literals, variables, grouping, or array literals).
     *
     * @return the parsed primary expression node
     * @throws ParseError if no matching expression can be parsed
     */
    private AST.Expr primary() {
        if (match(TokenType.FALSE)) return new Literal(false);
        if (match(TokenType.TRUE)) return new Literal(true);
        if (match(TokenType.NIL)) return new Literal(null);

        if (match(TokenType.NUMBER, TokenType.STRING)) {
            return new Literal(previous().literal());
        }

        if (match(TokenType.IDENTIFIER)) {
            return new Variable(previous());
        }

        if (match(TokenType.LEFT_PAREN)) {
            AST.Expr expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return expr;
        }

        if (match(TokenType.LEFT_BRACKET)) {
            List<AST.Expr> elements = new ArrayList<>();
            if (!check(TokenType.RIGHT_BRACKET)) {
                do {
                    elements.add(expression());
                } while (match(TokenType.COMMA));
            }
            consume(TokenType.RIGHT_BRACKET, "Expect ']' after array literal.");
            return new ArrayLiteral(elements);
        }

        throw error(peek(), "Expect expression.");
    }

    /**
     * Checks if the current token matches any of the specified types.
     * Consumes the token if it matches.
     *
     * @param types the token types to check against
     * @return true if matches and consumed, false otherwise
     */
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    /**
     * Consumes the current token if it matches the expected type; otherwise throws an error.
     *
     * @param type    the expected token type
     * @param message the error message to display if not matched
     * @return the consumed token
     * @throws ParseError if not matched
     */
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    /**
     * Checks if the current token matches the given type without consuming it.
     *
     * @param type the token type to check
     * @return true if the token type matches, false otherwise
     */
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    /**
     * Advances the pointer to the next token and returns the previous one.
     *
     * @return the previous token
     */
    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    /**
     * Checks if the parser has consumed all tokens in the stream.
     *
     * @return true if EOF reached, false otherwise
     */
    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    /**
     * Looks at the current token without consuming it.
     *
     * @return the current token
     */
    private Token peek() {
        return tokens.get(current);
    }

    /**
     * Returns the token just consumed.
     *
     * @return the previous token
     */
    private Token previous() {
        return tokens.get(current - 1);
    }

    /**
     * Logs a syntax error message and returns a ParseError exception.
     *
     * @param token   the token at which the error occurred
     * @param message the explanation of the error
     * @return the ParseError exception to unwind stack
     */
    private ParseError error(Token token, String message) {
        System.err.printf("[Line %d] Error at '%s': %s%n", token.line(), token.lexeme(), message);
        return new ParseError();
    }

    /**
     * Synchronizes parser state after a syntax error to restart parsing at
     * the next declaration boundary. Prevents cascading parser error messages.
     */
    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type() == TokenType.SEMICOLON) return;

            switch (peek().type()) {
                case FLUX, CYCLE, REVEAL, MANIFEST -> {
                    return;
                }
                default -> advance();
            }
        }
    }
}
