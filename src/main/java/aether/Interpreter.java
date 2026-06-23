package aether;

import aether.ast.*;
import aether.lexer.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes Aether programs by traversing the Abstract Syntax Tree (AST)
 * utilizing the Visitor pattern. Resolves variables, evaluates expressions,
 * and maintains environment scopes.
 */
public class Interpreter implements AST.Visitor<Object> {

    /**
     * The active execution scope holding variables and values.
     */
    private Environment environment = new Environment();

    /**
     * Interprets a list of parsed statements, catching and logging runtime errors.
     *
     * @param statements the list of AST statements to execute
     */
    public void interpret(List<AST.Stmt> statements) {
        try {
            for (AST.Stmt statement : statements) {
                if (statement != null) {
                    execute(statement);
                }
            }
        } catch (RuntimeException error) {
            System.err.println("Runtime Error: " + error.getMessage());
        }
    }

    /**
     * Helper method to execute a statement node.
     *
     * @param stmt the statement to execute
     */
    private void execute(AST.Stmt stmt) {
        stmt.accept(this);
    }

    /**
     * Helper method to evaluate an expression node.
     *
     * @param expr the expression to evaluate
     * @return the runtime value resulting from evaluation
     */
    private Object evaluate(AST.Expr expr) {
        return expr.accept(this);
    }

    /**
     * Visits a variable declaration statement. Evaluates the initializer if present
     * and registers the variable in the environment.
     *
     * @param stmt the variable declaration statement
     * @return null
     */
    @Override
    public Object visitVarDeclStmt(VarDecl stmt) {
        Object value = null;
        if (stmt.initializer() != null) {
            value = evaluate(stmt.initializer());
        }

        environment.define(stmt.name().lexeme(), value);
        return null;
    }

    /**
     * Visits an expression statement. Evaluates the wrapped expression.
     *
     * @param stmt the expression statement
     * @return null
     */
    @Override
    public Object visitExpressionStmt(ExpressionStmt stmt) {
        evaluate(stmt.expression());
        return null;
    }

    /**
     * Visits a block statement. Creates a nested local scope and executes all inner statements.
     *
     * @param stmt the block statement
     * @return null
     */
    @Override
    public Object visitBlockStmt(Block stmt) {
        executeBlock(stmt.statements(), new Environment(environment));
        return null;
    }

    /**
     * Executes a list of statements in the context of a specific Environment scope.
     * Guarantees restoration of the original environment upon completion.
     *
     * @param statements the statements inside the block
     * @param env        the new local Environment scope
     */
    public void executeBlock(List<AST.Stmt> statements, Environment env) {
        Environment previous = this.environment;
        try {
            this.environment = env;
            for (AST.Stmt statement : statements) {
                if (statement != null) {
                    execute(statement);
                }
            }
        } finally {
            this.environment = previous;
        }
    }

    /**
     * Visits an if (flux) statement. Evaluates the condition and branches execution accordingly.
     *
     * @param stmt the flux statement
     * @return null
     */
    @Override
    public Object visitFluxStmt(Flux stmt) {
        if (isTruthy(evaluate(stmt.condition()))) {
            if (stmt.thenBranch() != null) {
                execute(stmt.thenBranch());
            }
        } else if (stmt.elseBranch() != null) {
            execute(stmt.elseBranch());
        }
        return null;
    }

    /**
     * Visits a loop (cycle) statement. Executes the loop body repeatedly while the condition evaluates to truthy.
     *
     * @param stmt the cycle statement
     * @return null
     */
    @Override
    public Object visitCycleStmt(Cycle stmt) {
        while (isTruthy(evaluate(stmt.condition()))) {
            if (stmt.body() != null) {
                execute(stmt.body());
            }
        }
        return null;
    }

    /**
     * Visits a reveal (print) statement. Outputs the stringified representation of the evaluated expression.
     *
     * @param stmt the reveal statement
     * @return null
     */
    @Override
    public Object visitRevealStmt(Reveal stmt) {
        Object value = evaluate(stmt.expression());
        System.out.println(stringify(value));
        return null;
    }

    /**
     * Visits an assignment expression. Evaluates the value and updates the variable in the environment.
     *
     * @param expr the assignment expression
     * @return the assigned value
     */
    @Override
    public Object visitAssignExpr(Assign expr) {
        Object value = evaluate(expr.value());
        environment.assign(expr.name(), value);
        return value;
    }

