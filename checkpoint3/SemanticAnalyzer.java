/*
  file name: SemanticAnalyzer.java
  Name: Group 25 - Tameem Mughal, Richard, Muhammad Ali
  Date: Mar 15 2026
  Purpose: Performs semantic analysis on the abstract syntax tree (AST).
*/

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import absyn.*;

public class SemanticAnalyzer implements AbsynVisitor {

  private final SymbolTable symtab;
  private final PrintWriter symOut;
  private boolean hadError = false;

  private SymbolTable.BaseType lastType = SymbolTable.BaseType.ERROR;
  private boolean lastIsArray = false;
  private SymbolTable.BaseType currentFunctionReturnType = null;

  private enum Context {
    GLOBAL,
    PARAM,
    LOCAL
  }

  private Context currentContext = Context.GLOBAL;

  public SemanticAnalyzer(SymbolTable st, PrintWriter symOut) {
    this.symtab = st;
    this.symOut = symOut;
  }

  public boolean analyze(Absyn ast) {
    if (ast == null) {
      return true;
    }
    // global scope
    symtab.enterScope();
    // built-in functions: int input(void); void output(int);
    installBuiltIns();
    currentContext = Context.GLOBAL;
    ast.accept(this, 0);
    // print global scope right before leaving it
    printCurrentScope(0);
    symtab.exitScope();
    return !hadError;
  }

  // === Helpers ===

  private void installBuiltIns() {
    // int input(void);
    SymbolTable.Symbol inputFun =
        SymbolTable.Symbol.function(
            "input",
            SymbolTable.BaseType.INT,
            new ArrayList<SymbolTable.BaseType>(),
            0,
            0);
    symtab.insert(inputFun);

    // void output(int);
    List<SymbolTable.BaseType> outputParams = new ArrayList<SymbolTable.BaseType>();
    outputParams.add(SymbolTable.BaseType.INT);
    SymbolTable.Symbol outputFun =
        SymbolTable.Symbol.function(
            "output",
            SymbolTable.BaseType.VOID,
            outputParams,
            0,
            0);
    symtab.insert(outputFun);
  }

  private SymbolTable.BaseType fromNameTy(NameTy ty) {
    if (ty == null) {
      return SymbolTable.BaseType.ERROR;
    }
    switch (ty.typ) {
      case NameTy.BOOL:
        return SymbolTable.BaseType.BOOL;
      case NameTy.INT:
        return SymbolTable.BaseType.INT;
      case NameTy.VOID:
        return SymbolTable.BaseType.VOID;
      default:
        return SymbolTable.BaseType.ERROR;
    }
  }

  private void semanticError(Absyn node, String message) {
    int row = node != null ? node.row : 0;
    int col = node != null ? node.col : 0;
    System.err.println(row + ":" + col + ": Semantic error: " + message);
    hadError = true;
  }

