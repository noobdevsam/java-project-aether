package ast;

public record Unary() implements AST.Expr {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitUnaryExpr(this);
    }
}
