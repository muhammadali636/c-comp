CIS*4650 Checkpoint 1 (Winter 2026) README
Group 25: Muhammad Ali, Muhammad Tameem Mughal, Richard Milovanov
Repo: https://github.com/muhammadali636/c-comp/tree/main/checkpoint1


WHAT THIS PROJECT DOES
This checkpoint builds the front-end of a C- compiler:
- Scanner (JFlex): CM.flex -> Lexer.java
- Parser (CUP): CM.cup -> parser.java and sym.java
- Builds an AST using classes in the absyn/ folder
- Print the AST with the -a option (ShowTreeVisitor)
- Report lexical and syntax errors, and tries to continue after errors


FOLDER / FILE SUMMARY
absyn/                 AST node classes + AbsynVisitor
CM.flex                JFlex scanner rules (tokens + row/column + lexical errors)
CM.cup                 CUP grammar (precedence + AST building + recovery rules)
CM.java                Driver program (runs parser, supports -a)
ShowTreeVisitor.java   Prints AST as an indented tree
tests/                 Required tests: 1.cm to 5.cm
lib/                   JFlex and CUP jars
makefile               Build + clean rules


BUILD (on linux.socs.uoguelph.ca)
1) Build everything:
   make

2) Clean generated files and .class files:
   make clean


RUN (linux.socs.uoguelph.ca)
Run without AST:
   java -cp ".:lib/java-cup-11b-runtime.jar" CM tests/1.cm

Run with AST (-a is required for this checkpoint):
   java -cp ".:lib/java-cup-11b-runtime.jar" CM -a tests/1.cm


TEST PLAN (Required 1.cm to 5.cm)
All tests are in tests/ and each file has a header comment describing expected errors.

1.cm  Expected: no errors
   java -cp ".:lib/java-cup-11b-runtime.jar" CM -a tests/1.cm

2.cm  Expected: lexical errors (invalid characters)
   java -cp ".:lib/java-cup-11b-runtime.jar" CM -a tests/2.cm

3.cm  Expected: syntax errors (recovery should report multiple)
   java -cp ".:lib/java-cup-11b-runtime.jar" CM -a tests/3.cm

4.cm  Expected: different syntax/recovery case (<= 3 errors)
   java -cp ".:lib/java-cup-11b-runtime.jar" CM -a tests/4.cm

5.cm  Expected: many mixed errors (no limit)
   java -cp ".:lib/java-cup-11b-runtime.jar" CM -a tests/5.cm


REFERENCE PROGRAMS (Marking checklist)
We also tested with:
fac.cm, booltest.cm, gcd.cm, sort.cm, mutual.cm
These should parse and print a complete AST with -a.


ACKNOWLEDGEMENTS
We used the Tiny language sample parser from C1-Package.tgz (Professor Song's class courselink) as a starting point.
We adapted it for C- and added our own JFlex scanner rules, CUP grammar, AST classes, AST printing, and error handling.

