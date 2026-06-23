package aether.ast;

public record Literal() implements AST.Expr {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitLiteralExpr(this);
    }
}
