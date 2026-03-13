import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import absyn.*;

public class SemanticAnalyzer implements AbsynVisitor {

  private final SymbolTable symtab;
  private final PrintWriter symOut;
  private boolean hadError = false;

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

  private void declareVar(String name,
                          NameTy tyNode,
                          boolean isArray,
                          int arraySize,
                          int row,
                          int col) {
    SymbolTable.BaseType baseType = fromNameTy(tyNode);
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
    // nothing to do for just a type node in phase 2
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

    if (symtab.lookupCurrent(node.func) != null) {
      semanticError(node, "redefined identifier '" + node.func + "'");
      return;
    }

    SymbolTable.Symbol funSym =
        SymbolTable.Symbol.function(node.func, retType, paramTypes, node.row, node.col);
    symtab.insert(funSym);

    // new scope for function body + parameters
    symtab.enterScope();
    Context saved = currentContext;

    // parameters live in function scope
    currentContext = Context.PARAM;
    if (node.params != null) {
      node.params.accept(this, level + 1);
    }

    // function body (locals / statements)
    currentContext = Context.LOCAL;
    if (node.body != null) {
      node.body.accept(this, level + 1);
    }

    // print this function scope before leaving
    printCurrentScope(level + 1);
    symtab.exitScope();

    currentContext = saved;
  }

  @Override
  public void visit(NilExp node, int level) {
    // nothing to do
  }

  @Override
  public void visit(IntExp node, int level) {
    // nothing to do for phase 2
  }

  @Override
  public void visit(BoolExp node, int level) {
    // nothing to do for phase 2
  }

  @Override
  public void visit(VarExp node, int level) {
    if (node.variable != null) {
      node.variable.accept(this, level);
    }
  }

  @Override
  public void visit(CallExp node, int level) {
    SymbolTable.Symbol sym = symtab.lookup(node.func);
    if (sym == null) {
      semanticError(node, "undefined identifier '" + node.func + "'");
    }
    if (node.args != null) {
      node.args.accept(this, level);
    }
  }

  @Override
  public void visit(OpExp node, int level) {
    if (node.left != null) {
      node.left.accept(this, level);
    }
    if (node.right != null) {
      node.right.accept(this, level);
    }
  }

  @Override
  public void visit(AssignExp node, int level) {
    if (node.lhs != null) {
      node.lhs.accept(this, level);
    }
    if (node.rhs != null) {
      node.rhs.accept(this, level);
    }
  }

  @Override
  public void visit(IfExp node, int level) {
    if (node.test != null) {
      node.test.accept(this, level);
    }
    if (node.thenpart != null) {
      node.thenpart.accept(this, level);
    }
    if (node.elsepart != null && !(node.elsepart instanceof NilExp)) {
      node.elsepart.accept(this, level);
    }
  }

  @Override
  public void visit(WhileExp node, int level) {
    if (node.test != null) {
      node.test.accept(this, level);
    }
    if (node.body != null) {
      node.body.accept(this, level);
    }
  }

  @Override
  public void visit(ReturnExp node, int level) {
    if (node.exp != null && !(node.exp instanceof NilExp)) {
      node.exp.accept(this, level);
    }
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
  }

  @Override
  public void visit(SimpleVar node, int level) {
    SymbolTable.Symbol sym = symtab.lookup(node.name);
    if (sym == null) {
      semanticError(node, "undefined identifier '" + node.name + "'");
    }
  }

  @Override
  public void visit(IndexVar node, int level) {
    SymbolTable.Symbol sym = symtab.lookup(node.name);
    if (sym == null) {
      semanticError(node, "undefined identifier '" + node.name + "'");
    }
    if (node.index != null) {
      node.index.accept(this, level);
    }
  }
}