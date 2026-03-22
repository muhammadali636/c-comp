package absyn;

public class IfExp extends Exp {
  public Exp test;
  public Exp thenpart;
  public Exp elsepart;

  public IfExp(int row, int col, Exp test, Exp thenpart, Exp elsepart) {
    super(row, col);
    this.test = test;
    this.thenpart = thenpart;
    this.elsepart = elsepart;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
