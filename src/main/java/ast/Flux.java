package ast;

// ifStmt -> "flux" "(" expression ")" statement ( "else" statement )?
public record Flux(AST.Expr condition, AST.Stmt thenBranch, AST.Stmt elseBranch) implements AST.Stmt {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitFluxStmt(this);
    }
}