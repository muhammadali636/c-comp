package absyn;

public class VarDecList extends Absyn {
  public VarDec head;
  public VarDecList tail;

  public VarDecList(VarDec head, VarDecList tail) {
    super(head != null ? head.row : 0, head != null ? head.col : 0);
    this.head = head;
    this.tail = tail;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
