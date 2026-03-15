/*
  file name: SymbolTable.java
  Name: Group 25 - Tameem Mughal, Richard, Muhammad Ali
  Date: Mar 15 2026
  Purpose: Symbol table implementation for semantic analysis. 
*/

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

  public enum BaseType {
    INT,
    BOOL,
    VOID,
    ERROR
  }

  public enum Kind {
    VAR,
    ARRAY,
    FUNCTION,
    PARAMETER
  }

  public static class Symbol {
    public final String name;
    public final Kind kind;
    public final BaseType baseType;

    // array info (for vars / params)
    public final boolean isArray;
    public final int arraySize; // 0 if not specified / not an array

    // function info
    public final BaseType returnType;
    public final List<BaseType> paramTypes;
    public final List<Boolean> paramIsArray;
    public final boolean isPrototype;

    // declaration position
    public final int row;
    public final int col;

    private Symbol(
        String name,
        Kind kind,
        BaseType baseType,
        boolean isArray,
        int arraySize,
        BaseType returnType,
        List<BaseType> paramTypes,
        List<Boolean> paramIsArray,
        boolean isPrototype,
        int row,
        int col) {
      this.name = name;
      this.kind = kind;
      this.baseType = baseType;
      this.isArray = isArray;
      this.arraySize = arraySize;
      this.returnType = returnType;
      this.paramTypes = paramTypes;
      this.paramIsArray = paramIsArray;
      this.isPrototype = isPrototype;
      this.row = row;
      this.col = col;
    }

    public static Symbol variable(
        String name,
        Kind kind,
        BaseType baseType,
        boolean isArray,
        int arraySize,
        int row,
        int col) {
      return new Symbol(
          name,
          kind,
          baseType,
          isArray,
          arraySize,
          null,
          null,
          null,
          false,
          row,
          col);
    }

    public static Symbol function(
        String name,
        BaseType returnType,
        List<BaseType> paramTypes,
        List<Boolean> paramIsArray,
        boolean isPrototype,
        int row,
        int col) {
      // baseType is the return type for convenience
      return new Symbol(
          name,
          Kind.FUNCTION,
          returnType,
          false,
          0,
          returnType,
          new ArrayList<>(paramTypes != null ? paramTypes : new ArrayList<>()),
          paramIsArray != null ? new ArrayList<>(paramIsArray) : new ArrayList<>(),
          isPrototype,
          row,
          col);
    }

    public static Symbol function(
        String name,
        BaseType returnType,
        List<BaseType> paramTypes,
        List<Boolean> paramIsArray,
        int row,
        int col) {
      return function(name, returnType, paramTypes, paramIsArray, false, row, col);
    }

    public static Symbol function(
        String name,
        BaseType returnType,
        List<BaseType> paramTypes,
        int row,
        int col) {
      return function(name, returnType, paramTypes, new ArrayList<>(), false, row, col);
    }
  }

  private final Deque<Map<String, Symbol>> scopes = new ArrayDeque<>();

  public SymbolTable() {
  }

  public void enterScope() {
    scopes.push(new HashMap<>());
  }

  public Map<String, Symbol> exitScope() {
    if (scopes.isEmpty()) {
      return null;
    }
    return scopes.pop();
  }

  public boolean insert(Symbol symbol) {
    if (scopes.isEmpty()) {
      enterScope();
    }
    Map<String, Symbol> current = scopes.peek();
    if (current.containsKey(symbol.name)) {
      return false;
    }
    current.put(symbol.name, symbol);
    return true;
  }

  public void replace(Symbol symbol) {
    if (!scopes.isEmpty()) {
      scopes.peek().put(symbol.name, symbol);
    }
  }

  public Symbol lookupCurrent(String name) {
    if (scopes.isEmpty()) {
      return null;
    }
    Map<String, Symbol> current = scopes.peek();
    return current.get(name);
  }

  public Symbol lookup(String name) {
    for (Map<String, Symbol> scope : scopes) {
      Symbol sym = scope.get(name);
      if (sym != null) {
        return sym;
      }
    }
    return null;
  }

  public Map<String, Symbol> currentScopeSnapshot() {
    if (scopes.isEmpty()) {
      return null;
    }
    return scopes.peek();
  }

  public int depth() {
    return scopes.size();
  }
}
