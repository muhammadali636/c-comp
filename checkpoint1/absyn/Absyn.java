/*
  File name: Absyn.java
  Name: group 25: Tameem Mughal, Muhammad Ali, Richard
  Date: Feb 26 2026
  Purpose: Base class for all the AST nodes that stores line and column numbers and lets a visitor process the node
*/
package absyn;

// REPLACE with full AST  classes.
public abstract class Absyn 
{
  public int row;
  public int col;

  public Absyn() {
    this(0,0);
  }

  public Absyn(int r, int c) 
  {
    row=r;
    col=c;
  }

  // the entrypoint for the visitorr used by CM.java when printin AST.
  public abstract void accept(Object visitor, int level);
}