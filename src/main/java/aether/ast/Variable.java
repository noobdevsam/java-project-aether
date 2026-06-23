package aether.ast;

public record Variable() implements AST.Expr {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitVariableExpr(this);
    }
}
