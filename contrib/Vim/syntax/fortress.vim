" Vim syntax file
" Language:     Fortress
" Filenames:    *.fsi *.fss
" Maintainers:  Jon Rafkind <rafkind@cs.utah.edu>
" URL:          http://www.cs.utah.edu/~rafkind
" Last Change:  2008 May 20 - Initial version

" Some stuff stolen from ocaml.vim

if version < 600
  syntax clear
elseif exists("b:current_syntax") && b:current_syntax == "fortress"
  finish
endif

" syn region   fortressComment start="(\*)" end="\\$" end="$"
syn region   fortressComment start="(\*)\@!" end="(\@<!\*)" contains=fortressComment
syn match    fortressLineComment "(\*).*"
syn match    fortressEnd    "\<end\>"

syn keyword  fortressKeyword   component api grammar label nextgroup=fortressName
syn keyword  fortressKeyword   trait object asif type nextgroup=fortressTypeName
syn keyword  fortressKeyword   var test property nextgroup=fortressSymbolName
syn keyword  fortressKeyword   abstract override value transient native private getter setter
syn keyword  fortressKeyword   extends comprises excludes throws requires ensures invariant
syn keyword  fortressKeyword   case most typecase of try catch forbid finally
syn keyword  fortressKeyword   if then elif else for while at also do
syn keyword  fortressKeyword   exit with throw spawn atomic tryatomic
syn keyword  fortressKeyword   self coerce asif typed fn or
syn keyword  fortressKeyword   where nat int bool unit opr coerces widens
syn keyword  fortressExternal  import export
syn keyword  fortressOperator  MIN MAX MINMAX CMP COMPOSE
syn keyword  fortressOperator  DOT TIMES DIV MOD REM GCD LCM DIVIDES CHOOSE SQRT
syn keyword  fortressOperator  NOT AND OR XOR IMPLIES BITNOT BITAND BITOR BITXOR LSHIFT RSHIFT
syn keyword  fortressOperator  IN NOTIN UNION INTERSECTION DIFFERENCE SYMDIFF
syn keyword  fortressOperator  SUBSET SUBSETEQ SUPSET SUPSETEQ
syn keyword  fortressOperator  BIG juxtaposition

syn region   fortressString       start=+"+ skip=+\\\\\|\\"+ end=+"+
syn match    fortressChar         "'.'"
syn match    fortressNumber       "\<-\=\d\(_\|\d\)*\(\.\d\(_\|\d\)*\)\="
syn keyword  fortressBoolean      true false

syn keyword  fortressType  Any Number Integral
syn keyword  fortressType  Boolean Char IntLiteral FloatLiteral FlatString String
syn keyword  fortressType  ZZ32 ZZ64 ZZ RR32 RR64 QQ NN64 NN32 IntLiteral FloatLiteral
"syn keyword  fortressType  Vector Matrix Range Maybe Lazy List Set Map Exception

syn keyword  fortressFunction  print println assert deny
syn keyword  fortressFunction  sin cos tan cot sec csc
syn keyword  fortressFunction  sinh cosh tanh coth sech csch
syn keyword  fortressFunction  arcsin arccos arctan arccot arcsec arccsc
syn keyword  fortressFunction  arsinh arcosh artanh arcoth arsech arcsch
syn keyword  fortressFunction  asin acos atan atan2 acot asec acsc
syn keyword  fortressFunction  asinh acosh atanh acoth asech acsch
syn keyword  fortressFunction  arg deg det exp inf sup lg ln log gcd max min
syn keyword  fortressFunction  floor ceiling truncate round
syn keyword  fortressFunction  odd even widen narrow signed unsigned big
syn keyword  fortressFunction  seq

syn match    fortressName        "\%(\%(component\|api\|grammar\|label\)\s\+\)\@<=\h\w*"
syn match    fortressSymbolName  "\%(\%(var\|test\|property\)\s\+\)\@<=\h\w*"
syn match    fortressTypeName    "\%(\%(trait\|object\|asif\|type\)\s\+\)\@<=\h\w*"