  private void printCurrentScope(int level) {
    if (symOut == null) {
      return;
    }
    Map<String, SymbolTable.Symbol> scope = symtab.currentScopeSnapshot();
    if (scope == null || scope.isEmpty()) {
      return;
    }
    int indentSpaces = level * 2;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < indentSpaces; i++) {
      sb.append(' ');
    }
    String indent = sb.toString();
    symOut.println(indent + "Scope (level " + level + "):");
    for (SymbolTable.Symbol sym : scope.values()) {
      symOut.print(indent + "  " + sym.name + " : ");
      if (sym.kind == SymbolTable.Kind.FUNCTION) {
        symOut.print(typeToString(sym.returnType));
        symOut.print(" function(");
        for (int i = 0; i < sym.paramTypes.size(); i++) {
          if (i > 0) {
            symOut.print(", ");
          }
          symOut.print(typeToString(sym.paramTypes.get(i)));
        }
        symOut.println(")");
      } else {
        symOut.print(typeToString(sym.baseType));
        if (sym.isArray) {
          symOut.print("[" + sym.arraySize + "]");
        }
        symOut.println(" (" + sym.kind.name().toLowerCase() + ")");
      }
    }
    symOut.println();
  }

  private String typeToString(SymbolTable.BaseType t) {
    if (t == null) {
      return "unknown";
    }
    switch (t) {
      case INT:
        return "int";
      case BOOL:
        return "bool";
      case VOID:
        return "void";
      case ERROR:
      default:
        return "error";
    }
  }

  private List<SymbolTable.BaseType> collectParamTypes(VarDecList params) {
    List<SymbolTable.BaseType> types = new ArrayList<>();
    VarDecList current = params;
    while (current != null && current.head != null) {
      VarDec vd = current.head;
      if (vd instanceof SimpleDec) {
        SimpleDec sd = (SimpleDec) vd;
        types.add(fromNameTy(sd.typ));
      } else if (vd instanceof ArrayDec) {
        ArrayDec ad = (ArrayDec) vd;
        types.add(fromNameTy(ad.typ));
      }
      current = current.tail;
    }
    return types;
  }

  private List<Boolean> collectParamIsArray(VarDecList params) {
    List<Boolean> flags = new ArrayList<>();
    VarDecList current = params;
    while (current != null && current.head != null) {
      flags.add(current.head instanceof ArrayDec);
      current = current.tail;
    }
    return flags;
  }

  private void declareVar(String name,
                          NameTy tyNode,
                          boolean isArray,
                          int arraySize,
                          int row,
                          int col) {
    SymbolTable.BaseType baseType = fromNameTy(tyNode);

    if (baseType == SymbolTable.BaseType.VOID && currentContext != Context.PARAM) {
      semanticError(new Absyn(row, col) {
        @Override
        public void accept(AbsynVisitor visitor, int level) {}
      }, "variable '" + name + "' cannot be declared as void");
      return;
    }

    SymbolTable.Kind kind;
    switch (currentContext) {
      case PARAM:
        kind = SymbolTable.Kind.PARAMETER;
        break;
      case GLOBAL:
      case LOCAL:
      default:
        kind = isArray ? SymbolTable.Kind.ARRAY : SymbolTable.Kind.VAR;
        break;
    }

    if (symtab.lookupCurrent(name) != null) {
      semanticError(
          new Absyn(row, col) {
            @Override
            public void accept(AbsynVisitor visitor, int level) {
            }
          },
          "redefined identifier '" + name + "'");
      return;
    }

    SymbolTable.Symbol sym =
        SymbolTable.Symbol.variable(name, kind, baseType, isArray, arraySize, row, col);
    symtab.insert(sym);
  }

  // === Visitor implementations ===

  @Override
  public void visit(Program node, int level) {
    if (node.declarations != null) {
      node.declarations.accept(this, level);
    }
  }

  @Override
  public void visit(DecList node, int level) {
    while (node != null) {
      if (node.head != null) {
        node.head.accept(this, level);
      }
      node = node.tail;
    }
  }

  @Override
  public void visit(VarDecList node, int level) {
    while (node != null) {
      if (node.head != null) {
        node.head.accept(this, level);
      }
      node = node.tail;
    }
  }

  @Override
  public void visit(ExpList node, int level) {
    while (node != null) {
      if (node.head != null) {
        node.head.accept(this, level);
      }
      node = node.tail;
    }
  }

  @Override
  public void visit(NameTy node, int level) {
  }

  @Override
  public void visit(SimpleDec node, int level) {
    declareVar(node.name, node.typ, false, 0, node.row, node.col);
  }

  @Override
  public void visit(ArrayDec node, int level) {
    declareVar(node.name, node.typ, true, node.size, node.row, node.col);
  }

  @Override
  public void visit(FunctionDec node, int level) {
    // declare function in the current (likely global) scope
    List<SymbolTable.BaseType> paramTypes = collectParamTypes(node.params);
    SymbolTable.BaseType retType = fromNameTy(node.result);
    boolean isPrototype = (node.body == null);

    List<Boolean> paramArrayFlags = collectParamIsArray(node.params);

    SymbolTable.Symbol existing = symtab.lookupCurrent(node.func);
    if (existing != null) {
      if (existing.kind == SymbolTable.Kind.FUNCTION && existing.isPrototype && !isPrototype) {
        SymbolTable.Symbol funSym =
            SymbolTable.Symbol.function(node.func, retType, paramTypes, paramArrayFlags, false, node.row, node.col);
        symtab.replace(funSym);
      } else {
        semanticError(node, "redefined identifier '" + node.func + "'");
        return;
      }
    } else {
      SymbolTable.Symbol funSym =
          SymbolTable.Symbol.function(node.func, retType, paramTypes, paramArrayFlags, isPrototype, node.row, node.col);
      symtab.insert(funSym);
    }

    if (isPrototype) {
      return;
    }

    // new scope for function body + parameters
    symtab.enterScope();
    Context savedContext = currentContext;
    SymbolTable.BaseType savedReturnType = currentFunctionReturnType;
    currentFunctionReturnType = retType;

    // parameters live in function scope
    currentContext = Context.PARAM;
    if (node.params != null) {
      node.params.accept(this, level + 1);
    }

    // function body (locals / statements)
    currentContext = Context.LOCAL;
    node.body.accept(this, level + 1);

    // print this function scope before leaving
    printCurrentScope(level + 1);
    symtab.exitScope();

    currentFunctionReturnType = savedReturnType;
    currentContext = savedContext;
  }

  @Override
  public void visit(NilExp node, int level) {
    lastType = SymbolTable.BaseType.VOID;
    lastIsArray = false;
  }

  @Override
  public void visit(IntExp node, int level) {
    lastType = SymbolTable.BaseType.INT;
    lastIsArray = false;
  }

  @Override
  public void visit(BoolExp node, int level) {
    lastType = SymbolTable.BaseType.BOOL;
    lastIsArray = false;
  }

  @Override
  public void visit(VarExp node, int level) {
    if (node.variable != null) {
      node.variable.accept(this, level);
    } else {
      lastType = SymbolTable.BaseType.ERROR;
    }
  }

  @Override
  public void visit(CallExp node, int level) {
    SymbolTable.Symbol sym = symtab.lookup(node.func);
    if (sym == null) {
      semanticError(node, "undefined function '" + node.func + "'");
      lastType = SymbolTable.BaseType.ERROR;
      if (node.args != null) {
        node.args.accept(this, level);
      }
      return;
    }

    if (sym.kind != SymbolTable.Kind.FUNCTION) {
      semanticError(node, "'" + node.func + "' is not a function");
      lastType = SymbolTable.BaseType.ERROR;
      return;
    }

    List<SymbolTable.BaseType> argTypes = new ArrayList<>();
    List<Boolean> argIsArray = new ArrayList<>();
    ExpList argNode = node.args;
    while (argNode != null && argNode.head != null) {
      argNode.head.accept(this, level);
      argTypes.add(lastType);
      argIsArray.add(lastIsArray);
      argNode = argNode.tail;
    }

    int expected = sym.paramTypes.size();
    int actual = argTypes.size();
    if (actual != expected) {
      semanticError(node, "function '" + node.func + "' expects " + expected
          + " argument(s) but got " + actual);
    } else {
      for (int i = 0; i < expected; i++) {
        SymbolTable.BaseType pType = sym.paramTypes.get(i);
        SymbolTable.BaseType aType = argTypes.get(i);
        if (aType != SymbolTable.BaseType.ERROR && pType != aType) {
          semanticError(node, "argument " + (i + 1) + " of '" + node.func
              + "' expects " + typeToString(pType) + " but got " + typeToString(aType));
        }
        boolean paramExpectsArray = (sym.paramIsArray != null && i < sym.paramIsArray.size())
            ? sym.paramIsArray.get(i) : false;
        boolean argGivesArray = argIsArray.get(i);
        if (aType != SymbolTable.BaseType.ERROR) {
          if (paramExpectsArray && !argGivesArray) {
            semanticError(node, "argument " + (i + 1) + " of '" + node.func
                + "' expects an array");
          } else if (!paramExpectsArray && argGivesArray) {
            semanticError(node, "argument " + (i + 1) + " of '" + node.func
                + "' expects a scalar but got an array");
          }
        }
      }
    }

    lastType = sym.returnType;
    lastIsArray = false;
  }

  @Override
  public void visit(OpExp node, int level) {
    SymbolTable.BaseType leftType = SymbolTable.BaseType.VOID;
    if (node.left != null) {
      node.left.accept(this, level);
      leftType = lastType;
    }

    SymbolTable.BaseType rightType = SymbolTable.BaseType.VOID;
    if (node.right != null) {
      node.right.accept(this, level);
      rightType = lastType;
    }

    switch (node.op) {
      case OpExp.PLUS:
      case OpExp.MINUS:
      case OpExp.MUL:
      case OpExp.DIV:
        if (leftType != SymbolTable.BaseType.ERROR && leftType != SymbolTable.BaseType.INT) {
          semanticError(node, "left operand of arithmetic operator must be int");
        }
        if (rightType != SymbolTable.BaseType.ERROR && rightType != SymbolTable.BaseType.INT) {
          semanticError(node, "right operand of arithmetic operator must be int");
        }
        lastType = SymbolTable.BaseType.INT;
        break;

      case OpExp.UMINUS:
        if (rightType != SymbolTable.BaseType.ERROR && rightType != SymbolTable.BaseType.INT) {
          semanticError(node, "operand of unary minus must be int");
        }
        lastType = SymbolTable.BaseType.INT;
        break;

      case OpExp.LT:
      case OpExp.LE:
      case OpExp.GT:
      case OpExp.GE:
        if (leftType != SymbolTable.BaseType.ERROR && leftType != SymbolTable.BaseType.INT) {
          semanticError(node, "left operand of relational operator must be int");
        }
        if (rightType != SymbolTable.BaseType.ERROR && rightType != SymbolTable.BaseType.INT) {
          semanticError(node, "right operand of relational operator must be int");
        }
        lastType = SymbolTable.BaseType.BOOL;
        break;

      case OpExp.EQ:
      case OpExp.NE:
        if (leftType != SymbolTable.BaseType.ERROR && rightType != SymbolTable.BaseType.ERROR
            && leftType != rightType) {
          semanticError(node, "operands of equality operator must have the same type");
        }
        lastType = SymbolTable.BaseType.BOOL;
        break;

      case OpExp.AND:
      case OpExp.OR:
        if (leftType != SymbolTable.BaseType.ERROR && leftType != SymbolTable.BaseType.BOOL
            && leftType != SymbolTable.BaseType.INT) {
          semanticError(node, "left operand of logical operator must be bool or int");
        }
        if (rightType != SymbolTable.BaseType.ERROR && rightType != SymbolTable.BaseType.BOOL
            && rightType != SymbolTable.BaseType.INT) {
          semanticError(node, "right operand of logical operator must be bool or int");
        }
        lastType = SymbolTable.BaseType.BOOL;
        break;

      case OpExp.NOT:
        if (leftType != SymbolTable.BaseType.ERROR && leftType != SymbolTable.BaseType.BOOL
            && leftType != SymbolTable.BaseType.INT) {
          semanticError(node, "operand of '~' must be bool or int");
        }
        lastType = SymbolTable.BaseType.BOOL;
        break;

      default:
        lastType = SymbolTable.BaseType.ERROR;
        break;
    }
  }

  @Override
  public void visit(AssignExp node, int level) {
    SymbolTable.BaseType lhsType = SymbolTable.BaseType.ERROR;
    if (node.lhs != null) {
      node.lhs.accept(this, level);
      lhsType = lastType;
    }

    SymbolTable.BaseType rhsType = SymbolTable.BaseType.ERROR;
    if (node.rhs != null) {
      node.rhs.accept(this, level);
      rhsType = lastType;
    }

    if (lhsType != SymbolTable.BaseType.ERROR && rhsType != SymbolTable.BaseType.ERROR) {
      if (rhsType == SymbolTable.BaseType.VOID) {
        semanticError(node, "cannot assign void value");
      } else if (lhsType != rhsType) {
        semanticError(node, "assignment type mismatch: " + typeToString(lhsType)
            + " = " + typeToString(rhsType));
      }
    }

    lastType = lhsType;
  }

  @Override
  public void visit(IfExp node, int level) {
    if (node.test != null) {
      node.test.accept(this, level);
      SymbolTable.BaseType testType = lastType;
      if (testType != SymbolTable.BaseType.ERROR
          && testType != SymbolTable.BaseType.BOOL
          && testType != SymbolTable.BaseType.INT) {
        semanticError(node, "if condition must be bool or int");
      }
    }
    if (node.thenpart != null) {
      node.thenpart.accept(this, level);
    }
    if (node.elsepart != null && !(node.elsepart instanceof NilExp)) {
      node.elsepart.accept(this, level);
    }
    lastType = SymbolTable.BaseType.VOID;
  }

  @Override
  public void visit(WhileExp node, int level) {
    if (node.test != null) {
      node.test.accept(this, level);
      SymbolTable.BaseType testType = lastType;
      if (testType != SymbolTable.BaseType.ERROR
          && testType != SymbolTable.BaseType.BOOL
          && testType != SymbolTable.BaseType.INT) {
        semanticError(node, "while condition must be bool or int");
      }
    }
    if (node.body != null) {
      node.body.accept(this, level);
    }
    lastType = SymbolTable.BaseType.VOID;
  }

  @Override
  public void visit(ReturnExp node, int level) {
    if (currentFunctionReturnType == null) {
      lastType = SymbolTable.BaseType.VOID;
      return;
    }

    if (node.exp == null || node.exp instanceof NilExp) {
      if (currentFunctionReturnType != SymbolTable.BaseType.VOID) {
        semanticError(node, "non-void function must return a value");
      }
    } else {
      node.exp.accept(this, level);
      SymbolTable.BaseType retType = lastType;

      if (currentFunctionReturnType == SymbolTable.BaseType.VOID) {
        semanticError(node, "void function cannot return a value");
      } else if (retType != SymbolTable.BaseType.ERROR
          && retType != currentFunctionReturnType) {
        semanticError(node, "return type mismatch: expected "
            + typeToString(currentFunctionReturnType) + " but got " + typeToString(retType));
      }
    }
    lastType = SymbolTable.BaseType.VOID;
  }

  @Override
  public void visit(CompoundExp node, int level) {
    // new nested scope for { ... }
    symtab.enterScope();
    Context saved = currentContext;
    currentContext = Context.LOCAL;

    if (node.decs != null) {
      node.decs.accept(this, level + 1);
    }
    if (node.stmts != null) {
      node.stmts.accept(this, level + 1);
    }

    // print this block's scope before leaving
    printCurrentScope(level + 1);
    symtab.exitScope();
    currentContext = saved;
    lastType = SymbolTable.BaseType.VOID;
  }

  @Override
  public void visit(SimpleVar node, int level) {
    SymbolTable.Symbol sym = symtab.lookup(node.name);
    if (sym == null) {
      semanticError(node, "undefined variable '" + node.name + "'");
      lastType = SymbolTable.BaseType.ERROR;
      lastIsArray = false;
      return;
    }

    if (sym.kind == SymbolTable.Kind.FUNCTION) {
      semanticError(node, "'" + node.name + "' is a function, not a variable");
      lastType = SymbolTable.BaseType.ERROR;
      lastIsArray = false;
      return;
    }

    lastType = sym.baseType;
    lastIsArray = sym.isArray;
  }

  @Override
  public void visit(IndexVar node, int level) {
    SymbolTable.Symbol sym = symtab.lookup(node.name);
    if (sym == null) {
      semanticError(node, "undefined variable '" + node.name + "'");
      lastType = SymbolTable.BaseType.ERROR;
      lastIsArray = false;
      return;
    }

    if (!sym.isArray) {
      semanticError(node, "'" + node.name + "' is not an array");
    }

    if (node.index != null) {
      node.index.accept(this, level);
      SymbolTable.BaseType indexType = lastType;
      if (indexType != SymbolTable.BaseType.ERROR && indexType != SymbolTable.BaseType.INT) {
        semanticError(node, "array index must be int but got " + typeToString(indexType));
      }
    }

    lastType = sym.baseType;
    lastIsArray = false;
  }
}
