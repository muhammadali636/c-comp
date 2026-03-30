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
    After params: local variables
    After locals: temporaries

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

  private int inputEntry;
  private int outputEntry;

  private Deque<Map<String, VarInfo>> scopeStack = new ArrayDeque<>();
  private Map<String, Integer> funEntries = new HashMap<>();
  private Map<String, List<Integer>> forwardPatches = new HashMap<>();

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

  /* ================================================================
     Main entry point - generate()
     ================================================================ */

  public void generate(Absyn ast, SymbolTable st) {
    if (ast == null) return;

    emitComment("C- Compilation to TM Code");

    /* --- Standard prelude --- */
    emitComment("Standard prelude:");
    emitRM("LD", GP, 0, AC, "load gp with maxaddress");
    emitRM("LDA", FP, 0, GP, "copy gp to fp");
    emitRM("ST", AC, 0, AC, "clear location 0");
    emitComment("Jump around i/o routines here");
    int savedLoc = emitSkip(1);

    /* input routine */
    emitComment("code for input routine");
    inputEntry = emitSkip(0);
    emitRM("ST", AC, retFO, FP, "store return");
    emitRO("IN", AC, 0, 0, "input");
    emitRM("LD", PC, retFO, FP, "return to caller");

    /* output routine */
    emitComment("code for output routine");
    outputEntry = emitSkip(0);
    emitRM("ST", AC, retFO, FP, "store return");
    emitRM("LD", AC, initFO, FP, "load output value");
    emitRO("OUT", AC, 0, 0, "output");
    emitRM("LD", PC, retFO, FP, "return to caller");

    /* backpatch jump around i/o */
    int afterIO = emitSkip(0);
    emitBackup(savedLoc);
    emitRM_Abs("LDA", PC, afterIO, "jump around i/o code");
    emitRestore();
    emitComment("End of standard prelude.");

    /* --- Process all declarations --- */
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

    /* --- Finale: call main --- */
    if (mainEntry >= 0) {
      emitRM("ST", FP, globalOffset, FP, "push ofp");
      emitRM("LDA", FP, globalOffset, FP, "push frame");
      emitRM("LDA", AC, 1, PC, "load ac with ret ptr");
      emitRM_Abs("LDA", PC, mainEntry, "jump to main loc");
      emitRM("LD", FP, ofpFO, FP, "pop frame");
    }
    emitComment("End of execution.");
    emitRO("HALT", 0, 0, 0, "");
  }

  /* ================================================================
     Array access helper
     Computes array element address (and optionally loads value).
     ================================================================ */

  private void emitArrayAccess(String name, Exp index, boolean loadValue, int level) {
    VarInfo info = lookupVar(name);
    if (info == null) return;

    int base = info.isGlobal ? GP : FP;

    /* load array base address */
    if (info.isParamArray) {
      emitRM("LD", AC, info.offset, base, "load id value");
    } else {
      emitRM("LDA", AC, info.offset, base, "load id address");
    }
    int tmpOff = frameOffset;
    frameOffset--;
    emitRM("ST", AC, tmpOff, FP, "store array addr");

    /* evaluate index expression */
    if (index != null) index.accept(this, level);

    /* bounds check - lower bound (index < 0) */
    emitRM("JLT", AC, 1, PC, "halt if subscript < 0");
    emitRM("LDA", PC, 1, PC, "absolute jump if not");
    emitRO("HALT", 0, 0, 0, "halt if subscript < 0");

    /* bounds check - upper bound (index >= size), only for known-size arrays */
    if (!info.isParamArray && info.arraySize > 0) {
      int tmpOff2 = frameOffset;
      frameOffset--;
      emitRM("ST", AC, tmpOff2, FP, "save index for bound check");
      emitRM("LDC", AC1, info.arraySize, 0, "load array size");
      emitRO("SUB", AC, AC, AC1, "index - size");
      emitRM("JGE", AC, 1, PC, "halt if index >= size");
      emitRM("LDA", PC, 1, PC, "skip halt");
      emitRO("HALT", 0, 0, 0, "index out of range");
      emitRM("LD", AC, tmpOff2, FP, "restore index");
      frameOffset++;
    }

    /* compute element address: base - index */
    emitRM("LD", AC1, tmpOff, FP, "load array base addr");
    frameOffset++;
    emitRO("SUB", AC, AC1, AC, "base is at top of array");

    /* optionally load value at computed address */
    if (loadValue) {
      emitRM("LD", AC, 0, AC, "load value at array index");
    }
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

  /* --- Variable declarations --- */

  @Override
  public void visit(SimpleDec node, int level) {
    if (inGlobalScope) {
      emitComment("allocating global var: " + node.name);
      addVar(node.name, new VarInfo(globalOffset, true, false, 0, false));
      globalOffset--;
      emitComment("<- vardecl");
    } else {
      emitComment("processing local var: " + node.name);
      addVar(node.name, new VarInfo(frameOffset, false, false, 0, false));
      frameOffset--;
    }
  }

  @Override
  public void visit(ArrayDec node, int level) {
    int size = node.size > 0 ? node.size : 1;
    if (inGlobalScope) {
      emitComment("allocating global var: " + node.name);
      addVar(node.name, new VarInfo(globalOffset, true, true, node.size, false));
      globalOffset -= size;
      emitComment("<- vardecl");
    } else {
      emitComment("processing local var: " + node.name);
      addVar(node.name, new VarInfo(frameOffset, false, true, node.size, false));
      frameOffset -= size;
    }
  }

  /* --- Function declarations --- */

  @Override
  public void visit(FunctionDec node, int level) {
    if (node.body == null) return; /* prototype -- skip code generation */

    emitComment("processing function: " + node.func);
    emitComment("jump around function body here");
    int jumpAroundLoc = emitSkip(1);

    int funcEntry = emitSkip(0);
    funEntries.put(node.func, funcEntry);
    if (node.func.equals("main")) mainEntry = funcEntry;

    /* backpatch any forward references to this function */
    List<Integer> patches = forwardPatches.remove(node.func);
    if (patches != null) {
      for (int loc : patches) {
        emitBackup(loc);
        emitRM_Abs("LDA", PC, funcEntry, "jump to fun loc");
        emitRestore();
      }
    }

    emitRM("ST", AC, retFO, FP, "store return");

    /* set up scope and frame for this function */
    cgenEnterScope();
    boolean savedGlobal = inGlobalScope;
    inGlobalScope = false;
    frameOffset = initFO;

    /* process parameters */
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

    /* process function body */
    if (node.body != null)
      node.body.accept(this, level + 1);

    emitRM("LD", PC, retFO, FP, "return to caller");

    cgenExitScope();
    inGlobalScope = savedGlobal;

    /* backpatch jump around function body */
    int afterFunc = emitSkip(0);
    emitBackup(jumpAroundLoc);
    emitRM_Abs("LDA", PC, afterFunc, "jump around fn body");
    emitRestore();
    emitComment("<- fundecl");
  }

  /* --- Literals --- */

  @Override
  public void visit(NilExp node, int level) {
  }

  @Override
  public void visit(IntExp node, int level) {
    emitComment("-> constant");
    emitRM("LDC", AC, node.value, 0, "load const");
    emitComment("<- constant");
  }

  @Override
  public void visit(BoolExp node, int level) {
    emitComment("-> constant");
    emitRM("LDC", AC, node.value ? 1 : 0, 0, "load const");
    emitComment("<- constant");
  }

  /* --- Variable access (expressions / RHS) --- */

  @Override
  public void visit(VarExp node, int level) {
    if (node.variable != null)
      node.variable.accept(this, level);
  }

  @Override
  public void visit(SimpleVar node, int level) {
    VarInfo info = lookupVar(node.name);
    if (info == null) return;

    emitComment("-> id");
    emitComment("looking up id: " + node.name);

    int base = info.isGlobal ? GP : FP;

    if (info.isArray && !info.isParamArray) {
      /* local/global array: load base address for passing */
      emitRM("LDA", AC, info.offset, base, "load id address");
    } else if (info.isParamArray) {
      /* param array: load stored pointer */
      emitRM("LD", AC, info.offset, base, "load id value");
    } else {
      /* scalar: load value */
      emitRM("LD", AC, info.offset, base, "load id value");
    }

    emitComment("<- id");
  }

  @Override
  public void visit(IndexVar node, int level) {
    VarInfo info = lookupVar(node.name);
    if (info == null) return;

    emitComment("-> subs");
    emitArrayAccess(node.name, node.index, true, level);
    emitComment("<- subs");
  }

  /* --- Assignments --- */

  @Override
  public void visit(AssignExp node, int level) {
    if (node.lhs == null || node.lhs.variable == null) return;

    Var v = node.lhs.variable;

    emitComment("-> op");

    if (v instanceof SimpleVar) {
      SimpleVar sv = (SimpleVar) v;
      VarInfo info = lookupVar(sv.name);
      if (info == null) { emitComment("<- op"); return; }

      int base = info.isGlobal ? GP : FP;

      /* load ADDRESS of LHS */
      emitComment("-> id");
      emitComment("looking up id: " + sv.name);
      emitRM("LDA", AC, info.offset, base, "load id address");
      emitComment("<- id");

      int tmpOff = frameOffset;
      frameOffset--;
      emitRM("ST", AC, tmpOff, FP, "op: push left");

      /* evaluate RHS */
      if (node.rhs != null) node.rhs.accept(this, level);

      /* store value at address */
      emitRM("LD", AC1, tmpOff, FP, "op: load left");
      frameOffset++;
      emitRM("ST", AC, 0, AC1, "assign: store value");

    } else if (v instanceof IndexVar) {
      IndexVar iv = (IndexVar) v;
      VarInfo info = lookupVar(iv.name);
      if (info == null) { emitComment("<- op"); return; }

      /* compute ADDRESS of array element (no value load) */
      emitComment("-> subs");
      emitArrayAccess(iv.name, iv.index, false, level);
      emitComment("<- subs");

      int tmpOff = frameOffset;
      frameOffset--;
      emitRM("ST", AC, tmpOff, FP, "op: push left");

      /* evaluate RHS */
      if (node.rhs != null) node.rhs.accept(this, level);

      /* store value at address */
      emitRM("LD", AC1, tmpOff, FP, "op: load left");
      frameOffset++;
      emitRM("ST", AC, 0, AC1, "assign: store value");
    }

    emitComment("<- op");
  }

  /* --- Binary / unary operators --- */

  @Override
  public void visit(OpExp node, int level) {
    emitComment("-> op");

    /* unary NOT (~) */
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
      emitComment("<- op");
      return;
    }

    /* unary MINUS */
    if (node.op == OpExp.UMINUS) {
      if (node.right != null) node.right.accept(this, level);
      emitRM("LDC", AC1, 0, 0, "load 0");
      emitRO("SUB", AC, AC1, AC, "uminus: 0 - val");
      emitComment("<- op");
      return;
    }

    /* binary operators: evaluate left, push, evaluate right, operate */
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

    emitComment("<- op");
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
    emitRM_Abs("LDA", PC, endLoc, "unconditional jmp");
    emitRestore();
  }

  /* --- Function calls --- */

  @Override
  public void visit(CallExp node, int level) {
    emitComment("-> call of function: " + node.func);

    boolean isInput = node.func.equals("input");
    boolean isOutput = node.func.equals("output");

    /* determine entry point */
    int entry;
    boolean isForward = false;

    if (isInput) {
      entry = inputEntry;
    } else if (isOutput) {
      entry = outputEntry;
    } else {
      Integer e = funEntries.get(node.func);
      if (e != null) {
        entry = e;
      } else {
        entry = 0; /* placeholder for forward reference */
        isForward = true;
      }
    }

    /* evaluate and store arguments */
    int argIdx = 0;
    ExpList arg = node.args;
    while (arg != null && arg.head != null) {
      arg.head.accept(this, level);
      emitRM("ST", AC, frameOffset + initFO - argIdx, FP, "store arg val");
      argIdx++;
      arg = arg.tail;
    }

    /* standard call convention */
    emitRM("ST", FP, frameOffset + ofpFO, FP, "push ofp");
    emitRM("LDA", FP, frameOffset, FP, "push frame");
    emitRM("LDA", AC, 1, PC, "load ac with ret ptr");
    int jumpLoc = emitSkip(0);
    emitRM_Abs("LDA", PC, entry, "jump to fun loc");
    emitRM("LD", FP, ofpFO, FP, "pop frame");

    /* record forward reference for backpatching */
    if (isForward) {
      forwardPatches.computeIfAbsent(node.func, k -> new ArrayList<>()).add(jumpLoc);
    }

    emitComment("<- call");
  }

  /* --- Control structures --- */

  @Override
  public void visit(IfExp node, int level) {
    emitComment("-> if");

    /* evaluate test condition */
    if (node.test != null) node.test.accept(this, level);

    /* skip for conditional jump to else */
    emitComment("if: jump to else belongs here");
    int savedJmp = emitSkip(1);

    /* then part */
    if (node.thenpart != null) node.thenpart.accept(this, level);

    /* skip for unconditional jump to end */
    emitComment("if: jump to end belongs here");
    int savedEnd = emitSkip(1);

    /* backpatch conditional jump to here (else location) */
    int elseLoc = emitSkip(0);
    emitBackup(savedJmp);
    emitRM_Abs("JEQ", AC, elseLoc, "if: jmp to else");
    emitRestore();

    /* else part (if any) */
    boolean hasElse = node.elsepart != null && !(node.elsepart instanceof NilExp);
    if (hasElse) {
      node.elsepart.accept(this, level);
    }

    /* backpatch unconditional jump to here (end location) */
    int endLoc = emitSkip(0);
    emitBackup(savedEnd);
    emitRM_Abs("LDA", PC, endLoc, "jmp to end");
    emitRestore();

    emitComment("<- if");
  }

  @Override
  public void visit(WhileExp node, int level) {
    emitComment("-> while");
    emitComment("while: jump after body comes back here");

    int topLoc = emitSkip(0);

    /* evaluate test condition */
    if (node.test != null) node.test.accept(this, level);

    /* skip for conditional jump to end */
    emitComment("while: jump to end belongs here");
    int savedJmp = emitSkip(1);

    /* body */
    if (node.body != null) node.body.accept(this, level);

    /* unconditional jump back to test */
    emitRM_Abs("LDA", PC, topLoc, "while: absolute jmp to test");

    /* backpatch conditional jump to here (end) */
    int endLoc = emitSkip(0);
    emitBackup(savedJmp);
    emitRM_Abs("JEQ", AC, endLoc, "while: jmp to end");
    emitRestore();

    emitComment("<- while");
  }

  /* --- Return --- */

  @Override
  public void visit(ReturnExp node, int level) {
    emitComment("-> return");
    if (node.exp != null && !(node.exp instanceof NilExp))
      node.exp.accept(this, level);
    emitRM("LD", PC, retFO, FP, "return to caller");
    emitComment("<- return");
  }

  /* --- Compound statements --- */

  @Override
  public void visit(CompoundExp node, int level) {
    emitComment("-> compound statement");
    cgenEnterScope();
    if (node.decs != null) node.decs.accept(this, level);
    if (node.stmts != null) node.stmts.accept(this, level);
    cgenExitScope();
    emitComment("<- compound statement");
  }
}
