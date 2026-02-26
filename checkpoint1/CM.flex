/* Phase 1 placeholder lexer spec.
   File name: CM.flex
   Authors: Group 25- Richard Milovanov, Tameem Mughal, Muhammad Ali
   Date: 
   Purpose:  
*/

// PLACEHOLDER
import java_cup.runtime.*;

%%
%class Lexer
%cup
%line
%column

%eofval{
  return null;
%eofval};

%{
  private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
  }
%}

WS = [ \t\r\n]+

%%

{WS}   { /* skip */ }
.      { return symbol(sym.ERROR); }