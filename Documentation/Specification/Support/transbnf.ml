
open String

type choice = string list
type choices = choice list
type entry = string * choices
type entries = entry list
type section = string * entries
type grammar = section list

type kind = 
  | Section of string 
  | Entry of string 
  | Choice of string list

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
	
let rec parse_choices l: choices * kind list =
  match l with
    | Choice rhs :: tl -> 
      let (choices,l) = parse_choices tl in
      (rhs :: choices, l)
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

let pp_choice choice = " & " ^ " & " ^ (concat " " choice) ^ " \\\\"

let pp_choices choices = List.map pp_choice choices

let pp_entry (header,choices) = 
  let h = Printf.sprintf "\\begin{tabular}{lll}" in
  let s = header ^ " & " ^ " ::= " ^ " & " ^ " \\\\"  in
  let e = Printf.sprintf "\\end{tabular}" in
  h :: s :: ((pp_choices choices) @ [e]) 

let pp_entries entries = List.flatten (List.map pp_entry entries)

let pp_section (title,entries): string list = 
  let sec = Printf.sprintf "\\section{%s}" title in
  sec :: (pp_entries entries)

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
	      (* output_string oc s; *)
	incr line_counter
      done
    with End_of_file -> () in
  let read = List.rev !in_file in
  let g = parse_grammar read in
  let g = pp_grammar g in
  List.iter (fun x -> Printf.printf "%s\n" x) g
 
    
