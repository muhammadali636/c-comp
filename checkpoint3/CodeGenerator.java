/*
  File: CodeGenerator.java
  Authors: Group 25 - Muhammad Ali, Muhammad Tameem Mughal, Richard Milovanov
  Date: Mar 2026
  Purpose: Generates TM assembly code from a valid C- AST.

  Runtime layout (TM registers):
    r0 (AC)  = accumulator / expression results
    r1 (AC1) = second register for binary ops
    r5 (FP)  = frame pointer
    r6 (GP)  = global pointer  (top of data memory)
    r7 (PC)  = program counter

  Frame layout (offset from FP):
    FP + 0   = old frame pointer   (ofpFO)
    FP - 1   = return address       (retFO)
    FP - 2   = first parameter      (initFO)
    FP - 3   = second parameter
    ...
    FP - 2 - numParams       = first local
    ...
    frameOffset              = next free slot (used for temps)

  Globals are stored at offsets 0, -1, -2, ... from GP.
*/

import java.io.PrintWriter;
import java.util.*;
import absyn.*;

public class CodeGenerator implements AbsynVisitor {

  private static final int AC  = 0;
  private static final int AC1 = 1;
  private static final int FP  = 5;
  private static final int GP  = 6;
  private static final int PC  = 7;

  private static final int ofpFO  =  0;
  private static final int retFO  = -1;
  private static final int initFO = -2;

  private PrintWriter out;
  private int emitLoc    = 0;
  private int highEmitLoc = 0;

  private int globalOffset = 0;
  private int frameOffset;
  private int mainEntry = -1;
  private boolean inGlobalScope = true;

  private Deque<Map<String, VarInfo>> scopeStack = new ArrayDeque<>();
  private Map<String, Integer> funEntries = new HashMap<>();

  private static class VarInfo {
    int offset;
    boolean isGlobal;
    boolean isArray;
    int arraySize;
    boolean isParamArray;

    VarInfo(int offset, boolean isGlobal, boolean isArray, int arraySize, boolean isParamArray) {
      this.offset = offset;
      this.isGlobal = isGlobal;
      this.isArray = isArray;
      this.arraySize = arraySize;
      this.isParamArray = isParamArray;
    }
  }

  public CodeGenerator(PrintWriter out) {
    this.out = out;
  }

  /* ================================================================
     Emit helpers
     ================================================================ */

  private void emitRO(String op, int r, int s, int t, String c) {
    out.println(emitLoc + ":     " + op + "  " + r + "," + s + "," + t + "\t" + c);
    emitLoc++;
    if (highEmitLoc < emitLoc) highEmitLoc = emitLoc;
  }

  private void emitRM(String op, int r, int d, int s, String c) {
    out.println(emitLoc + ":     " + op + "  " + r + "," + d + "(" + s + ")\t" + c);
    emitLoc++;
    if (highEmitLoc < emitLoc) highEmitLoc = emitLoc;
  }

  private void emitRM_Abs(String op, int r, int a, String c) {
    out.println(emitLoc + ":     " + op + "  " + r + "," + (a - (emitLoc + 1)) + "(" + PC + ")\t" + c);
    emitLoc++;
    if (highEmitLoc < emitLoc) highEmitLoc = emitLoc;
  }

  private int emitSkip(int n) {
    int loc = emitLoc;
    emitLoc += n;
    if (highEmitLoc < emitLoc) highEmitLoc = emitLoc;
    return loc;
  }

  private void emitBackup(int loc) {
    emitLoc = loc;
  }

  private void emitRestore() {
    emitLoc = highEmitLoc;
  }

  private void emitComment(String c) {
    out.println("* " + c);
  }

  /* ================================================================
     Scope helpers
     ================================================================ */

  private void cgenEnterScope() {
    scopeStack.push(new HashMap<>());
  }

  private void cgenExitScope() {
    if (!scopeStack.isEmpty()) scopeStack.pop();
  }

  private void addVar(String name, VarInfo info) {
    if (!scopeStack.isEmpty()) scopeStack.peek().put(name, info);
  }

