package aether.ast;

// exprStmt -> expression ";"
public record ExpressionStmt(AST.Expr expression) implements AST.Stmt {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitExpressionStmt(this);
    }
}