syn match    fortressDelimiter  "(\*\@!\|\*\@<!)\|\[\|\]\|{\|}"
syn match    fortressDelimiter  "<\+|\+\||\+>\+\||\+\|(\(/\+\|\\\+\)\|\(/\+\|\\\+\))"
syn match    fortressDelimiter  "\(\[\|{\|<\+\||\+\)[.*]\=\(\(/[.*]\=\)*/\|\(\\[.*]\=\)*\\\)"
syn match    fortressDelimiter  "\(/\([.*]\=/\)*\|\\\([.*]\=\\\)*\)[.*]\=\(\]\|}\|>\+\||\+\)"
syn match    fortressDelimiter  "[\u2045-\u2046\u2308-\u230b\u27c5-\u27c6\u27e6-\u27ec]"
syn match    fortressDelimiter  "[\u2983-\u2998\u29d8-\u29db\u29fc-\u29fd\u300c-\u3011]"
syn match    fortressDelimiter  "[\u3014-\u3019]\|[\u2016\u2af4]"

syn match    fortressThenErr    "\<then\>"
syn match    fortressCaseErr    "\<case\>"
syn match    fortressCatchErr    "\<catch\>"
syn match    fortressTypecaseErr    "\<of\>"

syn region   fortressNone matchgroup=fortressKeyword start="\<if\>" matchgroup=fortressKeyword end="\<then\>" contains=ALLBUT,fortressThenErr nextgroup=fortressIf
syn region   fortressIf matchgroup=fortressKeyword start="\<elif\>" matchgroup=fortressKeyword end="\<then\>" contains=ALLBUT,fortressThenErr nextgroup=fortressIf
syn region   fortressIf matchgroup=fortressKeyword start="\<then\>" matchgroup=fortressKeyword end="\<end\>" contains=ALLBUT,fortressThenErr

syn region   fortressNone matchgroup=fortressKeyword start="\<case\>" matchgroup=fortressKeyword end="\<of\>" contains=ALLBUT,fortressCaseErr nextgroup=fortressCase
syn region   fortressCase matchgroup=fortressKeyword start="\<in\>" matchgroup=fortressKeyword end="\<end\>" contains=ALLBUT,fortressCaseErr

syn region   fortressNone matchgroup=fortressKeyword start="\<typecase\>" matchgroup=fortressKeyword end="\<of\>" contains=ALLBUT,fortressTypecaseErr nextgroup=fortressTypecase
syn region   fortressTypecase matchgroup=fortressKeyword start="\<of\>" matchgroup=fortressKeyword end="\<end\>" contains=ALLBUT,fortressEndErr

syn region   fortressNone matchgroup=fortressKeyword start="\<try\>" matchgroup=fortressKeyword end="\<catch\>" contains=ALLBUT,fortressCatchErr nextgroup=fortressCatch
syn region   fortressCatch matchgroup=fortressKeyword start="\<catch\>" matchgroup=fortressKeyword end="\<end\>" contains=ALLBUT,fortressEndErr

syn region   fortressNone matchgroup=fortressKeyword start="\<do\>" matchgroup=fortressKeyword end="\<end\>" contains=ALLBUT,fortressEndErr

syn region   fortressApi matchgroup=fortressModule start="\<api\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr
syn region   fortressObject matchgroup=fortressModule start="\<object\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr
syn region   fortressTrait matchgroup=fortressModule start="\<trait\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr
syn region   fortressComponent matchgroup=fortressModule start="\<component\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr

if exists("fortress_minlines")
  let b:fortress_minlines = fortress_minlines
else
  let b:fortress_minlines = 50
end
exec "syn sync minlines=" . b:fortress_minlines

if version >= 508 || !exists("did_fortress_syntax_inits")
  if version < 508
    let did_fortress_syntax_inits = 1
    command -nargs=+ HiLink hi link <args>
  else
    command -nargs=+ HiLink hi def link <args>
  endif

  HiLink fortressComment	   Comment
  HiLink fortressLineComment	   Comment
  HiLink fortressKeyword	   Keyword
  HiLink fortressExternal          Include
  HiLink fortressType		   Type
  HiLink fortressOperator	   Operator
  HiLink fortressFunction	   Function
  HiLink fortressEnd               Statement

  HiLink fortressString	           String
  HiLink fortressChar	           String
  HiLink fortressNumber	           Number
  HiLink fortressBoolean           Boolean

  HiLink fortressName              Identifier
  HiLink fortressTypeName          Identifier
  HiLink fortressSymbolName        Identifier

  HiLink fortressDelimiter         Delimiter
  
  HiLink fortressThenErr	   Error

  delcommand HiLink
endif

let b:current_syntax = "fortress"
