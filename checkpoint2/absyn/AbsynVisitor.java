package absyn;

public interface AbsynVisitor {
  void visit(Program node, int level);
  void visit(DecList node, int level);
  void visit(VarDecList node, int level);
  void visit(ExpList node, int level);
  void visit(NameTy node, int level);
  void visit(SimpleDec node, int level);
  void visit(ArrayDec node, int level);
  void visit(FunctionDec node, int level);
  void visit(NilExp node, int level);
  void visit(IntExp node, int level);
  void visit(BoolExp node, int level);
  void visit(VarExp node, int level);
  void visit(CallExp node, int level);
  void visit(OpExp node, int level);
  void visit(AssignExp node, int level);
  void visit(IfExp node, int level);
  void visit(WhileExp node, int level);
  void visit(ReturnExp node, int level);
  void visit(CompoundExp node, int level);
  void visit(SimpleVar node, int level);
  void visit(IndexVar node, int level);
}
