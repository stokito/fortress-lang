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

syn region   fortressComment start="(\*" end="\*)" contains=fortressComment
syn match    fortressEnd    "\<end\>"

" syn match    fortressKeyword "\<getter\>"
syn keyword  fortressKeyword getter
syn keyword  fortressExternal import export

syn region   fortressString       start=+"+ skip=+\\\\\|\\"+ end=+"+
syn match    fortressNumber       "\<-\=\d\(_\|\d\)*[l|L|n]\?\>"

syn keyword  fortressType  api object trait value
syn keyword  fortressType  extends abstract comprises
syn keyword  fortressType  grammar component
syn keyword  fortressType  Any Boolean String self

syn keyword  fortressOperator println

syn keyword  fortressKeyword opr for

syn match    fortressThenErr    "\<then\>"

syn region   fortressNone matchgroup=fortressKeyword start="\<if\>" matchgroup=fortressKeyword end="\<then\>" contains=ALLBUT,fortressThenErr

syn region   fortressNone matchgroup=fortressKeyword start="\<do\>" matchgroup=fortressKeyword end="\<end\>" contains=ALLBUT,fortressEndErr

syn region   fortressApi matchgroup=fortressModule start="\<api\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr
syn region   fortressObject matchgroup=fortressModule start="\<object\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr
syn region   fortressTrait matchgroup=fortressModule start="\<trait\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr
syn region   fortressComponent matchgroup=fortressModule start="\<component\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr

if version >= 508 || !exists("did_fortress_syntax_inits")
  if version < 508
    let did_fortress_syntax_inits = 1
    command -nargs=+ HiLink hi link <args>
  else
    command -nargs=+ HiLink hi def link <args>
  endif

  HiLink fortressComment	   Comment
  HiLink fortressKeyword	   Keyword
  HiLink fortressExternal          Include
  HiLink fortressType		   Type
  HiLink fortressOperator	   Operator
  HiLink fortressEnd               Statement

  HiLink fortressString	           String
  HiLink fortressNumber	           Number
  
  HiLink fortressThenErr	   Error

  delcommand HiLink
endif

let b:current_syntax = "fortress"