  private VarInfo lookupVar(String name) {
    for (Map<String, VarInfo> scope : scopeStack) {
      VarInfo v = scope.get(name);
      if (v != null) return v;
    }
    return null;
  }

  private int countArgs(ExpList args) {
    int n = 0;
    ExpList a = args;
    while (a != null && a.head != null) { n++; a = a.tail; }
    return n;
  }

  /* ================================================================
     Main entry point
     ================================================================ */

  public void generate(Absyn ast, SymbolTable st) {
    if (ast == null) return;

    emitComment("C- Compilation to TM Code");

    emitComment("Standard prelude:");
    emitRM("LD", GP, 0, AC, "load gp with maxaddress");
    int fpPatch = emitSkip(1);
    emitRM("ST", AC, 0, AC, "clear location 0");

    int jumpPatch = emitSkip(1);
    emitComment("End of standard prelude.");

    cgenEnterScope();
    inGlobalScope = true;

    if (ast instanceof Program) {
      DecList dl = ((Program) ast).declarations;
      while (dl != null) {
        if (dl.head != null) dl.head.accept(this, 0);
        dl = dl.tail;
      }
    }

    cgenExitScope();

    int endLoc = emitSkip(0);
    emitBackup(fpPatch);
    emitRM("LDA", FP, globalOffset, GP, "init fp past globals");
    emitRestore();

    emitBackup(jumpPatch);
    if (mainEntry >= 0) {
      emitRM("ST", FP, ofpFO, FP, "push ofp (prelude)");
      emitRestore();
      emitRM("LDA", AC, 1, PC, "load return addr");
      emitRM_Abs("LDA", PC, mainEntry, "jmp to main");
    } else {
      emitRestore();
    }

    emitRO("HALT", 0, 0, 0, "");
  }

  /* ================================================================
     Visitor implementations
     ================================================================ */

  @Override
  public void visit(Program node, int level) {
    if (node.declarations != null)
      node.declarations.accept(this, level);
  }

  @Override
  public void visit(DecList node, int level) {
    while (node != null) {
      if (node.head != null) node.head.accept(this, level);
      node = node.tail;
    }
  }

  @Override
  public void visit(VarDecList node, int level) {
    while (node != null) {
      if (node.head != null) node.head.accept(this, level);
      node = node.tail;
    }
  }

  @Override
  public void visit(ExpList node, int level) {
    while (node != null) {
      if (node.head != null) node.head.accept(this, level);
      node = node.tail;
    }
  }

  @Override
  public void visit(NameTy node, int level) {
  }

  @Override
  public void visit(SimpleDec node, int level) {
    if (inGlobalScope) {
      addVar(node.name, new VarInfo(globalOffset, true, false, 0, false));
      globalOffset--;
    } else {
      addVar(node.name, new VarInfo(frameOffset, false, false, 0, false));
      frameOffset--;
    }
  }

  @Override
  public void visit(ArrayDec node, int level) {
    if (inGlobalScope) {
      addVar(node.name, new VarInfo(globalOffset, true, true, node.size, false));
      globalOffset -= (node.size > 0 ? node.size : 1);
    } else {
      addVar(node.name, new VarInfo(frameOffset, false, true, node.size, false));
      frameOffset -= (node.size > 0 ? node.size : 1);
    }
  }

  @Override
  public void visit(FunctionDec node, int level) {
    if (node.body == null) return;

    int funcEntry = emitSkip(0);
    funEntries.put(node.func, funcEntry);
    if (node.func.equals("main")) mainEntry = funcEntry;

    emitComment("-> function " + node.func);
    emitRM("ST", AC, retFO, FP, "save return addr");

    cgenEnterScope();
    boolean savedGlobal = inGlobalScope;
    inGlobalScope = false;
    frameOffset = initFO;

    VarDecList params = node.params;
    while (params != null && params.head != null) {
      VarDec vd = params.head;
      if (vd instanceof SimpleDec) {
        SimpleDec sd = (SimpleDec) vd;
        addVar(sd.name, new VarInfo(frameOffset, false, false, 0, false));
      } else if (vd instanceof ArrayDec) {
        ArrayDec ad = (ArrayDec) vd;
        addVar(ad.name, new VarInfo(frameOffset, false, true, ad.size, true));
      }
      frameOffset--;
      params = params.tail;
    }

    if (node.body != null)
      node.body.accept(this, level + 1);

    emitRM("LD", PC, retFO, FP, "return to caller");

    cgenExitScope();
    inGlobalScope = savedGlobal;

    emitComment("<- function " + node.func);
  }