    /**
     * Visits a variable lookup expression. Retrieves its value from the environment.
     *
     * @param expr the variable lookup expression
     * @return the value of the variable
     */
    @Override
    public Object visitVariableExpr(Variable expr) {
        return environment.get(expr.name());
    }

    /**
     * Visits a literal expression. Returns the literal value directly.
     *
     * @param expr the literal expression
     * @return the literal value
     */
    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.value();
    }

    /**
     * Visits an array literal expression. Evaluates all element expressions.
     *
     * @param expr the array literal expression
     * @return a List containing evaluated element values
     */
    @Override
    public Object visitArrayLiteralExpr(ArrayLiteral expr) {
        List<Object> elements = new ArrayList<>();
        for (AST.Expr element : expr.elements()) {
            elements.add(evaluate(element));
        }
        return elements;
    }

    /**
     * Visits a unary expression (! or -). Evaluates the right operand and applies the operator.
     *
     * @param expr the unary expression
     * @return the evaluated unary result
     */
    @Override
    public Object visitUnaryExpr(Unary expr) {
        Object right = evaluate(expr.right());

        switch (expr.operator().type()) {
            case BANG -> {
                return !isTruthy(right);
            }
            case MINUS -> {
                checkNumberOperand(expr.operator(), right);
                return -(double) right;
            }
            default -> {
            }
        }

        return null;
    }

    /**
     * Visits a binary expression (+, -, *, /, comparisons, equalities).
     * Evaluates both operands and applies the operation.
     *
     * @param expr the binary expression
     * @return the evaluated binary result
     */
    @Override
    public Object visitBinaryExpr(Binary expr) {
        Object left = evaluate(expr.left());
        Object right = evaluate(expr.right());

        switch (expr.operator().type()) {
            case GREATER -> {
                checkNumberOperands(expr.operator(), left, right);
                return (double) left > (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(expr.operator(), left, right);
                return (double) left >= (double) right;
            }
            case LESS -> {
                checkNumberOperands(expr.operator(), left, right);
                return (double) left < (double) right;
            }
            case LESS_EQUAL -> {
                checkNumberOperands(expr.operator(), left, right);
                return (double) left <= (double) right;
            }
            case BANG_EQUAL -> {
                return !isEqual(left, right);
            }
            case EQUAL_EQUAL -> {
                return isEqual(left, right);
            }
            case MINUS -> {
                checkNumberOperands(expr.operator(), left, right);
                return (double) left - (double) right;
            }
            case PLUS -> {
                if (left instanceof Double d1 && right instanceof Double d2) {
                    return d1 + d2;
                }
                if (left instanceof String s1 && right instanceof String s2) {
                    return s1 + s2;
                }
                throw new RuntimeException("Operands must be two numbers or two strings at line " + expr.operator().line() + ".");
            }
            case SLASH -> {
                checkNumberOperands(expr.operator(), left, right);
                double divisor = (double) right;
                if (divisor == 0) {
                    throw new RuntimeException("Division by zero at line " + expr.operator().line() + ".");
                }
                return (double) left / divisor;
            }
            case STAR -> {
                checkNumberOperands(expr.operator(), left, right);
                return (double) left * (double) right;
            }
            default -> {
            }
        }

        return null;
    }

    /**
     * Checks if the evaluated value is truthy (non-null and not Boolean.FALSE).
     *
     * @param object the object to check
     * @return true if truthy, false otherwise
     */
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean b) return b;
        return true;
    }

    /**
     * Checks if two values are equal.
     * Handles null values safely.
     *
     * @param a first operand
     * @param b second operand
     * @return true if equal, false otherwise
     */
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    /**
     * Validates that the operand is a number.
     *
     * @param operator the unary operator token (for line reporting)
     * @param operand  the operand to check
     * @throws RuntimeException if operand is not a number
     */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeException("Operand must be a number at line " + operator.line() + ".");
    }

    /**
     * Validates that both operands are numbers.
     *
     * @param operator the binary operator token
     * @param left     the left operand
     * @param right    the right operand
     * @throws RuntimeException if either operand is not a number
     */
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeException("Operands must be numbers at line " + operator.line() + ".");
    }

    /**
     * Converts a runtime object to its string representation for output.
     * Truncates decimal parts (.0) for whole numbers.
     *
     * @param object the runtime value
     * @return the stringified value
     */
    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double d) {
            String text = d.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}
