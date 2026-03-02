package absyn;

public class ExpList extends Absyn {
  public Exp head;
  public ExpList tail;

  public ExpList(Exp head, ExpList tail) {
    super(head != null ? head.row : 0, head != null ? head.col : 0);
    this.head = head;
    this.tail = tail;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
