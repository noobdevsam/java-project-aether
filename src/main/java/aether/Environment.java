package aether;

import aether.lexer.Token;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages lexical scope, variable storage, and variable resolution
 * during the interpretation of Aether source programs.
 * Supports nesting environments for local scopes (blocks).
 */
public class Environment {
    /**
     * The parent/enclosing environment scope, or null if this is the global scope.
     */
    public final Environment enclosing;

    /** Stores the mapping from variable names (identifiers) to their evaluated runtime values. */
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Constructs a global environment scope with no enclosing scope.
     */
    public Environment() {
        this.enclosing = null;
    }

    /**
     * Constructs a nested environment scope enclosed by the specified parent scope.
     *
     * @param enclosing the parent environment scope
     */
    public Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    /**
     * Retrieves the value of a variable defined in this scope or its enclosing parent scopes.
     *
     * @param name the token representing the variable name
     * @return the value associated with the variable
     * @throws RuntimeException if the variable is not defined in this scope or enclosing scopes
     */
    public Object get(Token name) {
        if (values.containsKey(name.lexeme())) {
            return values.get(name.lexeme());
        }

        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeException("Undefined variable '" + name.lexeme() + "' at line " + name.line() + ".");
    }

    /**
     * Defines a new variable in the current scope.
     * Overwrites any existing definition in the current scope level.
     *
     * @param name  the name of the variable to define
     * @param value the initial value of the variable
     */
    public void define(String name, Object value) {
        values.put(name, value);
    }

    /**
     * Assigns a new value to an existing variable in this scope or its enclosing parent scopes.
     * Does not define a new variable.
     *
     * @param name  the token representing the variable name
     * @param value the new value to assign
     * @throws RuntimeException if the variable is not defined in this scope or enclosing scopes
     */
    public void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme())) {
            values.put(name.lexeme(), value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeException("Undefined variable '" + name.lexeme() + "' at line " + name.line() + ".");
    }
}
