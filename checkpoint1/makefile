# File: makefile
# CIS*4650 Checkpoint 1 — Phase 2: JFlex scanner + CUP parser
# Names: Muhammad Ali, Muhammad Tameem-Mughal, Richard Milasovanov

JAVAC = javac
JFLEX_JAR = lib/jflex-1.9.1.jar
CUP_JAR = lib/java-cup-11b.jar
CUP_RT  = lib/java-cup-11b-runtime.jar

# Windows uses ; for classpath, Unix uses :
ifeq ($(OS),Windows_NT)
  SEP = ;
else
  SEP = :
endif

CP = .$(SEP)$(CUP_RT)

.PHONY: all clean run

all: Lexer.java parser.java sym.java
	$(JAVAC) -cp "$(CP)" absyn/*.java sym.java parser.java Lexer.java CM.java ShowTreeVisitor.java

parser.java sym.java: CM.cup
	java -jar $(CUP_JAR) -parser parser -symbols sym -expect 1 CM.cup

Lexer.java: CM.flex sym.java
	java -cp "$(JFLEX_JAR)$(SEP)$(CUP_RT)" jflex.Main CM.flex

clean:
ifeq ($(OS),Windows_NT)
	-del /f /q Lexer.java parser.java sym.java *.class absyn\*.class 2>nul
else
	rm -f Lexer.java parser.java sym.java *.class absyn/*.class *~
endif

run: all
	java -cp "$(CP)" CM $(FILE)
