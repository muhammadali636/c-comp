/*
    File name: Program.java
    Name: Group 25- Tameem Mughal, Richard Milovanov, Muhammad Ali
    Purpose: AST root node that represents the whole program and lets a visitor process it
    Date: Feb 25 2026
*/
package absyn;

// REPLACE with REAL root node that includes declarations list, etc.
public class Program extends Absyn 
{

  public Program() {
    super(0,0);
  }

  public void accept(Object visitor, int level) 
  {
    // call visitor.visit(Program, int) 
    try 
    {
      visitor.getClass()
          .getMethod("visit", Program.class, int.class)
          .invoke(visitor, this, level);
    } 
    catch (Exception e) {
      System.out.println("(Program)");
    }
  }
}