package absyn;

public class SimpleDec extends VarDec {
  public NameTy typ;
  public String name;

  public SimpleDec(int row, int col, NameTy typ, String name) {
    super(row, col);
    this.typ = typ;
    this.name = name;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
