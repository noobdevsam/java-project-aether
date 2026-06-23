package ast;

// whileStmt -> "cycle" "(" expression ")" statement
public record Cycle(AST.Expr condition, AST.Stmt body) implements AST.Stmt {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitCycleStmt(this);
    }
}