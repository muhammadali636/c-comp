package absyn;

public class VarExp extends Exp {
  public Var variable;

  public VarExp(int row, int col, Var variable) {
    super(row, col);
    this.variable = variable;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
