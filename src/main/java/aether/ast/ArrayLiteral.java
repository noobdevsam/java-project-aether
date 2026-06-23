package aether.ast;

public record ArrayLiteral() implements AST.Expr {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitArrayLiteralExpr(this);
    }
}
