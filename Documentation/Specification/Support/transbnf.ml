(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************)
open String

type kind = 
  | Section of string 
  | Entry of string 
  | Choice of string list

type prere = 
  | PNonterminal of string
  | PKeyword of string
  | POr
  | POpt
  | PStar
  | PPlus
  | PLpar
  | PRpar
  | PGroup of prere list

type choice = prere list
type choices = choice list
type entry = string * choices
type entries = entry list
type section = string * entries
type grammar = section list

let rec print_line line = 
  match line with
    | [] -> ()
    | x :: tl -> print_string x; print_string "."; print_line tl

let print_kind k = 
  match k with
    | Section s -> Printf.printf "Section %s\n" s
    | Entry s -> Printf.printf "Entry %s\n" s
    | Choice l -> print_line l

let rec split_aux line n word words = 
  if n = length line then 
    if word = [] 
    then words 
    else word :: words
  else 
    let c = get line n in
    if c = ' ' && word != [] 
    then split_aux line (n + 1) [] (word :: words)
    else 
      if c = ' ' 
      then split_aux line (n + 1) [] words
      else split_aux line (n + 1) (Char.escaped c :: word) words

let split line = 
  let l = split_aux line 0 [] [] in
  let l = List.map List.rev l in
  let l = List.map (concat "") l in
  List.rev l

let analyze line line_counter = 
  let l = split line in
  let b1 = List.mem "$$$$$" l
  and b2 = List.mem "::=" l
  and b3 = List.mem "|" l in
  match b1, b2, b3 with
    | true, false, false -> 
      if List.nth l 0 = "$$$$$" then Section (concat " " (List.tl l)) 
      else
	let error_msg = "Error: pattern $$$$$ does not start line " ^ (string_of_int line_counter) ^ " |" ^ line ^ "| " in
	failwith error_msg
    | false, true, false -> 
      if List.length l != 2 
      then 
	let error_msg = "Error: line " ^ (string_of_int line_counter) ^ " has a pattern ::= but more than two elements" ^ " |" ^ line ^ "| "
	in failwith error_msg
      else
	if List.nth l 1 <> "::="
	then
	  let error_msg = "Error: pattern ::= does not end line " ^ (string_of_int line_counter) ^ " |" ^ line ^ "| " in
	  print_string (List.nth l 1);
	  failwith error_msg
	else Entry (List.nth l 0)
    | false, false, true ->
      if (List.nth l 0) <> "|"
      then 
	let error_msg = "Error: pattern | does not start line " ^ (string_of_int line_counter) ^ " |" ^ line ^ "| " ^ (List.nth l 0 ) ^ " instead"  in
	failwith error_msg
      else Choice (List.tl l)
    | false, false, false -> 
      let error_msg = "Error: cannot recognize line " ^ (string_of_int line_counter) ^ " |" ^ line ^ "| "  in 
      failwith error_msg
    | _ -> 
      let error_msg = "Error: ambiguous line " ^ (string_of_int line_counter) ^ " |" ^ line ^ "| " in
      failwith error_msg

let first s = get s 0
let last s = get s (length s - 1)
let remove_last s = sub s 0 (length s - 1)
let remove_first s = sub s 1 (length s - 1)

let identify c len = 
  match c with  
    | '+' -> if len = 1 then None else Some PPlus 
    | '*' -> if len = 1 then None else Some PStar
    | '?' -> if len = 1 then None else Some POpt
    | '|' -> if len = 1 then Some POr else None
    | '(' -> Some PLpar
    | ')' -> Some PRpar
    | _ -> None

let escape s = 
  match s with
    | "{" -> "\\{"
    | "}" -> "\\}"
    | "^" -> "CHAPEAU"
    | "#" -> "\\#"
    | "_" -> "\\_"
    | "|->" -> "\mapsto"
    | "[[[" -> "\\llbracket"
    | "]]]" -> "\\rrbracket"
    | "->" -> "\\rightarrow"
    | "<-" -> "\\leftarrow"
    | "[([" -> "("
    | "])]" -> ")"
    | "=>" -> "\\Rightarrow"
    | _ -> s

let rec explode s: choice = 
  if length s <= 0 then [] else
    match identify (last s) (length s) with
      | Some x -> explode (remove_last s) @ [x]
      | None -> match identify (first s) (length s) with
	  | Some x -> x :: explode (remove_first s)
	  | None -> 
	    if first s >= 'A' && first s <= 'Z' 
	    then [PNonterminal s]
	    else [PKeyword (escape s)]  
	    
