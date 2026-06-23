# Aether

Aether is an ethereal, tree-walk interpreted, custom programming language implemented entirely in Java 25. It features a
custom recursive descent parser, block scoping with dynamically nested scopes, standard control-flow constructs, arrays,
and arithmetic operations.

---

## Language Specifications & Grammar

Aether programs are composed of statements and declarations parsed using recursive descent according to the following
formal BNF grammar:

* **program** $\rightarrow$ **declaration*** EOF
* **declaration** $\rightarrow$ **varDecl** | **statement**
* **varDecl** $\rightarrow$ `"manifest"` IDENTIFIER ( `"="` **expression** )? `";"`
* **statement** $\rightarrow$ **block** | **exprStmt** | **ifStmt** | **whileStmt** | **printStmt**
* **block** $\rightarrow$ `"{"` **declaration*** `"}"`
* **exprStmt** $\rightarrow$ **expression** `";"`
* **ifStmt** $\rightarrow$ `"flux"` `"("` **expression** `")"` **statement** ( `"else"` **statement** )?
* **whileStmt** $\rightarrow$ `"cycle"` `"("` **expression** `")"` **statement**
* **printStmt** $\rightarrow$ `"reveal"` **expression** `";"`
* **expression** $\rightarrow$ **assignment**
* **assignment** $\rightarrow$ IDENTIFIER `"="` **assignment** | **equality**
* **equality** $\rightarrow$ **comparison** ( ( `"!="` | `"=="` ) **comparison** )*
* **comparison** $\rightarrow$ **term** ( ( `">"` | `">="` | `"<"` | `"<="` ) **term** )*
* **term** $\rightarrow$ **factor** ( ( `"-"` | `"+"` ) **factor** )*
* **factor** $\rightarrow$ **unary** ( ( `"/"` | `"*"` ) **unary** )*
* **unary** $\rightarrow$ ( `"!"` | `"-"` ) **unary** | **primary**
* **primary** $\rightarrow$ NUMBER | STRING | `"true"` | `"false"` | `"nil"` | `"("` **expression** `")"` | IDENTIFIER |
  `"["` **arrayLiteral** `"]"`
* **arrayLiteral** $\rightarrow$ **expression** ( `","` **expression** )*

---

## Keywords & Syntax Syntax Cheat-Sheet

* **`manifest`**: Declares a variable.
  ```aether
  manifest greeting = "Hello, World!";
  manifest unchecked; // Defaults to nil
  ```
* **`reveal`**: Outputs evaluated expressions to standard output.
  ```aether
  reveal greeting;
  reveal 1 + 2 * 3;
  ```
* **`flux` / `else`**: Conditional branch execution.
  ```aether
  flux (x > 10) {
      reveal "greater than 10";
  } else {
      reveal "10 or less";
  }
  ```
* **`cycle`**: While-loop iteration control.
  ```aether
  manifest count = 0;
  cycle (count < 3) {
      count = count + 1;
      reveal count;
  }
  ```
* **`nil`**: Null/empty literal.
* **Arrays**: Array literals represent collection values.
  ```aether
  manifest scores = [90, 85, 95];
  reveal scores;
  ```

---

## Project Structure

* **`aether.lexer`**: Contains the Lexer, token representations, and enum definitions:
    * [`Token`](src/main/java/aether/lexer/Token.java) - Representation record of scanned tokens.
    * [`TokenType`](src/main/java/aether/lexer/TokenType.java) - Enum mapping standard Aether lexemes.
    * [`Lexer`](src/main/java/aether/lexer/Lexer.java) - Performs string tokenization.
* **`aether.ast`**: Holds the compiler AST definitions:
    * [`AST`](src/main/java/aether/ast/AST.java) - Visitor interface and base expression/statement interfaces.
    * Concrete node classes (e.g. `Binary`, `Literal`, `Block`, `Flux`, `Cycle`).
* **`aether` (root)**: Core parser, environment, interpreter, and application driver:
    * [`Environment`](src/main/java/aether/Environment.java) - Resolves and manages lexical variable scopes.
    * [`Parser`](src/main/java/aether/Parser.java) - Parses a Token list into an AST.
    * [`Interpreter`](src/main/java/aether/Interpreter.java) - Executes the AST.
    * [`Aether`](src/main/java/aether/Aether.java) - The application entrypoint supporting REPL and file execution.

---

## How to Build and Run

### Prerequisites

* Java JDK 25
* Apache Maven

### 1. Package the Source Code

Package the Maven project:

```bash
mvn clean package
```

### 2. Run in Interactive REPL Mode

Type code dynamically line-by-line:

```bash
java -jar target/java-project-aether-1.0-SNAPSHOT.jar
```

**Example Session:**

```
aether> manifest text = "Hello Aether!";
aether> reveal text;
Hello Aether!
aether> reveal [1, 2, 3];
[1.0, 2.0, 3.0]
```

### 3. Run a Source File

Execute a script file (e.g., creating a script file `test.aether`) [script files must be inside the
`scripts` folder inside the root directory of the project]:

```bash
java -jar java-project-aether-1.0-SNAPSHOT.jar hello.aether
```