package aether.ast;

import aether.lexer.Token;

public record Variable(Token name) implements AST.Expr {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitVariableExpr(this);
    }
}
