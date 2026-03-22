/* C- Scanner (JFlex)
   Phase 2: tokens for CUP, row/column tracking, lexical error reporting.
*/

import java_cup.runtime.*;

%%
%class Lexer
%cup
%line
%column

%eofval{
  return new Symbol(sym.EOF);
%eofval}

%{
  private Symbol symbol(int type) {
    return new Symbol(type, yyline + 1, yycolumn + 1);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline + 1, yycolumn + 1, value);
  }
  private void reportError(String msg) {
    System.err.println("Lexical error at line " + (yyline + 1) + ", column " + (yycolumn + 1) + ": " + msg);
  }
%}

LineTerminator = \r|\n|\r\n
WhiteSpace     = [ \t\f] | {LineTerminator}
Digit          = [0-9]
Identifier     = [_a-zA-Z]([_a-zA-Z0-9])*
Number         = {Digit}+

%state COMMENT

%%

<YYINITIAL> {
  "/*"   { yybegin(COMMENT); }

  /* Keywords */
  "bool"     { return symbol(sym.BOOL); }
  "if"       { return symbol(sym.IF); }
  "else"     { return symbol(sym.ELSE); }
  "int"      { return symbol(sym.INT); }
  "void"     { return symbol(sym.VOID); }
  "return"   { return symbol(sym.RETURN); }
  "while"    { return symbol(sym.WHILE); }
  "true"     { return symbol(sym.TRUTH, true); }
  "false"    { return symbol(sym.TRUTH, false); }
  /* Identifiers and numbers (input/output are built-in functions, matched as ID) */
  {Identifier}  { return symbol(sym.ID, yytext()); }
  {Number}      { return symbol(sym.NUM, Integer.valueOf(yytext())); }

  /* Operators */
  "+"   { return symbol(sym.PLUS); }
  "-"   { return symbol(sym.MINUS); }
  "*"   { return symbol(sym.TIMES); }
  "/"   { return symbol(sym.DIVIDE); }
  "<"   { return symbol(sym.LT); }
  "<="  { return symbol(sym.LE); }
  ">"   { return symbol(sym.GT); }
  ">="  { return symbol(sym.GE); }
  "=="  { return symbol(sym.EQ); }
  "!="  { return symbol(sym.NE); }
  "="   { return symbol(sym.ASSIGN); }
  "~"   { return symbol(sym.NOT); }
  "||"  { return symbol(sym.OR); }
  "&&"  { return symbol(sym.AND); }

  /* Separators */
  ";"   { return symbol(sym.SEMI); }
  ","   { return symbol(sym.COMMA); }
  "["   { return symbol(sym.LBRACKET); }
  "]"   { return symbol(sym.RBRACKET); }
  "("   { return symbol(sym.LPAREN); }
  ")"   { return symbol(sym.RPAREN); }
  "{"   { return symbol(sym.LBRACE); }
  "}"   { return symbol(sym.RBRACE); }

  /* Whitespace: skip */
  {WhiteSpace}  { /* skip */ }

  /* Invalid character */
  .            {
    reportError("invalid character '" + yytext() + "' (code " + (int)yytext().charAt(0) + ")");
    return symbol(sym.ERROR);
  }
}

<COMMENT> {
  "*/"       { yybegin(YYINITIAL); }
  [^*\n\r]+  { /* skip */ }
  "*"        { /* skip */ }
  \n|\r|\r\n { /* skip */ }
  <<EOF>>    {
    reportError("unclosed comment");
    yybegin(YYINITIAL);
    return symbol(sym.ERROR);
  }
}
