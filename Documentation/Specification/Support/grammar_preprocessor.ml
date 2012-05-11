(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************)


let read_file path: string list = 
  let ic = open_in path in
  let in_file = ref [] in
  let _ = 
    try 
      while true do
	let s = input_line ic in 
	in_file := s :: !in_file;
      done
    with End_of_file -> () in
  Sys.remove path;
  List.rev !in_file

let write_file path f = 
  let oc = open_out path in
  List.iter (fun s -> output_string oc (s ^ "\n")) f;
  close_out oc

let grammar_start = "%%%%% GRAMMAR %%%%%" 
let special = "%%%%%%%%%%%%%%%%%%%"

let is_grammar_start s = 
  s = grammar_start

let is_special s = 
  s = special

(* Must be called on a non terminals declaration section, and returns the
   list of non terminals and the remainder of the file, stripped from the non
   terminals and the special line*)
let rec parse_nonterminals l = 
  match l with
    | [] -> failwith "Reached the end of the file while parsing the non terminals"
    | hd :: tl -> 
      if is_special hd then ([],tl) else
	let (res,rem) = parse_nonterminals tl in
	if hd = "" then (res, rem) else
	  (String.sub hd 1 (String.length hd - 1) :: res, rem)
	  
let rec discard_grammar l = 
  match l with
    | [] -> failwith "Reached the end of the file while discarding the grammar"
    | hd :: tl -> if is_special hd then tl else discard_grammar tl

(* fst(find_grammar_section l) = what we scanned before encountering the
   grammar section

   snd(find_grammar_section l) = the remainder of the file, stripped from the
   grammar section header *)
let rec find_grammar_section l = 
  match l with 
    | [] -> ([],[])
    | hd :: tl -> 
      if is_grammar_start hd then ([],tl) else
	let (res,rem) = find_grammar_section tl in
	(hd :: res, rem)	

let fetch_grammar l = 
  let (before, rem) = find_grammar_section l in
  if rem = [] then (before,[],[]) else
    let (nt, rem) = parse_nonterminals rem in
    let after = discard_grammar rem in
    (before, nt, after)

let process nt = 
  let header = "{\\FortressMathsurround=0pt\n\\begin{longtable}[l]{p{3cm}ll}" in
  let footer = "\\end{longtable} }" in
  header :: (Transbnf.select_and_print nt) @ [footer]

let rec transform l = 
  let (before,nt,after) = fetch_grammar l in 
  if nt = [] && after = [] then before else
    let grammar = process nt in 
    let nt = List.map (fun s -> "%" ^ s) nt in
    before @ [grammar_start] @ ["\n"] @ nt @ ["\n"] @ [special] @ ["\n"] @ grammar @ ["\n"] @ [special] @ (transform after)
    
let preprocess path =
  let file = read_file path in
  let file = transform file in
  write_file path file  

let is_tick_file s = 
  if String.length s < 5 then false
  else if String.sub s (String.length s - 5) 5 = ".tick" then true
  else false

let rec find_all_tick_files path = 
  let files = Array.to_list (Sys.readdir path) in
  let files = List.map (fun x -> path ^ "/" ^ x) files in
  let directories = List.filter Sys.is_directory files in
  let tick_files = List.filter is_tick_file files in
  let res = List.map find_all_tick_files directories in
  tick_files @ List.flatten res

let _ = 
  let path = Array.get Sys.argv 1 in
  let texfiles = find_all_tick_files path in
  List.iter preprocess texfiles


