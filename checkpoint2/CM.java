/*
    File Name: CM.java
    Authors: Group 25 - Muhammad Ali, Muhammad Tameem Mughal, Richard Milovanov
    Purpose: Driver to run code
    Date: February 22 2026
*/

import java.io.*;
import absyn.*;

class CM 
{
  // keep flag for showing the AST
  public static final boolean SHOW_TREE = true;

  public static void main(String[] args) 
  {
    boolean wantAst=false;
    String file=null;

    if (args.length== 1) {
      file = args[0];
    } 
    else if (args.length == 2 &&args[0].equals("-a")) 
    {
      wantAst = true;
      file=args[1];
    } 
    else 
    {
      System.err.println("Usage:");
      System.err.println("  java CM <file.cm>");
      System.err.println("  java CM -a <file.cm>");
      System.exit(1);
    }

    try 
    {
      parser p = new parser(new Lexer(new FileReader(file)));

      // parser should return AST root in parse().value
      java_cup.runtime.Symbol result = p.parse();

      if (result == null || result.value == null) {
        System.err.println("Parsing failed — no AST produced.");
        System.exit(1);
      }

      Absyn ast = (Absyn) result.value;

      if (wantAst && SHOW_TREE)
      {
        System.out.println("Abstract Syntax Tree:");
        ShowTreeVisitor v = new ShowTreeVisitor();
        ast.accept(v,0);
      }
    } 
    catch (Exception e) 
    {
      // error
      System.err.println("Error: "+ e.getMessage());
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }
}