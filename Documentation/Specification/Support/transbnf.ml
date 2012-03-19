
open String

type choices = string list
type entry = string * choices
type entries = entry list
type section = string * entries
type grammar = section list

type kind = 
  | Section of string
  | Entry of string
  | Choice of string

let scan_section line =
  if length line < 5 then None else 
    if sub line 0 5 = "$$$$$" then 
      Some (sub line 5 (length line - 5))
    else None

let scan_entry line =
  let l = length line in
  if l < 3 then None else
    if sub line (l-3) 3 = "::=" then Some (sub line 0 (l - 3))
    else None

let scan_choice line line_counter = 
  let l = length line in
  if contains line '|' then
    let index = index line '|' in
    let _ =
      if contains_from line (index + 1) '|' then 
	let error_msg = "Warning: line " ^ string_of_int(line_counter) ^ " (" ^ line  ^ ")" ^ " has several choices.\n" in
	print_string error_msg 
    in
      Some (sub line (index + 1) (l - index - 1))
  else None

let analyze line line_counter = 
  match scan_choice line line_counter with
    | Some s -> Choice s
    | None -> 
      begin
	match scan_entry line with
	  | Some s -> Entry s
	  | None -> 
	    begin
	      match scan_section line with
		| Some s -> Section s
		| None -> 
		  let error_msg = "Failed to recognize line " ^ (string_of_int line_counter) ^ " ( " ^ line ^  " ). " in
		    failwith error_msg
	    end
      end
	
let parse line line_counter =
  if length line <= 0 then () else 
    let s = 
      match analyze line line_counter with
	| Section s -> Printf.sprintf "Section %s\n" s
	| Entry s -> Printf.sprintf "Entry %s\n" s
	| Choice s -> Printf.sprintf "Choice %s\n" s
    in () (* print_string s; flush stdout *)


let _ = 
  let ic = open_in "../Data/bnf.txt" in
  let oc = open_out "../Data/bnf.tex" in
  let line_counter = ref 1 in
  try 
    while true do
      let s = input_line ic in 
      let _ = parse s !line_counter in
      (* output_string oc s; *)
      incr line_counter
    done
  with End_of_file -> ()
