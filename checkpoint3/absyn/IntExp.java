package absyn;

public class IntExp extends Exp {
  public int value;

  public IntExp(int row, int col, int value) {
    super(row, col);
    this.value = value;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