  @Override
  public void visit(NilExp node, int level) {
  }

  @Override
  public void visit(IntExp node, int level) {
    emitRM("LDC", AC, node.value, 0, "load const " + node.value);
  }

  @Override
  public void visit(BoolExp node, int level) {
    emitRM("LDC", AC, node.value ? 1 : 0, 0, "load bool " + node.value);
  }

  @Override
  public void visit(VarExp node, int level) {
    if (node.variable != null)
      node.variable.accept(this, level);
  }

  @Override
  public void visit(SimpleVar node, int level) {
    VarInfo info = lookupVar(node.name);
    if (info == null) return;
    int base = info.isGlobal ? GP : FP;
    if (info.isArray && !info.isParamArray) {
      emitRM("LDA", AC, info.offset, base, "load addr of " + node.name);
    } else if (info.isParamArray) {
      emitRM("LD", AC, info.offset, base, "load param array addr " + node.name);
    } else {
      emitRM("LD", AC, info.offset, base, "load " + node.name);
    }
  }

  @Override
  public void visit(IndexVar node, int level) {
    VarInfo info = lookupVar(node.name);
    if (info == null) return;

    if (node.index != null) node.index.accept(this, level);

    int tmpOff = frameOffset;
    frameOffset--;
    emitRM("ST", AC, tmpOff, FP, "push index");

    int base = info.isGlobal ? GP : FP;
    if (info.isParamArray) {
      emitRM("LD", AC, info.offset, base, "load param array base " + node.name);
    } else {
      emitRM("LDA", AC, info.offset, base, "load array base " + node.name);
    }

    emitRM("LD", AC1, tmpOff, FP, "pop index");
    frameOffset++;

    emitRO("SUB", AC, AC, AC1, "compute element addr");
    emitRM("LD", AC, 0, AC, "load array element");
  }

  @Override
  public void visit(AssignExp node, int level) {
    if (node.rhs != null) node.rhs.accept(this, level);

    if (node.lhs != null && node.lhs.variable != null) {
      Var v = node.lhs.variable;
      if (v instanceof SimpleVar) {
        SimpleVar sv = (SimpleVar) v;
        VarInfo info = lookupVar(sv.name);
        if (info != null) {
          int base = info.isGlobal ? GP : FP;
          emitRM("ST", AC, info.offset, base, "assign: " + sv.name + " =");
        }
      } else if (v instanceof IndexVar) {
        IndexVar iv = (IndexVar) v;
        VarInfo info = lookupVar(iv.name);
        if (info != null) {
          int tmpOff = frameOffset;
          frameOffset--;
          emitRM("ST", AC, tmpOff, FP, "assign: save rhs");

          if (iv.index != null) iv.index.accept(this, level);

          int tmpOff2 = frameOffset;
          frameOffset--;
          emitRM("ST", AC, tmpOff2, FP, "assign: save index");

          int base = info.isGlobal ? GP : FP;
          if (info.isParamArray) {
            emitRM("LD", AC, info.offset, base, "assign: load param array base");
          } else {
            emitRM("LDA", AC, info.offset, base, "assign: load array base");
          }

          emitRM("LD", AC1, tmpOff2, FP, "assign: load index");
          frameOffset++;
          emitRO("SUB", AC, AC, AC1, "assign: compute element addr");

          emitRM("LD", AC1, tmpOff, FP, "assign: load rhs");
          frameOffset++;
          emitRM("ST", AC1, 0, AC, "assign: store to array element");
        }
      }
    }
  }

