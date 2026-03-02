package absyn;

public class WhileExp extends Exp {
  public Exp test;
  public Exp body;

  public WhileExp(int row, int col, Exp test, Exp body) {
    super(row, col);
    this.test = test;
    this.body = body;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
