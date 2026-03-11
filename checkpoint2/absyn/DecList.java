package absyn;

public class DecList extends Absyn {
  public Dec head;
  public DecList tail;

  public DecList(Dec head, DecList tail) {
    super(head != null ? head.row : 0, head != null ? head.col : 0);
    this.head = head;
    this.tail = tail;
  }

  public void accept(AbsynVisitor visitor, int level) {
    visitor.visit(this, level);
  }
}