  @Override
  public void visit(OpExp node, int level) {
    if (node.op == OpExp.NOT) {
      if (node.left != null) node.left.accept(this, level);
      int jmpTrue = emitSkip(1);
      emitRM("LDC", AC, 0, 0, "not: false case");
      int jmpEnd = emitSkip(1);
      int trueLoc = emitSkip(0);
      emitRM("LDC", AC, 1, 0, "not: true case");
      int endLoc = emitSkip(0);
      emitBackup(jmpTrue);
      emitRM_Abs("JEQ", AC, trueLoc, "not: if zero then true");
      emitBackup(jmpEnd);
      emitRM_Abs("LDA", PC, endLoc, "not: skip");
      emitRestore();
      return;
    }

    if (node.op == OpExp.UMINUS) {
      if (node.right != null) node.right.accept(this, level);
      emitRM("LDC", AC1, 0, 0, "load 0");
      emitRO("SUB", AC, AC1, AC, "uminus: 0 - val");
      return;
    }

    if (node.left != null) node.left.accept(this, level);
    int tmpOff = frameOffset;
    frameOffset--;
    emitRM("ST", AC, tmpOff, FP, "op: push left");

    if (node.right != null) node.right.accept(this, level);
    emitRM("LD", AC1, tmpOff, FP, "op: load left");
    frameOffset++;

    switch (node.op) {
      case OpExp.PLUS:
        emitRO("ADD", AC, AC1, AC, "op +");
        break;
      case OpExp.MINUS:
        emitRO("SUB", AC, AC1, AC, "op -");
        break;
      case OpExp.MUL:
        emitRO("MUL", AC, AC1, AC, "op *");
        break;
      case OpExp.DIV:
        emitRO("DIV", AC, AC1, AC, "op /");
        break;
      case OpExp.LT:
        emitRO("SUB", AC, AC1, AC, "op <");
        genRelJump("JLT");
        break;
      case OpExp.LE:
        emitRO("SUB", AC, AC1, AC, "op <=");
        genRelJump("JLE");
        break;
      case OpExp.GT:
        emitRO("SUB", AC, AC1, AC, "op >");
        genRelJump("JGT");
        break;
      case OpExp.GE:
        emitRO("SUB", AC, AC1, AC, "op >=");
        genRelJump("JGE");
        break;
      case OpExp.EQ:
        emitRO("SUB", AC, AC1, AC, "op ==");
        genRelJump("JEQ");
        break;
      case OpExp.NE:
        emitRO("SUB", AC, AC1, AC, "op !=");
        genRelJump("JNE");
        break;
      case OpExp.AND: {
        int jmpL = emitSkip(1);
        int jmpR = emitSkip(1);
        emitRM("LDC", AC, 1, 0, "and: true");
        int jmpE = emitSkip(1);
        int falseLoc = emitSkip(0);
        emitRM("LDC", AC, 0, 0, "and: false");
        int endLoc = emitSkip(0);
        emitBackup(jmpL);
        emitRM_Abs("JEQ", AC1, falseLoc, "and: left==0?");
        emitBackup(jmpR);
        emitRM_Abs("JEQ", AC, falseLoc, "and: right==0?");
        emitBackup(jmpE);
        emitRM_Abs("LDA", PC, endLoc, "and: skip false");
        emitRestore();
        break;
      }
      case OpExp.OR: {
        int jmpL = emitSkip(1);
        int jmpR = emitSkip(1);
        emitRM("LDC", AC, 0, 0, "or: false");
        int jmpE = emitSkip(1);
        int trueLoc = emitSkip(0);
        emitRM("LDC", AC, 1, 0, "or: true");
        int endLoc = emitSkip(0);
        emitBackup(jmpL);
        emitRM_Abs("JNE", AC1, trueLoc, "or: left!=0?");
        emitBackup(jmpR);
        emitRM_Abs("JNE", AC, trueLoc, "or: right!=0?");
        emitBackup(jmpE);
        emitRM_Abs("LDA", PC, endLoc, "or: skip true");
        emitRestore();
        break;
      }
      default:
        break;
    }
  }

