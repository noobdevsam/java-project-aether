package aether.ast;

import aether.lexer.Token;

// assignment -> IDENTIFIER "=" assignment | equality
public record Assign(Token name, AST.Expr value) implements AST.Expr {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitAssignExpr(this);
    }
}
