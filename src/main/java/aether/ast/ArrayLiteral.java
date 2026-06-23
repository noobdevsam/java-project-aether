package aether.ast;

import java.util.List;

public record ArrayLiteral(List<AST.Expr> elements) implements AST.Expr {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitArrayLiteralExpr(this);
    }
}
