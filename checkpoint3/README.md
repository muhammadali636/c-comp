# CIS*4650 Checkpoint 2 (Winter 2026) README  
Group 25: Muhammad Ali, Muhammad Tameem Mughal, Richard Milovanov  
Repo: https://github.com/muhammadali636/c-comp

---

## WHAT THIS PROJECT DOES
This checkpoint builds the front-end + semantic analysis for a C- compiler:

- **Scanner (JFlex):** `CM.flex` -> `Lexer.java`
- **Parser (CUP):** `CM.cup` -> `parser.java` and `sym.java`
- Builds an **AST** using classes in `absyn/`
- Prints the AST with the **`-a`** option (ShowTreeVisitor)
- Builds **symbol tables** (nested scopes) and performs **type checking**
- Saves symbol tables with the **`-s`** option
- Reports lexical, syntax, and semantic errors with line/column when available

**Important rule:**  
If there are **syntax errors**, the compiler **stops after parsing** and does **not** run semantic analysis (symbol tables / type checking).

---

## FOLDER / FILE SUMMARY
absyn/                   AST node classes + AbsynVisitor  
CM.flex                  JFlex scanner rules (tokens + row/column + lexical errors)  
CM.cup                   CUP grammar (precedence + AST building + recovery rules)  
CM.java                  Driver program (runs parser, supports -a and -s)  
ShowTreeVisitor.java     Prints AST as an indented tree  
SemanticAnalyzer.java    Traverses AST (post-order) for symbol tables + type checking  
SymbolTable.java         Symbol table / scope stack structures (hash tables)  
tests/                   Required tests: 1.cm to 5.cm  
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

Save both AST and symbol tables:
  java -cp ".:lib/java-cup-11b-runtime.jar" CM -a -s tests/1.cm

**Output files:**
- For input `X.cm`, AST is saved to: `X.cm.ast`
- For input `X.cm`, symbol tables are saved to: `X.cm.sym`

**Note:** Output files are only saved if the program is valid in both **syntax** and **semantics**.

---

## ERROR OUTPUT FORMAT
All errors are printed to **stderr** in a consistent format when possible:
- line:col: <ErrorType>: <message>

Examples:
- 12:5: Semantic error: undefined identifier 'z'
- 18:9: Semantic error: redefined identifier 'y' in same scope

---

## SYMBOL TABLE OUTPUT (-s)
- Symbol tables are printed by scope
- Scopes are fully nested and printed with indentation based on nesting level
- Each scope’s symbols are printed **right before leaving the scope**

---

## TEST PLAN (Required 1.cm to 5.cm)
All tests are in `tests/` and each file has a header comment describing expected errors.

**Semantic pipeline sanity check**
- **1.cm** — no semantic errors (valid program; builds AST + symbol tables + passes type checking)  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -a -s tests/1.cm`

- **2.cm** — a few undefined/redefined identifiers  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/2.cm`

- **3.cm** — type mismatch / invalid operands / bad assignments  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/3.cm`

- **4.cm** — function call / return / condition errors  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/4.cm`

- **5.cm** — many mixed semantic errors (stress test)  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/5.cm`

---

## EXTRA TESTS (Symbol tables + Type checking)

**Symbol table / scope behaviour**
- `symtable_demo.cm` — no errors; symbol tables should show:
  - global `int x`
  - global `bool bbb[10]`
  - function `void foo(void)`
  
  Run:  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -a -s tests/symtable_demo.cm`

- `undef_redef.cm` — targeted undefined + redefined identifier demo  
  - Expected: undefined `z`, redefined `y` in same scope  
  
  Run:  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/undef_redef.cm`

**Type-checking focused tests**
- `tc1_arrayindex.cm` — array index must be `int`  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/tc1_arrayindex.cm`

- `tc2_assign.cm` — assignment compatibility (lvalue/rvalue types)  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/tc2_assign.cm`

- `tc3_operands.cm` — operand types for unary/binary operators  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/tc3_operands.cm`

- `tc4_return.cm` — return statements vs function return type  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/tc4_return.cm`

- `tc5_condition.cm` — if/while condition type rules  
  `java -cp ".:lib/java-cup-11b-runtime.jar" CM -s tests/tc5_condition.cm`

---

## REFERENCE PROGRAMS (Marking checklist)
We also tested with:
`fac.cm`, `booltest.cm`, `gcd.cm`, `sort.cm`, `mutual.cm`

These should parse and produce valid symbol tables and type checking output when the programs are correct.

---

## ACKNOWLEDGEMENTS
We used the Tiny language sample parser from C1-Package.tgz (Professor Song's class CourseLink) as a starting point.
We adapted it for C- and added our own JFlex scanner rules, CUP grammar, AST classes, AST printing, semantic analysis, and error handling.