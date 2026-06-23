package aether.interpreter;

import aether.ast.*;
import aether.lexer.Token;

import java.util.ArrayList;
import java.util.List;

public class Interpreter implements AST.Visitor<Object> {
    private Environment environment = new Environment();

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

    private void execute(AST.Stmt stmt) {
        stmt.accept(this);
    }

    private Object evaluate(AST.Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitVarDeclStmt(VarDecl stmt) {
        Object value = null;
        if (stmt.initializer() != null) {
            value = evaluate(stmt.initializer());
        }

        environment.define(stmt.name().lexeme(), value);
        return null;
    }

    @Override
    public Object visitExpressionStmt(ExpressionStmt stmt) {
        evaluate(stmt.expression());
        return null;
    }

    @Override
    public Object visitBlockStmt(Block stmt) {
        executeBlock(stmt.statements(), new Environment(environment));
        return null;
    }

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

    @Override
    public Object visitCycleStmt(Cycle stmt) {
        while (isTruthy(evaluate(stmt.condition()))) {
            if (stmt.body() != null) {
                execute(stmt.body());
            }
        }
        return null;
    }

    @Override
    public Object visitRevealStmt(Reveal stmt) {
        Object value = evaluate(stmt.expression());
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Object visitAssignExpr(Assign expr) {
        Object value = evaluate(expr.value());
        environment.assign(expr.name(), value);
        return value;
    }

    @Override
    public Object visitVariableExpr(Variable expr) {
        return environment.get(expr.name());
    }

    @Override
    public Object visitLiteralExpr(Literal expr) {
        return expr.value();
    }

    @Override
    public Object visitArrayLiteralExpr(ArrayLiteral expr) {
        List<Object> elements = new ArrayList<>();
        for (AST.Expr element : expr.elements()) {
            elements.add(evaluate(element));
        }
        return elements;
    }

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
            default -> {}
        }

        return null;
    }

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
            default -> {}
        }

        return null;
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean b) return b;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeException("Operand must be a number at line " + operator.line() + ".");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeException("Operands must be numbers at line " + operator.line() + ".");
    }

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
