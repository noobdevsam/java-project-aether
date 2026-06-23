package aether.ast;

import aether.lexer.Token;

// equality, comparison, term, factor rules
public record Binary(AST.Expr left, Token operator, AST.Expr right) implements AST.Expr {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitBinaryExpr(this);
    }
}
