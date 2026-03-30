# CIS*4650 Checkpoint 3 (Winter 2026) README  
Group 25: Muhammad Ali, Muhammad Tameem Mughal, Richard Milovanov  
Repo: https://github.com/muhammadali636/c-comp

---

## WHAT THIS PROJECT DOES
This checkpoint builds the complete C- compiler:

- **Scanner (JFlex):** `CM.flex` -> `Lexer.java`
- **Parser (CUP):** `CM.cup` -> `parser.java` and `sym.java`
- Builds an **AST** using classes in `absyn/`
- Prints the AST with the **`-a`** option (ShowTreeVisitor)
- Builds **symbol tables** (nested scopes) and performs **type checking**
- Saves symbol tables with the **`-s`** option
- Generates **TM assembly code** for valid programs
- Saves TM assembly with the **`-c`** option
- Reports lexical, syntax, and semantic errors with line/column when available
- Handles runtime bounds checking for indexed array access in generated TM code

**Important rules:**  
If there are **syntax errors**, the compiler **stops after parsing** and does **not** run semantic analysis or code generation.  
If there are **semantic errors**, the compiler **stops after semantic analysis** and does **not** run code generation.

---

## FOLDER / FILE SUMMARY
absyn/                   AST node classes + AbsynVisitor  
CM.flex                  JFlex scanner rules (tokens + row/column + lexical errors)  
CM.cup                   CUP grammar (precedence + AST building + recovery rules)  
CM.java                  Driver program (runs parser, supports -a, -s, and -c)  
ShowTreeVisitor.java     Prints AST as an indented tree  
SemanticAnalyzer.java    Traverses AST (post-order) for symbol tables + type checking  
SymbolTable.java         Symbol table / scope stack structures (hash tables)  
CodeGenerator.java       Generates TM assembly code for valid C- programs  
tests/                   Required tests: 1.cm to 0.cm  
lib/                     JFlex and CUP jars  
makefile                 Build + clean rules  

---

## BUILD (on linux.socs.uoguelph.ca)
1) Build everything:
   make

2) Clean generated files and .class files:
   make clean

---

## RUN (linux.socs.uoguelph.ca)

Run without saving outputs:
- Parse + semantic analysis (if syntax is valid):
  java -cp ".:lib/java-cup-11b-runtime.jar" CM tests/1.cm

Save AST only:
  java -cp ".:lib/java-cup-11b-runtime.jar" CM -a tests/1.cm

Save symbol tables only:
  java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/1.cm

Save TM assembly only:
  java -cp ".:lib/java-cup-11b-runtime.jar" CM -c tests/1.cm

Save AST and symbol tables:
  java -cp ".:lib/java-cup-11b-runtime.jar" CM -a -s tests/1.cm

Save AST, symbol tables, and TM assembly:
  java -cp ".:lib/java-cup-11b-runtime.jar" CM -a -s -c tests/1.cm

**Output files:**
- For input `X.cm`, AST is saved to: `X.cm.ast`
- For input `X.cm`, symbol tables are saved to: `X.cm.sym`
- For input `X.cm`, TM assembly is saved to: `X.cm.tm`

**Note:** Output files are only saved if the program is valid in both **syntax** and **semantics**.  
TM code is only generated for programs that pass parsing and semantic analysis.

---

## TM SIMULATOR
Generated `.tm` files are meant to be run on the TM simulator provided on CourseLink.

Typical workflow:
1) Compile the compiler:
   `make`

2) Generate TM code:
   `java -cp ".:lib/java-cup-11b-runtime.jar" CM -c tests/1.cm`

3) Run the generated TM program in the TM simulator using:
   `tests/1.cm.tm`

The exact TM simulator command may depend on how the simulator is set up on the machine you are using.

---

## ERROR OUTPUT FORMAT
All errors are printed to **stderr** in a consistent format when possible:
- line:col: <ErrorType>: <message>

Examples:
- 12:5: Semantic error: undefined identifier 'z'
- 18:9: Semantic error: redefined identifier 'y' in same scope
- runtime error: array index out of bounds

---

## SYMBOL TABLE OUTPUT (-s)
- Symbol tables are printed by scope
- Scopes are fully nested and printed with indentation based on nesting level
- Each scope’s symbols are printed **right before leaving the scope**

---

## CODE GENERATION OUTPUT (-c)
- TM code is generated only when the input program is valid
- Code generation is performed after parsing and semantic analysis succeed
- Generated code covers expressions, assignments, control flow, function calls, returns, nested blocks, and arrays
- Runtime checking is included for out-of-bounds indexed array access

---

## TEST PLAN (Required 1.cm to 0.cm)
All tests are in `tests/` and each file has a header comment describing expected errors or expected runtime behavior.

**Valid code generation programs (compile and run on TM simulator)**
- **1.cm** — Easy: arithmetic, assignments, input/output in main
- **2.cm** — Medium: recursive Fibonacci, while loops, if-else
- **3.cm** — Hard: arrays, array parameters, nested blocks, global vars, boolean expressions

**Controlled error programs (no more than 3 errors each)**
- **4.cm** — Semantic: void function returns value, int function has empty return (2 errors)
- **5.cm** — Semantic: undefined variable, wrong arg count, void variable (3 errors)
- **6.cm** — Semantic: scalar used as array, void condition in if (2 errors)
- **7.cm** — Semantic: array/scalar mismatch, type mismatch, void while condition (3 errors)
- **8.cm** — Runtime: compiles successfully but halts on out-of-bounds array access (1 runtime error)

**Stress tests (unlimited errors)**
- **9.cm** — Many semantic errors: undefined vars, redefinitions, void vars, wrong arg count/type, array misuse
- **0.cm** — Mixed syntax + semantic errors: missing semicolons, malformed conditions, undefined functions

---

## EXTRA TESTS
We also used extra test programs during development to check symbol tables, type checking, and code generation behavior more closely.

These included focused tests for:
- symbol table / scope behavior
- array indexing
- assignment compatibility
- operand type checking
- return statements
- condition checking
- code generation for expressions and assignments
- function calls and returns
- runtime bounds checking

---

## REFERENCE PROGRAMS
We also tested with valid reference-style programs to check parsing, semantic analysis, and TM code generation on larger examples.

---

## ACKNOWLEDGEMENTS
We used the Tiny language sample parser from C1-Package.tgz (Professor Song's class CourseLink) as a starting point.
We adapted it for C- and added our own JFlex scanner rules, CUP grammar, AST classes, AST printing, semantic analysis, code generation, and error handling.