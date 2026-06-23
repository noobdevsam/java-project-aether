package aether;

import aether.ast.*;
import aether.lexer.Token;
import aether.lexer.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<AST.Stmt> parse() {
        List<AST.Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private AST.Stmt declaration() {
        try {
            if (match(TokenType.MANIFEST)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private AST.Stmt varDeclaration() {
        Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

        AST.Expr initializer = null;
        if (match(TokenType.ASSIGN)) {
            initializer = expression();
        }

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new VarDecl(name, initializer);
    }

    private AST.Stmt statement() {
        if (match(TokenType.FLUX)) return ifStatement();
        if (match(TokenType.REVEAL)) return printStatement();
        if (match(TokenType.CYCLE)) return whileStatement();
        if (match(TokenType.LEFT_BRACE)) return new Block(block());

        return expressionStatement();
    }

    private List<AST.Stmt> block() {
        List<AST.Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }

        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

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

    private AST.Stmt printStatement() {
        AST.Expr value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Reveal(value);
    }

    private AST.Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'cycle'.");
        AST.Expr condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after cycle condition.");
        AST.Stmt body = statement();

        return new Cycle(condition, body);
    }

    private AST.Stmt expressionStatement() {
        AST.Expr expr = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new ExpressionStmt(expr);
    }

    private AST.Expr expression() {
        return assignment();
    }

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

    private AST.Expr equality() {
        AST.Expr expr = comparison();

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            Token operator = previous();
            AST.Expr right = comparison();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private AST.Expr comparison() {
        AST.Expr expr = term();

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            Token operator = previous();
            AST.Expr right = term();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private AST.Expr term() {
        AST.Expr expr = factor();

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            Token operator = previous();
            AST.Expr right = factor();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private AST.Expr factor() {
        AST.Expr expr = unary();

        while (match(TokenType.SLASH, TokenType.STAR)) {
            Token operator = previous();
            AST.Expr right = unary();
            expr = new Binary(expr, operator, right);
        }

        return expr;
    }

    private AST.Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = previous();
            AST.Expr right = unary();
            return new Unary(operator, right);
        }

        return primary();
    }

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

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        System.err.printf("[Line %d] Error at '%s': %s%n", token.line(), token.lexeme(), message);
        return new ParseError();
    }

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
