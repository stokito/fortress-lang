" Vim syntax file
" Language:     Fortress
" Filenames:    *.fsi *.fss
" Maintainers:  Jon Rafkind <rafkind@cs.utah.edu>
" URL:          http://www.cs.utah.edu/~rafkind
" Last Change:  2008 May 20 - Initial version

if version < 600
  syntax clear
elseif exists("b:current_syntax") && b:current_syntax == "fortress"
  finish
endif

syn region   fortressComment start="(\*" end="\*)" contains=fortressComment
syn match    fortressKeyword    "\<end\>"

syn match    fortressKeyword "\<getter\>"

syn keyword  fortressType  api object trait value
syn keyword  fortressType  extends abstract comprises

syn keyword  fortressOperator opr

syn region   fortressApi matchgroup=fortressModule start="\<api\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr
syn region   fortressObject matchgroup=fortressModule start="\<object\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr
syn region   fortressTrait matchgroup=fortressModule start="\<trait\>" matchgroup=fortressModule end="\<end\>" contains=ALLBUT,fortressEndErr

if version >= 508 || !exists("did_fortress_syntax_inits")
  if version < 508
    let did_fortress_syntax_inits = 1
    command -nargs=+ HiLink hi link <args>
  else
    command -nargs=+ HiLink hi def link <args>
  endif

  HiLink fortressComment	   Comment
  HiLink fortressKeyword	   Keyword
  HiLink fortressType		   Type
  HiLink fortressOperator	   Keyword

  delcommand HiLink
endif

let b:current_syntax = "fortress"
