package aether.ast;

public final class AST {
    private AST() {
    } // Prevent instantiation

    // Base Visitor Interface
    public interface Visitor<R> {
        // Expression Visits
        R visitAssignExpr(Assign expr);

        R visitBinaryExpr(Binary expr);

        R visitUnaryExpr(Unary expr);

        R visitLiteralExpr(Literal expr);

        R visitVariableExpr(Variable expr);

        R visitArrayLiteralExpr(ArrayLiteral expr);

        // Statement Visits
        R visitBlockStmt(Block stmt);

        R visitExpressionStmt(ExpressionStmt stmt);

        R visitFluxStmt(Flux stmt);

        R visitCycleStmt(Cycle stmt);

        R visitRevealStmt(Reveal stmt);

        R visitVarDeclStmt(VarDecl stmt);
    }

    // Root Expression Type
    public sealed interface Expr permits Assign, Binary, Unary, Literal, Variable, ArrayLiteral {
        <R> R accept(Visitor<R> visitor);
    }

    // Root Statement Type
    public sealed interface Stmt permits Block, ExpressionStmt, Flux, Cycle, Reveal, VarDecl {
        <R> R accept(Visitor<R> visitor);
    }
}
