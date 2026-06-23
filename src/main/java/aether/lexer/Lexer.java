package aether.lexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lexer for the Aether programming language.
 * <p>
 * This class is responsible for tokenizing source code into a stream of tokens.
 * It performs lexical analysis by scanning the input source character by character,
 * recognizing keywords, identifiers, literals, and operators, and converting them
 * into Token objects.
 * <p>
 * The lexer maintains state about the current position in the source, the current
 * line number, and builds a list of tokens as it scans the input.
 *
 * @author Aether Project
 */
public class Lexer {
    /** The source code to be tokenized */
    private final String source;

    /** List of tokens generated during lexical analysis */
    private final List<Token> tokens = new ArrayList<>();

    /** Starting position of the current token being scanned */
    private int start = 0;

    /** Current position in the source code */
    private int current = 0;

    /** Current line number for error reporting */
    private int line = 1;

    /**
     * Map of reserved keywords to their corresponding token types.
     * Used during identifier recognition to distinguish keywords from identifiers.
     */
    private static final Map<String, TokenType> keywords = Map.of(
            "manifest", TokenType.MANIFEST,
            "flux", TokenType.FLUX,
            "else", TokenType.ELSE,
            "cycle", TokenType.CYCLE,
            "reveal", TokenType.REVEAL,
            "true", TokenType.TRUE,
            "false", TokenType.FALSE,
            "nil", TokenType.NIL
    );

    /**
     * Constructs a Lexer with the given source code.
     *
     * @param source the source code string to tokenize
     */
    public Lexer(String source) {
        this.source = source;
    }

    /**
     * Scans the entire source code and returns a list of tokens.
     *
     * This is the main entry point for lexical analysis. It repeatedly calls
     * scanToken() until the end of the source is reached, then appends an EOF token.
     *
     * @return a list of Token objects representing the scanned source code
     * @throws RuntimeException if an unexpected character or unterminated string is encountered
     */
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "", null, line));
        return tokens;
    }

    /**
     * Checks if the lexer has reached the end of the source code.
     *
     * @return true if the current position is at or past the end of the source, false otherwise
     */
    private boolean isAtEnd() {
        return current >= source.length();
    }

    /**
     * Scans a single token from the source code.
     *
     * This method reads one character and dispatches to the appropriate handler
     * based on the character type (operator, keyword, literal, etc.).
     * Single-line comments (starting with //) are also handled here.
     *
     * @throws RuntimeException if an unexpected character is encountered
     */
    private void scanToken() {
        char c = advance();
        switch (c) {
            case '(' -> addToken(TokenType.LEFT_PAREN);
            case ')' -> addToken(TokenType.RIGHT_PAREN);
            case '{' -> addToken(TokenType.LEFT_BRACE);
            case '}' -> addToken(TokenType.RIGHT_BRACE);
            case '[' -> addToken(TokenType.LEFT_BRACKET);
            case ']' -> addToken(TokenType.RIGHT_BRACKET);
            case ',' -> addToken(TokenType.COMMA);
            case '-' -> addToken(TokenType.MINUS);
            case '+' -> addToken(TokenType.PLUS);
            case ';' -> addToken(TokenType.SEMICOLON);
            case '*' -> addToken(TokenType.STAR);
            case '!' -> addToken(match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
            case '=' -> addToken(match('=') ? TokenType.EQUAL_EQUAL : TokenType.ASSIGN);
            case '<' -> addToken(match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
            case '>' -> addToken(match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
            case '/' -> {
                if (match('/')) {
                    // Single-line comment; skip until end of line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(TokenType.SLASH);
                }
            }
            case ' ', '\r', '\t' -> {
                // Whitespace is ignored
            }
            case '\n' -> line++;
            case '"' -> string();
            default -> {
                if (Character.isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw new RuntimeException("Unexpected character at line " + line + ": " + c);
                }
            }
        }
    }

    /**
     * Determines if the given character is alphabetic or underscore.
     *
     * Underscores are considered valid identifier start characters.
     *
     * @param c the character to check
     * @return true if the character is a-z, A-Z, or underscore, false otherwise
     */
    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    /**
     * Determines if the given character is alphanumeric or underscore.
     *
     * @param c the character to check
     * @return true if the character is alphabetic, numeric, or underscore, false otherwise
     */
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || Character.isDigit(c);
    }

    /**
     * Conditionally advances if the current character matches the expected character.
     *
     * This is used for recognizing two-character operators such as ==, !=, <=, >=.
     *
     * @param expected the character expected at the current position
     * @return true if the character matched and the position was advanced, false otherwise
     */
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    /**
     * Adds a token of the given type to the token list with no literal value.
     *
     * @param tokenType the type of the token to add
     */
    private void addToken(TokenType tokenType) {
        addToken(tokenType, null);
    }

    /**
     * Adds a token of the given type and optional literal value to the token list.
     *
     * The token text is extracted from the source between the start and current positions.
     *
     * @param tokenType the type of the token to add
     * @param literal the literal value associated with the token, or null if none
     */
    private void addToken(TokenType tokenType, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(tokenType, text, literal, line));
    }

    /**
     * Consumes and returns the character at the current position, then advances.
     *
     * @return the character at the current position before advancing
     */
    private char advance() {
        return source.charAt(current++);
    }

    /**
     * Scans an identifier or keyword.
     *
     * Identifiers may contain letters, digits, and underscores after the initial character.
     * If the identifier matches a reserved keyword, the appropriate keyword token type is used.
     * Otherwise, an IDENTIFIER token is created.
     */
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();
        String text = source.substring(start, current);
        TokenType type = keywords.getOrDefault(text, TokenType.IDENTIFIER);
        addToken(type);
    }

    /**
     * Returns the character at the current position without advancing.
     *
     * @return the character at the current position, or '\0' if at end of source
     */
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    /**
     * Scans a numeric literal (integer or floating-point).
     *
     * Recognizes:
     * - Integer sequences (e.g., 123)
     * - Floating-point numbers with decimal point (e.g., 123.456)
     *
     * The numeric value is parsed as a Double and stored as the literal value.
     */
    private void number() {
        while (Character.isDigit(peek())) advance();
        if (peek() == '.' && Character.isDigit(peekNext())) {
            advance();
            while (Character.isDigit(peek())) advance();
        }
        addToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    /**
     * Returns the character at the position following the current position without advancing.
     *
     * @return the character one position ahead of current, or '\0' if at or past end of source
     */
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    /**
     * Scans a string literal delimited by double quotes.
     *
     * Supports multi-line strings; line numbers are tracked while scanning.
     * The closing double quote must be present, or a RuntimeException is thrown.
     * The string literal value (without quotes) is stored as the token's literal value.
     *
     * @throws RuntimeException if the string is not terminated with a closing double quote
     */
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            throw new RuntimeException("Unterminated string.");
        }
        advance(); // The closing "
        String value = source.substring(start + 1, current - 1);
        addToken(TokenType.STRING, value);
    }
}