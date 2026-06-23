package aether.ast;

import java.util.List;

// block -> "{" declaration* "}"
public record Block(List<AST.Stmt> statements) implements AST.Stmt {
    @Override
    public <R> R accept(AST.Visitor<R> visitor) {
        return visitor.visitBlockStmt(this);
    }
}