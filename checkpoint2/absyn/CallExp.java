package absyn;

public class CallExp extends Exp {
  public String func;
  public ExpList args;

  public CallExp(int row, int col, String func, ExpList args) {
    super(row, col);
    this.func = func;
    this.args = args;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
