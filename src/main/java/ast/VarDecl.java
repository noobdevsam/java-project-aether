package ast;

import lexer.Token;

// varDecl -> "manifest" IDENTIFIER ( "=" expression )? ";"
public record VarDecl(Token name, AST.Expr initializer) implements AST.Stmt {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitVarDeclStmt(this);
    }
}
