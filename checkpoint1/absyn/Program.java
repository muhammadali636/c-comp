/*
    File name: Program.java
    Name: Group 25- Tameem Mughal, Richard Milovanov, Muhammad Ali
    Purpose: AST root node that represents the whole program and lets a visitor process it
    Date: Feb 25 2026
*/
package absyn;

public class Program extends Absyn {
  public DecList declarations;

  public Program(DecList declarations) {
    super(0, 0);
    this.declarations = declarations;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}