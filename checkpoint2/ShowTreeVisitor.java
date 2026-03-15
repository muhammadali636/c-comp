/*
  file name: ShowTreeVisitor.java
  Name: Group 25 - Tameem Mughal, Richard, Muhammad Ali
  Date: Mar 15 2026
  Purpose: visitor class that prints the AST as a hierarchical tree
*/

import java.io.PrintWriter;
import absyn.*;

public class ShowTreeVisitor implements AbsynVisitor {
  static final int SPACES = 4;
  private PrintWriter out;

  public ShowTreeVisitor(PrintWriter out) {
    this.out = out;
  }

  private void indent(int level) {
    for (int i = 0; i < level * SPACES; i++)
      out.print(" ");
  }

  public void visit(Program node, int level) {
    indent(level);
    out.println("Program");
    if (node.declarations != null)
      node.declarations.accept(this, level + 1);
  }

  public void visit(DecList node, int level) {
    while (node != null) {
      if (node.head != null)
        node.head.accept(this, level);
      node = node.tail;
    }
  }

  public void visit(VarDecList node, int level) {
    while (node != null) {
      if (node.head != null)
        node.head.accept(this, level);
      node = node.tail;
    }
  }

  public void visit(ExpList node, int level) {
    while (node != null) {
      if (node.head != null)
        node.head.accept(this, level);
      node = node.tail;
    }
  }

  public void visit(NameTy node, int level) {
    indent(level);
    if (node.typ == NameTy.BOOL)
      out.println("Type: bool");
    else if (node.typ == NameTy.INT)
      out.println("Type: int");
    else if (node.typ == NameTy.VOID)
      out.println("Type: void");
  }

  public void visit(SimpleDec node, int level) {
    indent(level);
    out.println("SimpleDec: " + node.name);
    node.typ.accept(this, level + 1);
  }

  public void visit(ArrayDec node, int level) {
    indent(level);
    out.println("ArrayDec: " + node.name + "[" + node.size + "]");
    node.typ.accept(this, level + 1);
  }

  public void visit(FunctionDec node, int level) {
    indent(level);
    out.println("FunctionDec: " + node.func);
    node.result.accept(this, level + 1);
    if (node.params != null) {
      indent(level + 1);
      out.println("Params:");
      node.params.accept(this, level + 2);
    } else {
      indent(level + 1);
      out.println("Params: void");
    }
    if (node.body != null)
      node.body.accept(this, level + 1);
  }

  public void visit(NilExp node, int level) {
  }

  public void visit(IntExp node, int level) {
    indent(level);
    out.println("IntExp: " + node.value);
  }

  public void visit(BoolExp node, int level) {
    indent(level);
    out.println("BoolExp: " + node.value);
  }

  public void visit(VarExp node, int level) {
    if (node.variable != null)
      node.variable.accept(this, level);
  }

  public void visit(CallExp node, int level) {
    indent(level);
    out.println("CallExp: " + node.func);
    if (node.args != null) {
      indent(level + 1);
      out.println("Args:");
      node.args.accept(this, level + 2);
    }
  }

  public void visit(OpExp node, int level) {
    indent(level);
    String op;
    switch (node.op) {
      case OpExp.PLUS:   op = "+"; break;
      case OpExp.MINUS:  op = "-"; break;
      case OpExp.UMINUS: op = "-"; break;
      case OpExp.MUL:    op = "*"; break;
      case OpExp.DIV:    op = "/"; break;
      case OpExp.LT:     op = "<"; break;
      case OpExp.LE:     op = "<="; break;
      case OpExp.GT:     op = ">"; break;
      case OpExp.GE:     op = ">="; break;
      case OpExp.EQ:     op = "=="; break;
      case OpExp.NE:     op = "!="; break;
      case OpExp.NOT:    op = "~"; break;
      case OpExp.AND:    op = "&&"; break;
      case OpExp.OR:     op = "||"; break;
      default:           op = "?"; break;
    }
    out.println("OpExp: " + op);
    if (node.left != null)
      node.left.accept(this, level + 1);
    if (node.right != null)
      node.right.accept(this, level + 1);
  }

  public void visit(AssignExp node, int level) {
    indent(level);
    out.println("AssignExp:");
    if (node.lhs != null)
      node.lhs.accept(this, level + 1);
    if (node.rhs != null)
      node.rhs.accept(this, level + 1);
  }

  public void visit(IfExp node, int level) {
    indent(level);
    out.println("IfExp:");
    indent(level + 1);
    out.println("Test:");
    if (node.test != null)
      node.test.accept(this, level + 2);
    indent(level + 1);
    out.println("Then:");
    if (node.thenpart != null)
      node.thenpart.accept(this, level + 2);
    if (node.elsepart != null && !(node.elsepart instanceof NilExp)) {
      indent(level + 1);
      out.println("Else:");
      node.elsepart.accept(this, level + 2);
    }
  }

  public void visit(WhileExp node, int level) {
    indent(level);
    out.println("WhileExp:");
    indent(level + 1);
    out.println("Test:");
    if (node.test != null)
      node.test.accept(this, level + 2);
    indent(level + 1);
    out.println("Body:");
    if (node.body != null)
      node.body.accept(this, level + 2);
  }

  public void visit(ReturnExp node, int level) {
    indent(level);
    out.println("ReturnExp:");
    if (node.exp != null && !(node.exp instanceof NilExp))
      node.exp.accept(this, level + 1);
  }

  public void visit(CompoundExp node, int level) {
    indent(level);
    out.println("CompoundExp:");
    if (node.decs != null) {
      indent(level + 1);
      out.println("LocalDecs:");
      node.decs.accept(this, level + 2);
    }
    if (node.stmts != null) {
      indent(level + 1);
      out.println("Statements:");
      node.stmts.accept(this, level + 2);
    }
  }

  public void visit(SimpleVar node, int level) {
    indent(level);
    out.println("SimpleVar: " + node.name);
  }

  public void visit(IndexVar node, int level) {
    indent(level);
    out.println("IndexVar: " + node.name);
    indent(level + 1);
    out.println("Index:");
    if (node.index != null)
      node.index.accept(this, level + 2);
  }
}
