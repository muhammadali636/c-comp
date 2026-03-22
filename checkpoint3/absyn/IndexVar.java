package absyn;

public class IndexVar extends Var {
  public String name;
  public Exp index;

  public IndexVar(int row, int col, String name, Exp index) {
    super(row, col);
    this.name = name;
    this.index = index;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
