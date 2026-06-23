package aether.ast;

import aether.lexer.Token;

public record Unary(Token operator, AST.Expr right) implements AST.Expr {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitUnaryExpr(this);
    }
}