  private void genRelJump(String jmpOp) {
    int jmpTrue = emitSkip(1);
    emitRM("LDC", AC, 0, 0, "false case");
    int jmpEnd = emitSkip(1);
    int trueLoc = emitSkip(0);
    emitRM("LDC", AC, 1, 0, "true case");
    int endLoc = emitSkip(0);
    emitBackup(jmpTrue);
    emitRM_Abs(jmpOp, AC, trueLoc, "br if true");
    emitBackup(jmpEnd);
    emitRM_Abs("LDA", PC, endLoc, "br to end");
    emitRestore();
  }

  @Override
  public void visit(CallExp node, int level) {
    if (node.func.equals("input")) {
      emitRO("IN", AC, 0, 0, "input()");
      return;
    }

    if (node.func.equals("output")) {
      if (node.args != null && node.args.head != null)
        node.args.head.accept(this, level);
      emitRO("OUT", AC, 0, 0, "output()");
      return;
    }

    Integer entry = funEntries.get(node.func);
    if (entry == null) return;

    int argIdx = 0;
    ExpList arg = node.args;
    while (arg != null && arg.head != null) {
      arg.head.accept(this, level);
      emitRM("ST", AC, frameOffset + initFO - argIdx, FP, "store arg " + argIdx);
      argIdx++;
      arg = arg.tail;
    }

    emitRM("ST", FP, frameOffset + ofpFO, FP, "push ofp");
    emitRM("LDA", FP, frameOffset, FP, "push frame");
    emitRM("LDA", AC, 1, PC, "load ret addr");
    emitRM_Abs("LDA", PC, entry, "jmp to " + node.func);
    emitRM("LD", FP, ofpFO, FP, "pop frame");
  }

  @Override
  public void visit(IfExp node, int level) {
    if (node.test != null) node.test.accept(this, level);

    int jmpFalse = emitSkip(1);

    if (node.thenpart != null) node.thenpart.accept(this, level);

    boolean hasElse = node.elsepart != null && !(node.elsepart instanceof NilExp);

    if (hasElse) {
      int jmpEnd = emitSkip(1);

      int elseLoc = emitSkip(0);
      emitBackup(jmpFalse);
      emitRM_Abs("JEQ", AC, elseLoc, "if: jmp to else");
      emitRestore();

      node.elsepart.accept(this, level);

      int endLoc = emitSkip(0);
      emitBackup(jmpEnd);
      emitRM_Abs("LDA", PC, endLoc, "if: jmp past else");
      emitRestore();
    } else {
      int endLoc = emitSkip(0);
      emitBackup(jmpFalse);
      emitRM_Abs("JEQ", AC, endLoc, "if: jmp to end");
      emitRestore();
    }
  }

  @Override
  public void visit(WhileExp node, int level) {
    int topLoc = emitSkip(0);
    emitComment("while: top");

    if (node.test != null) node.test.accept(this, level);

    int jmpEnd = emitSkip(1);

    if (node.body != null) node.body.accept(this, level);
    emitRM_Abs("LDA", PC, topLoc, "while: jmp to top");

    int endLoc = emitSkip(0);
    emitBackup(jmpEnd);
    emitRM_Abs("JEQ", AC, endLoc, "while: jmp to end");
    emitRestore();
  }

  @Override
  public void visit(ReturnExp node, int level) {
    if (node.exp != null && !(node.exp instanceof NilExp))
      node.exp.accept(this, level);
    emitRM("LD", PC, retFO, FP, "return to caller");
  }

  @Override
  public void visit(CompoundExp node, int level) {
    cgenEnterScope();
    if (node.decs != null) node.decs.accept(this, level);
    if (node.stmts != null) node.stmts.accept(this, level);
    cgenExitScope();
  }
}
