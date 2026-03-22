/*
    File Name: CM.java
    Authors: Group 25 - Muhammad Ali, Muhammad Tameem Mughal, Richard Milovanov
    Purpose: Driver to run code
    Date: mar 15 2026
*/

import java.io.*;
import absyn.*;

class CM {
  // keep flag for showing the AST
  public static final boolean SHOW_TREE = true;

  // print usage for correct running
  private static void usage() {
    System.err.println("Usage:");
    System.err.println("  java CM <file.cm>");
    System.err.println("  java CM -a <file.cm>");
    System.err.println("  java CM -s <file.cm>");
    System.err.println("  java CM -a -s <file.cm>");
    System.err.println("  java CM -s -a <file.cm>");
  }

  public static void main(String[] args) {
    boolean wantAst = false;
    boolean wantSym = false;
    String file = null;

    // parse args and allow -a and -s in any order
    if (args.length == 1) {
      file = args[0];
    } else if (args.length == 2) {
      if (args[0].equals("-a")) {
        wantAst = true;
        file = args[1];
      } else if (args[0].equals("-s")) {
        wantSym = true;
        file = args[1];
      } else {
        usage();
        System.exit(1);
      }
    } else if (args.length == 3) {
      if ((args[0].equals("-a") && args[1].equals("-s")) ||
          (args[0].equals("-s") && args[1].equals("-a"))) {
        wantAst = true;
        wantSym = true;
        file = args[2];
      } else {
        usage();
        System.exit(1);
      }
    } else {
      usage();
      System.exit(1);
    }

    try {
      parser p = new parser(new Lexer(new FileReader(file)));

      // parser should return AST root in parse().value
      java_cup.runtime.Symbol result = p.parse();

      if (result == null || result.value == null) {
        System.err.println("Parsing failed — no AST produced.");
        System.exit(1);
      }

      // stop if syntax errors happened (parser must expose p.hasSyntaxErrors)
      if (p.hasSyntaxErrors) {
        System.exit(1);
      }

      Absyn ast = (Absyn) result.value;

      // collect symbol table output if -s was requested
      StringWriter symBuffer = null;
      PrintWriter symWriter = null;
      if (wantSym) {
        symBuffer = new StringWriter();
        symWriter = new PrintWriter(symBuffer);
      }

      // run semantic analysis (symbol tables + type checking)
      SymbolTable st = new SymbolTable();
      SemanticAnalyzer sa = new SemanticAnalyzer(st, symWriter);
      boolean semanticOk = sa.analyze(ast);

      if (symWriter != null) {
        symWriter.flush();
      }

      // if semantic errors happened, do not write .ast or .sym files
      if (!semanticOk) {
        System.exit(2);
      }

      // write AST to file only when fully valid (syntax + semantics)
      if (wantAst && SHOW_TREE) {
        String astOut = file + ".ast";
        try (PrintWriter out = new PrintWriter(new FileWriter(astOut))) {
          ShowTreeVisitor v = new ShowTreeVisitor(out);
          ast.accept(v, 0);
        }
      }

      // write symbol tables to file only when fully valid (syntax + semantics)
      if (wantSym) {
        String symOut = file + ".sym";
        try (PrintWriter out = new PrintWriter(new FileWriter(symOut))) {
          out.write(symBuffer.toString());
        }
      }
    } catch (Exception e) {
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }
}
