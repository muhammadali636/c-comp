package absyn;

public class ArrayDec extends VarDec {
  public NameTy typ;
  public String name;
  public int size;

  public ArrayDec(int row, int col, NameTy typ, String name, int size) {
    super(row, col);
    this.typ = typ;
    this.name = name;
    this.size = size;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