let rec scan_regexp l: choice = 
  match l with
    | [] -> []
    | hd :: tl ->
      let res = scan_regexp tl in
      (explode hd) @ res

let rec group_regexp l = 
  match l with 
    | [] -> ([],[])
    | PLpar :: tl ->
      let (res, rem) = group_regexp tl in
      let (behind,rem) = group_regexp rem in
      (PGroup res :: behind, rem)
    | PRpar :: tl ->
      ([], tl)
    | hd :: tl -> 
      let (res, rem) = group_regexp tl in
      (hd :: res, rem)

let rec pp_prere p = 
  match p with
    | POr -> " $^|$"
    | PStar -> "$^*$"
    | POpt -> "$^?$"
    | PPlus -> "$^+$"
    | PNonterminal s -> Printf.sprintf " $\\mathsf{%s}$" s
    | PKeyword s -> Printf.sprintf " $\\mathbf{%s}$" s
    | PGroup l -> " $\\rbag$ " ^ concat "" (List.map pp_prere l) ^ " $\\lbag$"
    | PLpar -> failwith "A regexp has a left parenthis while pretty printing"
    | PRpar -> failwith "A regexp has a right parenthis while pretty printing"

let parse_regexp l: choice = 
  let nl = scan_regexp l in
  let (nl, rem) = group_regexp nl in 
  if rem <> [] then 
    let error_msg = "Error: failed to recognize the following reg exp: " ^ (concat " " l) ^ " found the following remainder: " ^ (concat " " (List.map pp_prere rem )) in
    failwith error_msg else
    nl

let rec parse_choices l: choices * kind list =
  match l with
    | Choice rhs :: tl -> 
      let (choices,l) = parse_choices tl in
      (parse_regexp rhs :: choices, l)
    | _ -> ([],l) 

let rec parse_entry l: entry option * kind list =
  match l with
    | Entry e :: tl -> 
      let (choices,rem) = parse_choices tl in
      (Some (e,choices), rem) 
    | _ -> (None,l)

let rec parse_entries l: entries * kind list = 
  match parse_entry l with
    | Some (e,choices), l -> 
      let (entries,rem) = parse_entries l in
      (((e,choices) :: entries), rem)
    | _ -> ([],l)

let parse_section l: section option * kind list = 
  match l with
    | Section s :: tl -> 
      let (res,rem) = parse_entries tl in
      (Some (s,res),rem)
    | _ -> (None,l)

let emptyg : grammar = []

let rec parse_sections l: grammar * kind list = 
  match parse_section l with
    | Some section, tl ->
      let (res,rem) = parse_sections tl in
      (section :: res,rem)
    | _ -> (emptyg,l)

let rec parse_grammar l = 
  match parse_sections l with
    | g,[] -> g
    | g,rem -> failwith "BNF parsing error"

let pp_choice (choice: choice) = " & " ^ " & " ^ (concat "" (List.map pp_prere choice)) ^ " \\\\"

let pp_choices (choices: choices) = List.map pp_choice choices

let pp_entry ((header,choices): string * choices)  = 
  let first = List.hd choices in
  let s = "$\\mathsf{" ^ header ^ "}$ & " ^ " $\\mathsf{::=}$ " ^ " &" ^ (concat "" (List.map pp_prere first)) ^ " \\\\"  in
  s :: (pp_choices (List.tl choices))

let pp_entries entries = List.flatten (List.map pp_entry entries)

let pp_section (title,entries): string list = 
  let sec = Printf.sprintf "\\section{%s} \n" title in
  let h = Printf.sprintf " \n\\begin{longtable}[l]{p{3cm}ll}" in
  let e = Printf.sprintf "\\end{longtable} \\hfill \n" in
  sec :: h :: ((pp_entries entries) @ [e])

let pp_sections sections: string list = 
  List.flatten (List.map pp_section sections) 

let pp_grammar = pp_sections

let _ = 
  let ic = open_in "../Data/bnf.txt" in
  let oc = open_out "../Data/bnf.tex" in
  let line_counter = ref 1 in
  let in_file = ref [] in
  let _ = 
    try 
      while true do
	let s = input_line ic in 
	if length s > 0 then in_file := (analyze s !line_counter) :: !in_file;
	incr line_counter
      done
    with End_of_file -> () in
  let read = List.rev !in_file in
  Printf.printf "Parsing...\n";
  flush stdout;
  let g = parse_grammar read in
  Printf.printf "Pretty printing...\n";
  flush stdout;
  let g = pp_grammar g in
  Printf.printf "Done!\n";
  flush stdout;
  List.iter (fun x -> let x = x ^ "\n" in output_string oc x) g
 
    
