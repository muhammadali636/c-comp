# File: makefile
# CIS*4650 Checkpoint 3 — Makefile + Build sanity + Optional test target
# Names: Muhammad Ali, Muhammad Tameem-Mughal, Richard Milasovanov

JAVAC = javac
JFLEX_JAR = lib/jflex-1.9.1.jar
CUP_JAR = lib/java-cup-11b.jar
CUP_RT  = lib/java-cup-11b-runtime.jar

# Linux uses : for classpath
CP = .:$(CUP_RT)

.PHONY: all clean run test

all: Lexer.java parser.java sym.java
	$(JAVAC) -cp "$(CP)" absyn/*.java sym.java parser.java Lexer.java CM.java ShowTreeVisitor.java SemanticAnalyzer.java SymbolTable.java CodeGenerator.java

parser.java sym.java: CM.cup
	java -jar $(CUP_JAR) -parser parser -symbols sym -expect 1 CM.cup

Lexer.java: CM.flex sym.java
	java -cp "$(JFLEX_JAR):$(CUP_RT)" jflex.Main CM.flex

clean:
	rm -f Lexer.java parser.java sym.java *.class absyn/*.class *~ tests/*.ast tests/*.sym tests/*.tm

run: all
	java -cp "$(CP)" CM $(FILE)

test: all
	java -cp "$(CP)" CM -a tests/1.cm
	java -cp "$(CP)" CM -s tests/1.cm
	java -cp "$(CP)" CM -c tests/1.cm
	java -cp "$(CP)" CM -c tests/2.cm
	java -cp "$(CP)" CM -c tests/3.cm
	java -cp "$(CP)" CM tests/4.cm
	java -cp "$(CP)" CM tests/5.cm
	java -cp "$(CP)" CM tests/6.cm
	java -cp "$(CP)" CM tests/7.cm
	java -cp "$(CP)" CM tests/8.cm
	java -cp "$(CP)" CM tests/9.cm
	java -cp "$(CP)" CM tests/0.cm
	