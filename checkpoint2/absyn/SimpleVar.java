package absyn;

public class SimpleVar extends Var {
  public String name;

  public SimpleVar(int row, int col, String name) {
    super(row, col);
    this.name = name;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
