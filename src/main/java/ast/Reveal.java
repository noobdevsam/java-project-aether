package ast;

// printStmt -> "reveal" expression ";"
public record Reveal(AST.Expr expression) implements AST.Stmt {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitRevealStmt(this);
    }
}