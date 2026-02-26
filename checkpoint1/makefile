# File: makefile
# CIS*4650 Checkpoint 1 
# Names: Muhammad Ali, Muhammad Tameem-Mughal, Richard Milasovanov
# Date: Feb 21 2026


# extend this when u add files

JAVAC=javac

SRC_DIR=src

all: CM.class

CM.class: $(SRC_DIR)/CM.java $(SRC_DIR)/ASTNode.java $(SRC_DIR)/ParserStub.java $(SRC_DIR)/ScannerStub.java
	$(JAVAC) $(SRC_DIR)/*.java

%.class: %.java
	$(JAVAC) $^

clean:
	rm -f $(SRC_DIR)/*.class *.class *~