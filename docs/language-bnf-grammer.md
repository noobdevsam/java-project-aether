### The Aether BNF Grammar

This grammar defines the hierarchy of your language, using `( )` for expression grouping/control flow, `{ }` for block
scopes, and `[ ]` for potential future indexing or collections.

* **program** $\rightarrow$ **declaration*** EOF
* **declaration** $\rightarrow$ **varDecl** | **statement**
* **varDecl** $\rightarrow$ "manifest" IDENTIFIER ( "=" **expression** )? ";"
* **statement** $\rightarrow$ **block** | **exprStmt** | **ifStmt** | **whileStmt** | **printStmt**
* **block** $\rightarrow$ "{" **declaration*** "}"
* **exprStmt** $\rightarrow$ **expression** ";"
* **ifStmt** $\rightarrow$ "flux" "(" **expression** ")" **statement** ( "else" **statement** )?
* **whileStmt** $\rightarrow$ "cycle" "(" **expression** ")" **statement**
* **printStmt** $\rightarrow$ "reveal" **expression** ";"
* **expression** $\rightarrow$ **assignment**
* **assignment** $\rightarrow$ IDENTIFIER "=" **assignment** | **equality**
* **equality** $\rightarrow$ **comparison** ( ( "!=" | "==" ) **comparison** )*
* **comparison** $\rightarrow$ **term** ( ( ">" | ">=" | "<" | "<=" ) **term** )*
* **term** $\rightarrow$ **factor** ( ( "-" | "+" ) **factor** )*
* **factor** $\rightarrow$ **unary** ( ( "/" | "*" ) **unary** )*
* **unary** $\rightarrow$ ( "!" | "-" ) **unary** | **primary**
* **primary** $\rightarrow$ NUMBER | STRING | "true" | "false" | "nil" | "(" **expression** ")" | IDENTIFIER | "[" *
  *arrayLiteral** "]"
* **arrayLiteral** $\rightarrow$ **expression** ( "," **expression** )*

---

### Implementation Notes for Your Parser

- **Block Scoping `{ }`**: The `block` rule is essential for local scoping. When your parser encounters `{`, it should
  enter a new `Environment` scope; when it hits `}`, it should pop back to the previous scope.

- **Delimiters in Java**: Ensure your `Lexer` identifies `LEFT_BRACE`, `RIGHT_BRACE`, `LEFT_PAREN`, `RIGHT_PAREN`,
  `LEFT_BRACKET`, and `RIGHT_BRACKET` as distinct token types.

- **Recursive Descent**: Remember that your `Parser.java` will need methods for each non-terminal above (e.g.,
  `parseBlock()`, `parseIfStatement()`).

- **Handling `[ ]`**: I have added an `arrayLiteral` rule to the `primary` expression. This allows `[1, 2, 3]` to be
  treated as a single unit within your AST.



