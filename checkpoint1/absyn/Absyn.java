/*
  File name: Absyn.java
  Name: group 25: Tameem Mughal, Muhammad Ali, Richard
  Date: Feb 26 2026
  Purpose: Base class for all the AST nodes that stores line and column numbers and lets a visitor process the node
*/
package absyn;

public abstract class Absyn {
  public int row;
  public int col;

  public Absyn(int row, int col) {
    this.row = row;
    this.col = col;
  }

  public abstract void accept(AbsynVisitor visitor, int level);
}
