/*
  file name: ShowTreeVisits.java
  Name: Group 25 - Tameem Mughal, Richard, Muhammad Ali
  Date: Feb 25 2026 
  Purpose: visitor class that prints parts of the AST like a tree
*/

import absyn.*;

// REPLACE with REAL visitor covering ALL AST.
public class ShowTreeVisitor {

  private void indent(int n) {
    for (int i = 0; i < n; i++) System.out.print(" ");
  }

  public void visit(Program p, int level) 
  {
    indent(level);
    System.out.println("Program");
  }
}