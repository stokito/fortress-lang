;;;
;;    Copyright 2007 Sun Microsystems, Inc.,
;;    4150 Network Circle, Santa Clara, California 95054, U.S.A.
;;    All rights reserved.
;;
;;    U.S. Government Rights - Commercial software.
;;    Government users are subject to the Sun Microsystems, Inc. standard
;;    license agreement and applicable provisions of the FAR and its supplements.
;;
;;    Use is subject to license terms.
;;
;;    This distribution may include materials developed by third parties.
;;
;;    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
;;    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
;;
;;
;; Stuff for the EMACS "fortify" command (M-&)
;;


(global-set-key "\M-?" 'buffer-rotate-switch)
(global-set-key "\M-&" 'fortify)
(global-set-key "\C-x&" 'method-fortify)
(global-set-key "\M-_" 'emphasize)

(defvar buffer-rotate-switch-depth 0 "Depth of rotation for buffer-rotate-switch command")

(defun buffer-rotate-switch (prefix-arg)
  "Switch buffers in rotation.
Doing this command once is similar to typing C-X B Return to switch to
the default buffer, that is, the one second from the top of the buffer
list.  Typing this command n times in succession has the effect of pulling
the the n+1'th buffer on the buffer list to the top of the buffer list,
making it the current buffer."
  (interactive "p")
  (let ((n (length (buffer-list))))
    (cond ((eq last-command 'buffer-rotate-switch)
	   (setq buffer-rotate-switch-depth
		 (mod (+ 1 buffer-rotate-switch-depth) n)))
	  (t (setq buffer-rotate-switch-depth 0)))
    (let ((b buffer-rotate-switch-depth))
      (while (> b 0)
	(switch-to-buffer (nth buffer-rotate-switch-depth (buffer-list)))
	(setq b (- b 1))))
    (cond ((< (+ buffer-rotate-switch-depth 1) n)
	   (switch-to-buffer (nth (+ buffer-rotate-switch-depth 1) (buffer-list)))))))

(defun emphasize (prefix-arg)
  "Emphasize preceding word.
The strings \\emph{ and } are inserted before and after the word before point.
However, if the character before point is }, and the preceding { is in turn
preceded by \\emph, then the \\emph{ sequence is moved backward one word.
Thus, after typing a sequence of n words, one can mark them the TeX emphasis
simply by typing M-_ n times."
  (interactive "*p")
  (let* ((case-fold-search nil)
	 (old-point (point)))
    (cond ((= (char-before (point)) ?\})
	   (search-backward "{")
	   (goto-char (- (point) 5))
	   (unless (looking-at "\\\\emph")
	     (goto-char old-point)
	     (keyboard-quit))
	   (let ((old-emph-point (point)))
	     (backward-word 1)
	     (let ((new-emph-point (point)))
	       (goto-char old-emph-point)
	       (delete-char 6)
	       (goto-char new-emph-point)
	       (insert "\\emph{")
	       (goto-char old-point))))
	  (t (backward-word 1)
	     (insert "\\emph{")
	     (forward-word 1)
	     (insert "}")))))

;;; We assume these TeX macro definitions:
;;; \def\Fortress{\list{}{\leftmargin 1em
;;;                       \itemindent\listparindent
;;;                       \parsep 0pt plus 1pt}\item
;;;                       \FortressInnerMacros\tabbing}
;;; \def\endFortress{\endtabbing\endlist}
;;; \def\FortressMathsurround{\mathsurround=0.166666em}
;;; \def\KWD#1{\(\mathtt{#1}\FortressMathsurround\)}
;;; \def\OPR#1{\(\mathtt{#1}\FortressMathsurround\)}
;;; \def\TYP#1{\(\mathrm{#1}\FortressMathsurround\)}
;;; \def\VAR#1{\(\mathit{#1}\FortressMathsurround\)}
;;; \def\EXP#1{\({\FortressInnerMacros#1}\FortressMathsurround\)}
;;; \def\innerKWD#1{\mathrel\mathtt{#1}}
;;; \def\innerOPR#1{\mathbin\mathtt{#1}}
;;; \def\innerTYP#1{\mathrm{#1}}
;;; \def\innerVAR#1{\mathit{#1}}
;;; \def\FortressInnerMacros{\let\KWD\innerKWD\let\OPR\innerOPR
;;;   \let\TYP\innerTYP\let\VAR\innerVAR}

(defun fortify (prefix-arg)
  "Format text as Fortress code.
A portion of the text in the buffer is formatted as Fortress code
by inserting TeX commands and performing certain transformations.
With no prefix argument, the contiguous nonblank text preceding point
is formatted.  With ^U, the region is formatted.  With ^U^U, the
region is formatted into a side-by-side display showing formatted
code on the left and unformatted code in the \\tt font on the right.
A prefix argument of 2 or 5 causes the preceding nonblank text (if 2)
or the region (if 5) to be formatted as if the contents of a string.
A prefix argument of 3 or 6 causes the preceding nonblank text (if 3)
or the region (if 6) to be formatted as Fortress code and followed
by another copy, formatted as string contents, in parentheses.
For prefix arguments 2, 3, 5, and 6, the text to be formatted must
lie within a single line.  No matter what the prefix argument,
if the text to be formatted lies entirely within a single line and
the side-by-side option is not requested, then the formatted text
is simply surrounded by either \\KWD{...} or \\OPR{...} or \\VAR{...}
or \\TYP{...} or \\EXP{...} within the line; otherwise
a multiline LaTeX Fortress environment is created.  If the region
is to be processed and the first character in the region is %,
then the region is first copied, and within the copy all lines
have any initial % character removed before the text is formatted,
but if the first two characters in the region are %%, then
C-U M-X comment-region is called instead, to do a cleverer job
of removing the comment characters; thus the net result is
that the original region precedes the formatted code.
In this way, Fortress code can be kept within a TeX source file
as a comment and easily altered and reformatted as necessary.
The text to be formatted must not contain any ^P or ^Q characters,
which is not much of a restriction, since these are not valid in
Fortress source code)."
  (interactive "*p")
  (let* ((case-fold-search nil)
	 (extra-space-inserted nil)
	 (process-region (and prefix-arg (>= prefix-arg 4)))
	 (side-by-side (and prefix-arg (>= prefix-arg 16)))
	 (string-only (and prefix-arg (or (= prefix-arg 2) (= prefix-arg 5))))
	 (parenthetical-string (and prefix-arg (or (= prefix-arg 3) (= prefix-arg 6))))
	 (old-point (copy-marker (point)))
	 (end (copy-marker (cond (process-region
				  (region-end))
				 (t (re-search-backward "[^\n \t]" nil nil)
				    (forward-char)
				    (unless (= (point) (marker-position old-point))
				      (insert " ")  ;Need to delete this later!
				      (setq extra-space-inserted (copy-marker (point))))
				    (point)))))
	 (start (copy-marker (cond (process-region
				    (region-beginning))
				   (t (re-search-backward "\\(^\\| \\|\t\\)" nil t)
				      (if (looking-at "[ \t]") (forward-char))
				      (point)))))
	 (multi-line (> (count-lines start end) 1)))
    (cond ((and (< start end) (= (char-after (- (marker-position end) 1)) ?\n))
	   (set-marker end (- (marker-position end) 1))
	   (setq multi-line t)))
    (if (and multi-line (or string-only parenthetical-string)) (keyboard-quit))
    (goto-char (marker-position start))
    (if (re-search-forward "[]" (marker-position end) t) (keyboard-quit))
    (untabify start end)
    (cond ((and process-region
		(= (char-after (marker-position start)) ?\%)
		(or (null (char-before (marker-position start)))
		    (= (char-before (marker-position start)) ?\n)))
	   ;; Make a copy
	   (let ((region-data (buffer-substring (marker-position start) (marker-position end))))
	     (goto-char (marker-position end))
	     (insert "\n")
	     (set-marker start (point))
	     (insert region-data)
	     (set-marker end (point))
	     (goto-char (marker-position start))
             (cond ((looking-at "%%") (comment-region (marker-position start) (marker-position end) '(4)))
		   (t (delete-char 1)
		      ;; Remove leading % from each line
		      (process-fortify-rules '(("\n%" "\n")) (marker-position start) (marker-position end)))))))
    (cond (string-only
	   (process-fortify-rules fortify-brace-rules (marker-position start) (marker-position end))
	   (process-fortify-rules fortify-string-rules (marker-position start) (marker-position end))
	   (goto-char (marker-position start))
	   (insert "\\STR{")
	   (goto-char (marker-position end))
	   (insert-before-markers "}"))
	  (t (let ((parenthetical-data (and parenthetical-string
					    (buffer-substring (marker-position start) (marker-position end)))))
	       ;; indent-stack contains pairs (indentation point-for-start-of-line-data)
	       (let ((indent-stack '()))
		 (goto-char (marker-position start))
		 (let ((first-indent (current-indentation)))
		   (cond (side-by-side
			  (let ((line-start (point)))
			    (end-of-line)
			    (let ((data (buffer-substring line-start (point))))
			      (insert-before-markers " START")
			      (insert-before-markers data)
			      (insert-before-markers "END"))
			    (goto-char line-start))))
		   (insert "\\(")
		   (cond ((or side-by-side multi-line)
			  (cond ((= first-indent 0)
				 (setq indent-stack (list (list 0 (point)))))
				(t
				 ;; Establish indentation of first line
				 (insert "\\)LEFT\\tt")
				 (insert-char ?\~ first-indent)
				 (insert "RIGHT\\pushtabs\\=\\+\\(")
				 (setq indent-stack (list (list first-indent (point))
							  (list 0 (point)))))))))
		 (forward-line)
		 (while (< (point) (marker-position end))
		   ;; Invariant: indent-stack is nonempty
		   ;; Invariant: indentations in the stack are strictly monotonically decreasing
		   ;; Invariant: points-for-start-of-line-data in the stack are monotonically decreasing
		   ;; Invariant: we are just after a newline
		   (let ((k (current-indentation)))
		     (cond ((not (looking-at "^[ ]*$"))
			    ;; Line is nonblank
			    (backward-char)
			    (let ((decrements "") (poptabs ""))
			      (while (< k (car (car indent-stack)))
				(setq decrements (concat "\\-" decrements))
				(setq poptabs (concat "\\poptabs" poptabs))
				(setq indent-stack (cdr indent-stack)))
			      ;; Stack began with a 0, so stack cannot become empty
			      (insert "\\)")
			      (insert decrements)
			      (insert poptabs)
			      (insert "\\("))
			    (forward-line)
			    (cond (side-by-side
				   (let ((line-start (point)))
				     (end-of-line)
				     (let ((data (buffer-substring line-start (point))))
				       (insert-before-markers " START")
				       (insert-before-markers data)
				       (insert-before-markers "END")
				       (goto-char line-start)))))
			    ;; Invariant: stack is nonempty
			    (cond ((> k (car (car indent-stack)))
				   (let ((here (copy-marker (point))))
				     (goto-char (cadr (car indent-stack)))
				     (let ((desired-column (+ (current-column) k)))
				       (move-to-column desired-column)
				       (let ((c (char-before (point))))
					 (cond ((and (= (current-column) desired-column)
						     (or (and (= c ?\ ) (not (= (char-after (point)) ?\ )))
							 (= c ?\()
							 (= c ?\[)
							 (= c ?\{)
							 (and (or (= c ?\\) (= c ?\/))
							      (> k 1)
							      (let ((b (char-before (- (point) 1))))
								(or (= b ?\<) (= b ?\() (= b ?\[) (= b ?\{))))))
						;; We have a good place to align to
						(insert " \\null\\)\\pushtabs\\=\\+\\(")
						(goto-char (marker-position here)))
					       (t (goto-char (marker-position here))
						  ;; This line has to establish its own indentation
						  (insert "\\)LEFT\\tt")
						  (insert-char ?\~ (- k (car (car indent-stack))))
						  (insert "RIGHT\\pushtabs\\=\\+\\(")))))
				     (set-marker here nil)
				     (setq indent-stack (cons (list k (point)) indent-stack))))
				  (t (setq indent-stack (cons (list k (point)) (cdr indent-stack))))))))
		   ;; Step forward a line for while loop
		   (forward-line))
		 (goto-char (marker-position end))
		 (let ((decrements "") (poptabs ""))
		   (while (not (null (cdr indent-stack)))
		     (setq decrements (concat "\\-" decrements))
		     (setq poptabs (concat "\\poptabs" poptabs))
		     (setq indent-stack (cdr indent-stack)))
		   (insert-before-markers "\\)")
		   (insert-before-markers decrements)
		   (insert-before-markers poptabs)
		   (insert-before-markers "\\("))
		 (insert-before-markers "\\)"))
	       (process-fortify-rules fortify-brace-rules (marker-position start) (marker-position end))
	       (goto-char (marker-position start))
	       (let ((code-start (point)))
		 (while (re-search-forward "\\((\\*\\(.*\\)\\*)\\|\"[^\"\n]*\"\\|START.*END\\)" (marker-position end) t)
		   (cond ((= (char-before (point)) ?\))
			  ;; Comment
			  (replace-match "\\\\mathtt{(*}\\\\;\\\\hbox{\\\\rm \\2\\\\unskip}\\\\;\\\\mathtt{*)}" t nil)
			  (process-fortify-rules fortify-code-rules code-start (match-beginning 0)))
			 ((= (char-before (point)) ?\")
			  ;; String
			  (let ((mbeg (match-beginning 0))
				(here (copy-marker (point))))
			    (process-fortify-rules fortify-string-rules mbeg (point))
			    (goto-char mbeg)
			    (delete-char 1)
			    (insert "\\hbox{\\rm``\\STR{")
			    (goto-char (marker-position here))
			    (set-marker here nil)
			    (delete-char -1)
			    (insert "}''}")
			    (process-fortify-rules fortify-code-rules code-start mbeg)))
			 (t
			  ;; Side-by-side text
			  (let ((mbeg (match-beginning 0)))
			    (process-fortify-rules fortify-string-rules mbeg (point))
			    (process-fortify-rules fortify-code-rules code-start mbeg))))
		   (setq code-start (point)))
		 (process-fortify-rules fortify-code-rules code-start (marker-position end)))
	       (cond ((or side-by-side multi-line)
		      (goto-char (marker-position start))
		      (insert "\\begin{Fortress}\n")
		      (goto-char (marker-position end))
		      (insert-before-markers "\n\\end{Fortress}"))
		     (t (goto-char (marker-position start))
			(cond ((looking-at "\\\\(")
			       (replace-match "\\\\EXP{" t nil)))
			(goto-char (marker-position end))
			(backward-char 2)
			(cond ((looking-at "\\\\)")
			       (replace-match "}" t nil)))))
	       (process-fortify-rules fortify-final-fixups (marker-position start) (marker-position end))
	       (cond (parenthetical-string
		      (goto-char (marker-position end))
		      (insert-before-markers " (\\STR{")
		      (let ((place (point)))
			(insert-before-markers parenthetical-data) ;need to insert before old-point
			(set-marker start place))
		      (set-marker end (point))
		      (process-fortify-rules fortify-brace-rules (marker-position start) (marker-position end))
		      (process-fortify-rules fortify-string-rules (marker-position start) (marker-position end))
		      (insert-before-markers "})"))))))
	  (set-marker start nil)
	  (set-marker end nil)
	  (when extra-space-inserted
	    (goto-char (marker-position extra-space-inserted))
	    (delete-char -1)
	    (set-marker extra-space-inserted nil))
	  (let ((old-point-position (marker-position old-point)))
	    (set-marker old-point nil)
	    (goto-char old-point-position))))

;;; The bulk of the work is done by regexp-replacement.
;;; This routine processes a set of rules, one at a time, in order,
;;; on the portion of the buffer delimited by start and end.
;;; Each rule is simply a pair of a regexp and a replacement string.
;;; A subtlety is that the rule processor backs up by one character
;;; after each replacement.  This is because many rules match an
;;; extra character of right context that is not subject to replacement,
;;; and that character might be needed for a subsequent match.
;;; (In situations where more than one character of backup might be
;;; required, the solution is to have two copies of the rule in the list.)
;;; The one-character backup means that certain other rules need to be careful.
;;; For example, the rule in "fortify-brace-rules" that turns "}" into "\}"
;;; must check to make sure that the "}" is not preceded by a "\";
;;; otherwise the same "}" would be repeatedly replaced by "\}",
;;; leading to a runaway iteration (yes, this actually happened to me).

(defun process-fortify-rules (rules startpos endpos)
  (let ((old-point (copy-marker (point)))
	(end-marker (copy-marker endpos)))
    (while (not (null rules))
      (let ((regexp (car (car rules)))
	    (replacement (cadr (car rules))))
	(goto-char startpos)
	(while (re-search-forward regexp (marker-position end-marker) t)
	  (replace-match replacement t nil)
	  (goto-char (- (point) 1))))
      (setq rules (cdr rules)))
    (set-marker end-marker nil)
    (let ((old-point-position (marker-position old-point)))
      (set-marker old-point nil)
      (goto-char old-point-position))))

;; (defun process-fortify-rules (rules startpos endpos)
;;   (let ((old-point (copy-marker (point))))
;;     (goto-char endpos)
;;     (insert-before-markers " ")
;;     (let ((end-marker (copy-marker (point))))
;;       (while (not (null rules))
;; 	(let ((regexp (car (car rules)))
;; 	      (replacement (cadr (car rules))))
;; 	  (goto-char startpos)
;; 	  (while (re-search-forward regexp (marker-position end-marker) t)
;; 	    (replace-match replacement t nil)
;; 	    (goto-char (- (point) 1))))
;; 	(setq rules (cdr rules)))
;;       (goto-char (marker-position end-marker))
;;       (when (= (char-before (point)) ?\ )
;; 	(delete-char -1))
;;       (set-marker end-marker nil)
;;       (let ((old-point-position (marker-position old-point)))
;; 	(set-marker old-point nil)
;; 	(goto-char old-point-position)))))


(setq fortify-brace-rules
      '(
	;; Braces, observing interior space
	("{" "LB")
	("}" "RB")
	("LB " "\\\\{\\\\,")
	("LB" "\\\\{{}")
	(" RB" "\\\\,\\\\}")
	("RB" "\\\\}")
	;; Now fix up braces needed by indentation stuff: spaces adjacent
        ;; to them should not be converted to LaTeX thin-space commands!
	("LEFT" "{")
	("RIGHT" "}")
	))

(setq fortify-string-rules     ;also used for side-by-side text
      '(
	;; Have to undo damage done by the brace rules!  Also convert backslashes.
	;; Using LBX and RBX rather than LB and RB merely aids in debugging these rules.
	("\\\\{\\\\," "LBX ")
	("\\\\,\\\\}" " RBX")
	("\\\\{" "LBX")
	("\\\\}" "RBX")
	;; Must handle backslashes after unconverting the braces and before
        ;; converting any characters to use \char !  Note that the following
	;; character expansions carefully use no spaces, because later we
	;; convert spaces to tildes.
	("\\\\" "{\\\\char'134}")
	("LBX" "{\\\\char'173}")
	("RBX" "{\\\\char'175}")
	("#" "{\\\\char'43}")
	("\\$" "{\\\\char'44}")
	("%" "{\\\\char'45}")
	("&" "{\\\\char'46}")
	("_" "{\\\\char'137}")
	("\\^" "{\\\\char'136}")
	("~" "{\\\\char'176}")
	(" " "~")
	("START" "\\\\)\\\\`\\\\(\\\\hbox to 0pt{\\\\hss\\\\hbox to 0.5\\\\linewidth{\\\\tt{}")
	("END" "\\\\hfill}}")
	))

(setq fortify-code-rules
      '(
	;; Colon-equals-colon and colon-equals
	(":=:" "{}\\\\CONDEQ{}")
	(":=" "{}\\\\ASSIGN{}")
	;; Colons, observing space
	;;   Space after but not before is used for declarations
	;;   Space on neither side means it's a (typically infix) colon operator
	("\\([^ ]\\):\\([ ]\\)" "\\1{}\\\\COLON\\2")
	("\\([^ ]\\):\\([^ }]\\)" "\\1{}\\\\COLONOP{}\\2")
	(":\\([^}]\\)" "{}\\\\mathrel{\\\\mathtt{:}}\\1")
	;; Exponentiation: consider various forms for the exponent
	;;   Parameterized type (used after comprehensions)
	("\\^\\([a-zA-Z0-9_']+\\[\\\\[a-zA-Z0-9_ ,]*\\(\\[\\\\[a-zA-Z0-9_ ,]*\\\\\\]\\)\\\\\\]\\)" "^{ \\1 }")
	;;   Parameterized type with no nesting
	("\\^\\([a-zA-Z0-9_']+\\[\\\\[a-zA-Z0-9_ ,]*\\\\\\]\\)" "^{ \\1 }")
	;;   Parenthesized sums/diffs/dots/quotients of juxtapositions of variables and literals (no sum/diff/dot required)
	("\\^(\\(\\([+-][ ]*\\)?[A-Za-z0-9]+[']*\\([ ]+[A-Za-z0-9]+[']*\\)*\\([ ]*\\(+\\|-\\|DOT\\|/\\)[ ]*[A-Za-z0-9]+[']*\\([ ]+[A-Za-z0-9]+[']*\\)*\\)*\\))"
	 "^{ \\1 }")
	;;   Simple name or literal
	("\\^\\([A-Za-z0-9_.']+\\)" "^{ \\1 }")
	;; Subscripts
	;;  Single-dimensional
	;;     Single letter or digit with optional prime marks
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z0-9][']*\\)\\]" "\\1{}_{\\2}")
	;;     Multidigit numeral
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([1-9][0-9]+\\)\\]" "\\1{}_{\\2}")
	;;     Single letter followed by digits with optional prime marks
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z]\\)\\([0-9]+\\)\\([']*\\)\\]" "\\1{}_{\\2{\\4}_{\\3}}")
	;;     Single letter, underscore, and label
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z]\\)_\\([A-Za-z][A-Za-z0-9]+\\)\\]" "\\1{}_{{\\2}_{\\\\mathrm{\\3}}}")
	;;     Sums/diffs/dots of juxtapositions of variables and literals (at least one sum/diff/dot required)
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\(\\([+-][ ]*\\)?[A-Za-z0-9]+[']*\\([ ]+[A-Za-z0-9]+[']*\\)*\\([ ]*\\(+\\|-\\|DOT\\)[ ]*[A-Za-z0-9]+[']*\\([ ]+[A-Za-z0-9]+[']*\\)*\\)+\\)\\]" "\\1{}_{\\2}")
	;;  Two-dimensional
	;;    Each either a single letter or digit, or single letter followed by digits, with optional prime marks
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z0-9][']*\\),\\([A-Za-z0-9][']*\\)\\]" "\\1{}_{\\2\\3}")
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z0-9][']*\\),\\([A-Za-z]\\)\\([0-9]+\\)\\([']*\\)\\]" "\\1{}_{\\2\\3{\\5}_{\\4}}")
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z]\\)\\([0-9]+\\)\\([']*\\),\\([A-Za-z0-9][']*\\)\\]" "\\1{}_{\\2{\\4}_{\\3}\\5}")
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z]\\)\\([0-9]+\\)\\([']*\\),\\([A-Za-z]\\)\\([0-9]+\\)\\([']*\\)\\]" "\\1{}_{\\2{\\4}_{\\3}\\5{\\7}_{\\6}}")
	;;  Three-dimensional
	;;    Each either a single letter or digit with optional prime marks, or a single letter followed by digits (no prime marks)
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z0-9][']*\\),\\([A-Za-z0-9][']*\\),\\([A-Za-z0-9][']*\\)\\]" "\\1{}_{\\2\\3\\4}")
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z0-9][']*\\),\\([A-Za-z0-9][']*\\),\\([A-Za-z]\\)\\([0-9]+\\)\\]" "\\1{}_{\\2\\3{\\4}_{\\5}}")
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z0-9][']*\\),\\([A-Za-z]\\)\\([0-9]+\\),\\([A-Za-z0-9][']*\\)\\]" "\\1{}_{\\2{\\3}_{\\4}\\5}")
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z0-9][']*\\),\\([A-Za-z]\\)\\([0-9]+\\),\\([A-Za-z]\\)\\([0-9]+\\)\\]" "\\1{}_{\\2{\\3}_{\\4}{\\5}_{\\6}}")
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z]\\)\\([0-9]+\\),\\([A-Za-z0-9][']*\\),\\([A-Za-z0-9][']*\\)\\]" "\\1{}_{{\\2}_{\\3}\\4\\5}")
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z]\\)\\([0-9]+\\),\\([A-Za-z0-9][']*\\),\\([A-Za-z]\\)\\([0-9]+\\)\\]" "\\1{}_{{\\2}_{\\3}\\4{\\5}_{\\6}}")
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z]\\)\\([0-9]+\\),\\([A-Za-z]\\)\\([0-9]+\\),\\([A-Za-z0-9][']*\\)\\]" "\\1{}_{{\\2}_{\\3}{\\4}_{\\5}\\6}")
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z]\\)\\([0-9]+\\),\\([A-Za-z]\\)\\([0-9]+\\),\\([A-Za-z]\\)\\([0-9]+\\)\\]" "\\1{}_{{\\2}_{\\3}{\\4}_{\\5}{\\6}_{\\7}}")
	;;  Four-dimensional
	;;    Each either a single letter or digit with optional prime marks
	("\\(\\<[a-z][a-zA-Z0-9']*\\|\\<[A-Z]\\)\\[\\([A-Za-z0-9][']*\\),\\([A-Za-z0-9][']*\\),\\([A-Za-z0-9][']*\\),\\([A-Za-z0-9][']*\\)\\]" "\\1{}_{\\2\\3\\4\\5}")
	;;  Subscript that has generators
	;; (Note use of spaces around \\2 etc. to allow contents to be properly expanded)
	;;    Simple generator
	("\\([^ \t\n]\\)\\[\\([^],<]*<-\\([^][(){},]*\\|[^][(){},]*\\([[({][^][(){}]*[])}][^][(){},]*\\)*\\)\\)\\]"
	 "\\1{}\\\\limits_{ \\2 }")
	;;    Two generators
	("\\([^ \t\n]\\)\\[\\([^],<]*<-\\([^][(){},]*\\|[^][(){},]*\\([[({][^][(){}]*[])}][^][(){},]*\\)*\\)\\),\\([^][(){},]*\\|[^][(){},]*\\([[({][^][(){}]*[])}][^][(){},]*\\)*\\)\\]"
	 "\\1{}\\\\limits_{\\\\genfrac{}{}{0pt}{1}{ \\2 }{ \\5 }}")
	;;    Three generators
	("\\([^ \t\n]\\)\\[\\([^],<]*<-\\([^][(){},]*\\|[^][(){},]*\\([[({][^][(){}]*[])}][^][(){},]*\\)*\\)\\),\\([^][(){},]*\\|[^][(){},]*\\([[({][^][(){}]*[])}][^][(){},]*\\)*\\),\\([^][(){},]*\\|[^][(){},]*\\([[({][^][(){}]*[])}][^][(){},]*\\)*\\)\\]"
	 "\\1{}\\\\limits_{\\\\genfrac{}{}{0pt}{1}{\\\\genfrac{}{}{0pt}{1}{ \\2 }{ \\5 }}{ \\7 }}")
	;;    Four generators
	("\\([^ \t\n]\\)\\[\\([^],<]*<-\\([^][(){},]*\\|[^][(){},]*\\([[({][^][(){}]*[])}][^][(){},]*\\)*\\)\\),\\([^][(){},]*\\|[^][(){},]*\\([[({][^][(){}]*[])}][^][(){},]*\\)*\\),\\([^][(){},]*\\|[^][(){},]*\\([[({][^][(){}]*[])}][^][(){},]*\\)*\\),\\([^][(){},]*\\|[^][(){},]*\\([[({][^][(){}]*[])}][^][(){},]*\\)*\\)\\]"
	 "\\1{}\\\\limits_{\\\\genfrac{}{}{0pt}{1}{\\\\genfrac{}{}{0pt}{1}{\\\\genfrac{}{}{0pt}{1}{ \\2 }{ \\5 }}{ \\7 }}{ \\9 }}")
	;; Greek letters
	("\\([^_]\\)\\<ALPHA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{A}\\2")
	("\\([^_]\\)\\<alpha\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\alpha}\\2")
	("\\([^_]\\)\\<BETA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{B}\\2")
	("\\([^_]\\)\\<beta\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\beta}\\2")
	("\\([^_]\\)\\<GAMMA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Gamma}\\2")
	("\\([^_]\\)\\<gamma\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\gamma}\\2")
	("\\([^_]\\)\\<DELTA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Delta}\\2")
	("\\([^_]\\)\\<delta\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\delta}\\2")
	("\\([^_]\\)\\<EPSILON\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{E}\\2")
	("\\([^_]\\)\\<epsilon\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\epsilon}\\2")
	("\\([^_]\\)\\<ZETA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{Z}\\2")
	("\\([^_]\\)\\<zeta\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\zeta}\\2")
	("\\([^_]\\)\\<ETA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{H}\\2")
	("\\([^_]\\)\\<eta\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\eta}\\2")
	("\\([^_]\\)\\<THETA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Theta}\\2")
	("\\([^_]\\)\\<theta\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\theta}\\2")
	("\\([^_]\\)\\<IOTA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{I}\\2")
	("\\([^_]\\)\\<iota\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\iota}\\2")
	("\\([^_]\\)\\<KAPPA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{K}\\2")
	("\\([^_]\\)\\<kappa\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\kappa}\\2")
	("\\([^_]\\)\\<LAMBDA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Lambda}\\2")
	("\\([^_]\\)\\<lambda\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\lambda}\\2")
	("\\([^_]\\)\\<MU\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{M}\\2")
	("\\([^_]\\)\\<mu\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\mu}\\2")
	("\\([^_]\\)\\<NU\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{N}\\2")
	("\\([^_]\\)\\<nu\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\nu}\\2")
	("\\([^_]\\)\\<XI\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Xi}\\2")
	("\\([^_]\\)\\<xi\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\xi}\\2")
	("\\([^_]\\)\\<OMICRON\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{O}\\2")
	("\\([^_]\\)\\<omicron\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{o}\\2")
	("\\([^_]\\)\\<PI\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Pi}\\2")
	("\\([^_]\\)\\<pi\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\pi}\\2")
	("\\([^_]\\)\\<RHO\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{P}\\2")
	("\\([^_]\\)\\<rho\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\rho}\\2")
	("\\([^_]\\)\\<SIGMA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Sigma}\\2")
	("\\([^_]\\)\\<sigma\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\sigma}\\2")
	("\\([^_]\\)\\<TAU\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{T}\\2")
	("\\([^_]\\)\\<tau\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\tau}\\2")
	("\\([^_]\\)\\<UPSILON\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Upsilon}\\2")
	("\\([^_]\\)\\<upsilon\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\upsilon}\\2")
	("\\([^_]\\)\\<PHI\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Phi}\\2")
	("\\([^_]\\)\\<phi\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\phi}\\2")
	("\\([^_]\\)\\<CHI\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{X}\\2")
	("\\([^_]\\)\\<chi\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\chi}\\2")
	("\\([^_]\\)\\<PSI\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Psi}\\2")
	("\\([^_]\\)\\<psi\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\psi}\\2")
	("\\([^_]\\)\\<OMEGA\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\Omega}\\2")
	("\\([^_]\\)\\<omega\\([']*\\>\\|[0-9_]\\)" "\\1{}\\\\GREEK{\\\\omega}\\2")
	;; Trig functions etc.
	("\\([^_]\\)\\<arccos\\>" "\\1{}\\\\arccos{}")
	("\\([^_]\\)\\<arcsin\\>" "\\1{}\\\\arcsin{}")
	("\\([^_]\\)\\<arctan\\>" "\\1{}\\\\arctan{}")
;; arg?
	("\\([^_]\\)\\<cos\\>" "\\1{}\\\\cos{}")
	("\\([^_]\\)\\<cosh\\>" "\\1{}\\\\cosh{}")
	("\\([^_]\\)\\<cot\\>" "\\1{}\\\\cot{}")
	("\\([^_]\\)\\<coth\\>" "\\1{}\\\\coth{}")
	("\\([^_]\\)\\<csc\\>" "\\1{}\\\\csc{}")
;; deg?
	("\\([^_]\\)\\<det\\>" "\\1{}\\\\det{}")
;; dim?
	("\\([^_]\\)\\<exp\\>" "\\1{}\\\\exp{}")
	("\\([^_]\\)\\<gcd\\>" "\\1{}\\\\gcd{}")
;; hom?
	("\\([^_]\\)\\<inf\\>" "\\1{}\\\\inf{}")
;; ker?
	("\\([^_]\\)\\<lcm\\>" "\\1{}\\\\TYP{lcm}")
	("\\([^_]\\)\\<lg\\>" "\\1{}\\\\lg{}")
;; lim?
;; liminf?
;; limsup?
	("\\([^_]\\)\\<ln\\>" "\\1{}\\\\ln{}")
	("\\([^_]\\)\\<log\\>" "\\1{}\\\\log{}")
	("\\([^_]\\)\\<max\\>" "\\1{}\\\\max{}")
	("\\([^_]\\)\\<min\\>" "\\1{}\\\\min{}")
;; Pr?
	("\\([^_]\\)\\<sec\\>" "\\1{}\\\\sec{}")
	("\\([^_]\\)\\<sin\\>" "\\1{}\\\\sin{}")
	("\\([^_]\\)\\<sinh\\>" "\\1{}\\\\sinh{}")
	("\\([^_]\\)\\<sup\\>" "\\1{}\\\\sup{}")
	("\\([^_]\\)\\<tan\\>" "\\1{}\\\\tan{}")
	("\\([^_]\\)\\<tanh\\>" "\\1{}\\\\tanh{}")
	;; Mathematical operators
	("<->" "{}\\\\leftrightarrow{}")  ;must precede <- and ->
	("<-" "{}\\\\leftarrow{}")
	("|->" "{}\\\\mapsto{}")
	("->" "{}\\\\rightarrow{}")
	("\\([^_]\\)\\<IMPLIES\\>" "\\1{}\\\\rightarrow{}")
	("\\([^_]\\)\\<IFF\\>" "\\1{}\\\\leftrightarrow{}")
	("<=>" "{}\\\\Leftrightarrow{}")  ;must precede <= and =>
	("=>" "{}\\\\Rightarrow{}")
	("<=" "{}\\\\leq{}")
	(">=" "{}\\\\geq{}")
	("=/=" "{}\\\\neq{}")
	("~>" "{}\\\\leadsto{}")
	("===" "{}\\\\sequiv{}")
	("\\([^_]\\)\\<DOT\\>\\([^_]\\)" "\\1{}\\\\cdot{}\\2")
	("\\([^_]\\)\\<BY\\>\\([^_]\\)" "\\1{}\\\\times{}\\2")
	("\\([^_]\\)\\<TIMES\\>\\([^_]\\)" "\\1{}\\\\times{}\\2")
	("\\([^_]\\)\\<CROSS\\>\\([^_]\\)" "\\1{}\\\\times{}\\2")
	("\\([^_]\\)\\<UNION\\>\\([^_]\\)" "\\1{}\\\\cup{}\\2")
	("\\([^_]\\)\\<INTERSECTION\\>\\([^_]\\)" "\\1{}\\\\cap{}\\2")
	("\\([^_]\\)\\<CUP\\>\\([^_]\\)" "\\1{}\\\\cup{}\\2")
	("\\([^_]\\)\\<CAP\\>\\([^_]\\)" "\\1{}\\\\cap{}\\2")
	("\\([^_]\\)\\<BOTTOM\\>\\([^_]\\)" "\\1{}\\\\bot{}\\2")
	("\\([^_]\\)\\<TOP\\>\\([^_]\\)" "\\1{}\\\\top{}\\2")
	("\\([^_]\\)\\<SUM\\>\\([^_]\\)" "\\1{}\\\\sum{}\\2")
	("\\([^_]\\)\\<PRODUCT\\>\\([^_]\\)" "\\1{}\\\\prod{}\\2")
	("\\([^_]\\)\\<INTEGRAL\\>\\([^_]\\)" "\\1{}\\\\int{}\\2")
	("\\([^_]\\)\\<EMPTYSET\\>\\([^_]\\)" "\\1{}\\\\emptyset{}\\2")
	("\\([^_]\\)\\<SEQUIV\\>\\([^_]\\)" "\\1{}\\\\sequiv{}\\2")
	("\\([^_]\\)\\<EQUIV\\>\\([^_]\\)" "\\1{}\\\\equiv{}\\2")
	("\\([^_]\\)\\<NOTEQUIV\\>\\([^_]\\)" "\\1{}\\\\not\\\\equiv{}\\2")
	("\\([^_]\\)\\<EQ\\>\\([^_]\\)" "\\1=\\2")
	("\\([^_]\\)\\<LT\\>\\([^_]\\)" "\\1<{}\\2")
	("\\([^_]\\)\\<GT\\>\\([^_]\\)" "\\1>\\2")
	("\\([^_]\\)\\<LE\\>\\([^_]\\)" "\\1{}\\\\leq{}\\2")
	("\\([^_]\\)\\<GE\\>\\([^_]\\)" "\\1{}\\\\geq{}\\2")
	("\\([^_]\\)\\<NE\\>\\([^_]\\)" "\\1{}\\\\neq{}\\2")
	("\\([^_]\\)\\<AND\\>\\([^_]\\)" "\\1{}\\\\wedge{}\\2")
	("\\([^_]\\)\\<OR\\>\\([^_]\\)" "\\1{}\\\\vee{}\\2")
	("\\([^_]\\)\\<NOT\\>\\([^_]\\)" "\\1{}\\\\neg{}\\2")
	("\\([^_]\\)\\<XOR\\>\\([^_]\\)" "\\1{}\\\\xor{}\\2")
	("\\([^_]\\)\\<NAND\\>\\([^_]\\)" "\\1{}\\\\nand{}\\2")
	("\\([^_]\\)\\<NOR\\>\\([^_]\\)" "\\1{}\\\\nor{}\\2")
	("\\([^_]\\)\\<INF\\>\\([^_]\\)" "\\1{}\\\\infty{}\\2")
	("\\([^_]\\)\\<INFINITY\\>\\([^_]\\)" "\\1{}\\\\infty{}\\2")
	("\\([^_]\\)\\<SQRT\\>\\([^_]\\)" "\\1{}\\\\surd{}\\2")
	("\\([^_]\\)\\<EQV\\>\\([^_]\\)" "\\1{}\\\\equiv{}\\2")
	("\\([^_]\\)\\<EQUIV\\>\\([^_]\\)" "\\1{}\\\\equiv{}\\2")
	("\\([^_]\\)\\<CIRC\\>\\([^_]\\)" "\\1{}\\\\circ{}\\2")
	("\\([^_]\\)\\<RING\\>\\([^_]\\)" "\\1{}\\\\circ{}\\2")
	("\\([^_]\\)\\<COMPOSE\\>\\([^_]\\)" "\\1{}\\\\circ{}\\2")
	("\\([^_]\\)\\<VDASH\\>\\([^_]\\)" "\\1{}\\\\vdash{}\\2")
	("\\([^_]\\)\\<TURNSTILE\\>\\([^_]\\)" "\\1{}\\\\vdash{}\\2")
	("\\([^_]\\)\\<FORALL\\>\\([ \t]*([^()]*)\\)" "\\1{}\\\\forall{}\\2\\\\;")
	("\\([^_]\\)\\<FORALL\\>\\([^_]\\)" "\\1{}\\\\forall{}\\2")
	("\\([^_]\\)\\<EXISTS\\>\\([^_]\\)" "\\1{}\\\\exists{}\\2")
	("\\([^_]\\)\\<BIGAND\\>\\([^_]\\)" "\\1{}\\\\bigwedge{}\\2")
	("\\([^_]\\)\\<ALL\\>\\([^_]\\)" "\\1{}\\\\bigwedge{}\\2")
	("\\([^_]\\)\\<BIGOR\\>\\([^_]\\)" "\\1{}\\\\bigvee{}\\2")
	("\\([^_]\\)\\<ANY\\>\\([^_]\\)" "\\1{}\\\\bigvee{}\\2")
	("\\([^_]\\)\\<BIGCAP\\>\\([^_]\\)" "\\1{}\\\\bigcap{}\\2")
	("\\([^_]\\)\\<BIGINTERSECT\\>\\([^_]\\)" "\\1{}\\\\bigcap{}\\2")
	("\\([^_]\\)\\<BIGCUP\\>\\([^_]\\)" "\\1{}\\\\bigcup{}\\2")
	("\\([^_]\\)\\<BIGUNION\\>\\([^_]\\)" "\\1{}\\\\bigcup{}\\2")
	("\\([^_]\\)\\<OTIMES\\>\\([^_]\\)" "\\1{}\\\\otimes{}\\2")
	("\\([^_]\\)\\<ODOT\\>\\([^_]\\)" "\\1{}\\\\odot{}\\2")
	("\\([^_]\\)\\<CIRCLEDAST\\>\\([^_]\\)" "\\1{}\\\\circledast{}\\2")
	("\\([^_]\\)\\<BOXTIMES\\>\\([^_]\\)" "\\1{}\\\\boxtimes{}\\2")
	("\\([^_]\\)\\<BOXDOT\\>\\([^_]\\)" "\\1{}\\\\boxdot{}\\2")
	("\\([^_]\\)\\<DIV\\>\\([^_]\\)" "\\1{}\\\\div{}\\2")
	("\\([^_]\\)\\<OSLASH\\>\\([^_]\\)" "\\1{}\\\\oslash{}\\2")
	("\\([^_]\\)\\<DOT\\>\\([^_]\\)" "\\1{}\\\\cdot{}\\2")
	("\\([^_]\\)\\<TIMES\\>\\([^_]\\)" "\\1{}\\\\times{}\\2")
	("\\([^_]\\)\\<DIV\\>\\([^_]\\)" "\\1{}\\\\div{}\\2")
	("\\([^_]\\)\\<DOTPLUS\\>\\([^_]\\)" "\\1{}\\\\dotplus{}\\2")
	("\\([^_]\\)\\<DOTMINUS\\>\\([^_]\\)" "\\1{}\\\\dotminus{}\\2")
	("\\([^_]\\)\\<DOTTIMES\\>\\([^_]\\)" "\\1{}\\\\dottimes{}\\2")
	("\\([^_]\\)\\<OPLUS\\>\\([^_]\\)" "\\1{}\\\\oplus{}\\2")
	("\\([^_]\\)\\<OMINUS\\>\\([^_]\\)" "\\1{}\\\\ominus{}\\2")
	("\\([^_]\\)\\<OTIMES\\>\\([^_]\\)" "\\1{}\\\\otimes{}\\2")
	("\\([^_]\\)\\<OSLASH\\>\\([^_]\\)" "\\1{}\\\\oslash{}\\2")
	("\\([^_]\\)\\<ODOT\\>\\([^_]\\)" "\\1{}\\\\odot{}\\2")
	("\\([^_]\\)\\<CIRCLEDAST\\>\\([^_]\\)" "\\1{}\\\\circledast{}\\2")
	("\\([^_]\\)\\<BOXPLUS\\>\\([^_]\\)" "\\1{}\\\\boxplus{}\\2")
	("\\([^_]\\)\\<BOXMINUS\\>\\([^_]\\)" "\\1{}\\\\boxminus{}\\2")
	("\\([^_]\\)\\<BOXTIMES\\>\\([^_]\\)" "\\1{}\\\\boxtimes{}\\2")
	("\\([^_]\\)\\<BOXDOT\\>\\([^_]\\)" "\\1{}\\\\boxdot{}\\2")
	("\\([^_]\\)\\<BOXSLASH\\>\\([^_]\\)" "\\1{}\\\\boxslash{}\\2")
	("\\([^_]\\)\\<CAPCAP\\>\\([^_]\\)" "\\1{}\\\\Cap{}\\2")
	("\\([^_]\\)\\<UPLUS\\>\\([^_]\\)" "\\1{}\\\\uplus{}\\2")
	("\\([^_]\\)\\<CUPCUP\\>\\([^_]\\)" "\\1{}\\\\Cup{}\\2")
	("\\([^_]\\)\\<SETMINUS\\>\\([^_]\\)" "\\1{}\\\\setminus{}\\2")
	("\\([^_]\\)\\<SQCAP\\>\\([^_]\\)" "\\1{}\\\\sqcap{}\\2")
	("\\([^_]\\)\\<SQCUP\\>\\([^_]\\)" "\\1{}\\\\sqcup{}\\2")
	("\\([^_]\\)\\<CURLYAND\\>\\([^_]\\)" "\\1{}\\\\curlywedge{}\\2")
	("\\([^_]\\)\\<CURLYOR\\>\\([^_]\\)" "\\1{}\\\\curlyvee{}\\2")
	("\\([^_]\\)\\<SIMEQ\\>\\([^_]\\)" "\\1{}\\\\simeq{}\\2")
	("\\([^_]\\)\\<SIM\\>\\([^_]\\)" "\\1{}\\\\sim{}\\2")
	("\\([^_]\\)\\<APPROX\\>\\([^_]\\)" "\\1{}\\\\approx{}\\2")
	("\\([^_]\\)\\<APPROXEQ\\>\\([^_]\\)" "\\1{}\\\\approxeq{}\\2")
	("\\([^_]\\)\\<BUMPEQV\\>\\([^_]\\)" "\\1{}\\\\Bumpeq{}\\2")
	("\\([^_]\\)\\<DOTEQDOT\\>\\([^_]\\)" "\\1{}\\\\doteqdot{}\\2")
	("\\([^_]\\)\\<EQRING\\>\\([^_]\\)" "\\1{}\\\\eqcirc{}\\2")
	("\\([^_]\\)\\<RINGEQ\\>\\([^_]\\)" "\\1{}\\\\circeq{}\\2")
	("\\([^_]\\)\\<EQDEL\\>\\([^_]\\)" "\\1{}\\\\triangleq{}\\2")
	("\\([^_]\\)\\<NSIMEQ\\>\\([^_]\\)" "\\1{}\\\\not\\\\simeq{}\\2")
	("\\([^_]\\)\\<NAPPROX\\>\\([^_]\\)" "\\1{}\\\\not\\\\approx{}\\2")
	("\\([^_]\\)\\<NEQV\\>\\([^_]\\)" "\\1{}\\\\not\\\\equiv{}\\2")
	("\\([^_]\\)\\<NSEQV\\>\\([^_]\\)" "\\1{}\\\\not\\\\sequiv{}\\2")
	("<<<" "{}\\\\lll{}")
	("<<" "{}\\\\ll{}")
	("\\([^_]\\)\\<DOTLT\\>\\([^_]\\)" "\\1{}\\\\lessdot{}\\2")
	(">>>" "{}\\\\ggg{}")
	(">>" "{}\\\\gg{}")
	("\\([^_]\\)\\<DOTGT\\>\\([^_]\\)" "\\1{}\\\\gtrdot{}\\2")
	("\\([^_]\\)\\<NLT\\>\\([^_]\\)" "\\1{}\\\\nless{}\\2")
	("\\([^_]\\)\\<NGT\\>\\([^_]\\)" "\\1{}\\\\ngtr{}\\2")
	("\\([^_]\\)\\<NLE\\>\\([^_]\\)" "\\1{}\\\\nleq{}\\2")
	("\\([^_]\\)\\<NGE\\>\\([^_]\\)" "\\1{}\\\\ngeq{}\\2")
	("\\([^_]\\)\\<SUBSET\\>\\([^_]\\)" "\\1{}\\\\subset{}\\2")
	("\\([^_]\\)\\<SUBSETEQ\\>\\([^_]\\)" "\\1{}\\\\subseteq{}\\2")
	("\\([^_]\\)\\<SUBSETNEQ\\>\\([^_]\\)" "\\1{}\\\\subsetneq{}\\2")
	("\\([^_]\\)\\<SUBSUB\\>\\([^_]\\)" "\\1{}\\\\Subset{}\\2")
	("\\([^_]\\)\\<SUPSET\\>\\([^_]\\)" "\\1{}\\\\supset{}\\2")
	("\\([^_]\\)\\<SUPSETEQ\\>\\([^_]\\)" "\\1{}\\\\supseteq{}\\2")
	("\\([^_]\\)\\<SUPSETNEQ\\>\\([^_]\\)" "\\1{}\\\\supsetneq{}\\2")
	("\\([^_]\\)\\<SUPSUP\\>\\([^_]\\)" "\\1{}\\\\Supset{}\\2")
	("\\([^_]\\)\\<NSUBSET\\>\\([^_]\\)" "\\1{}\\\\not\\\\subset{}\\2")
	("\\([^_]\\)\\<NSUPSET\\>\\([^_]\\)" "\\1{}\\\\not\\\\supset{}\\2")
	("\\([^_]\\)\\<NSUBSETEQ\\>\\([^_]\\)" "\\1{}\\\\nsubseteq{}\\2")
	("\\([^_]\\)\\<NSUPSETEQ\\>\\([^_]\\)" "\\1{}\\\\nsupseteq{}\\2")
	("\\([^_]\\)\\<SQSUBSET\\>\\([^_]\\)" "\\1{}\\\\sqsubset{}\\2")
	("\\([^_]\\)\\<SQSUBSETEQ\\>\\([^_]\\)" "\\1{}\\\\sqsubseteq{}\\2")
	("\\([^_]\\)\\<SQSUPSET\\>\\([^_]\\)" "\\1{}\\\\sqsupset{}\\2")
	("\\([^_]\\)\\<SQSUPSETEQ\\>\\([^_]\\)" "\\1{}\\\\sqsupseteq{}\\2")
	("\\([^_]\\)\\<NSQSUBSET\\>\\([^_]\\)" "\\1{}\\\\not\\\\sqsubset{}\\2")
	("\\([^_]\\)\\<NSQSUBSETEQ\\>\\([^_]\\)" "\\1{}\\\\not\\\\sqsubseteq{}\\2")
	("\\([^_]\\)\\<NSQSUPSET\\>\\([^_]\\)" "\\1{}\\\\not\\\\sqsupset{}\\2")
	("\\([^_]\\)\\<NSQSUPSETEQ\\>\\([^_]\\)" "\\1{}\\\\not\\\\sqsupseteq{}\\2")
	("\\([^_]\\)\\<PREC\\>\\([^_]\\)" "\\1{}\\\\prec{}\\2")
	("\\([^_]\\)\\<PRECEQ\\>\\([^_]\\)" "\\1{}\\\\preccurlyeq{}\\2")
	("\\([^_]\\)\\<PRECSIM\\>\\([^_]\\)" "\\1{}\\\\precsim{}\\2")
	("\\([^_]\\)\\<EQPREC\\>\\([^_]\\)" "\\1{}\\\\curlyeqprec{}\\2")
	("\\([^_]\\)\\<PRECNSIM\\>\\([^_]\\)" "\\1{}\\\\precnsim{}\\2")
	("\\([^_]\\)\\<SUCC\\>\\([^_]\\)" "\\1{}\\\\succ{}\\2")
	("\\([^_]\\)\\<SUCCEQ\\>\\([^_]\\)" "\\1{}\\\\succcurlyeq{}\\2")
	("\\([^_]\\)\\<SUCCSIM\\>\\([^_]\\)" "\\1{}\\\\succsim{}\\2")
	("\\([^_]\\)\\<EQSUCC\\>\\([^_]\\)" "\\1{}\\\\curlyeqsucc{}\\2")
	("\\([^_]\\)\\<SUCCNSIM\\>\\([^_]\\)" "\\1{}\\\\succnsim{}\\2")
	("\\([^_]\\)\\<NPREC\\>\\([^_]\\)" "\\1{}\\\\nprec{}\\2")
	("\\([^_]\\)\\<NSUCC\\>\\([^_]\\)" "\\1{}\\\\nsucc{}\\2")
	("\\([^_]\\)\\<NPRECEQ\\>\\([^_]\\)" "\\1{}\\\\not\\\\preceq{}\\2")
	("\\([^_]\\)\\<NSUCCEQ\\>\\([^_]\\)" "\\1{}\\\\not\\\\succeq{}\\2")
	("\\([^_]\\)\\<SMALLER\\>\\([^_]\\)" "\\1{<\\\\!\\\\\llap{$-$}}\\2")
	("\\([^_]\\)\\<SMALLEREQ\\>\\([^_]\\)" "\\1{\\\\leq\\\\!\\\\llap{\\\\raisebox{.15ex}[0cm][0cm]{$-$}}}\\2")
	("\\([^_]\\)\\<LARGER\\>\\([^_]\\)" "\\1{\\\\rlap{$-$}\\\\!>}\\2")
	("\\([^_]\\)\\<LARGEREQ\\>\\([^_]\\)" "\\1{\\\\rlap{\\\\raisebox{.15ex}[0cm][0cm]{$-$}}\\\\!\\\\geq}\\2")
	("\\([^_]\\)\\<IN\\>\\([^_]\\)" "\\1{}\\\\in{}\\2")
	("\\([^_]\\)\\<NOTIN\\>\\([^_]\\)" "\\1{}\\\\not\\\\in{}\\2")
	("\\([^_]\\)\\<CONTAINS\\>\\([^_]\\)" "\\1{}\\\\ni{}\\2")
	("\\([^_]\\)\\<NCONTAINS\\>\\([^_]\\)" "\\1{}\\\\not\\\\ni{}\\2")
	("\\([^_]\\)\\<DEGREES\\>\\([^_]\\)" "\\1{{}^\\\\circ}\\2")
	("\\([^_]\\)\\<UPARROW\\>\\([^_]\\)" "\\1{}\\\\uparrow{}\\2")
	("\\([^_]\\)\\<DOWNARROW\\>\\([^_]\\)" "\\1{}\\\\downarrow{}\\2")
	("\\([^_]\\)\\<UPDOWNARROW\\>\\([^_]\\)" "\\1{}\\\\updownarrow{}\\2")
	("\\([^_]\\)\\<NWARROW\\>\\([^_]\\)" "\\1{}\\\\nwarrow{}\\2")
	("\\([^_]\\)\\<NEARROW\\>\\([^_]\\)" "\\1{}\\\\nearrow{}\\2")
	("\\([^_]\\)\\<SEARROW\\>\\([^_]\\)" "\\1{}\\\\searrow{}\\2")
	("\\([^_]\\)\\<SWARROW\\>\\([^_]\\)" "\\1{}\\\\swarrow{}\\2")
	("<-/-" "{}\\\\nleftarrow{}")
	("-/->" "{}\\\\nrightarrow{}")
	("\\([^_]\\)\\<LEFTHARPOONUP\\>\\([^_]\\)" "\\1{}\\\\leftharpoonup{}\\2")
	("\\([^_]\\)\\<LEFTHARPOONDOWN\\>\\([^_]\\)" "\\1{}\\\\leftharpoondown{}\\2")
	("\\([^_]\\)\\<UPHARPOONRIGHT\\>\\([^_]\\)" "\\1{}\\\\upharpoonright{}\\2")
	("\\([^_]\\)\\<UPHARPOONLEFT\\>\\([^_]\\)" "\\1{}\\\\upharpoonleft{}\\2")
	("\\([^_]\\)\\<RIGHTHARPOONUP\\>\\([^_]\\)" "\\1{}\\\\rightharpoonup{}\\2")
	("\\([^_]\\)\\<RIGHTHARPOONDOWN\\>\\([^_]\\)" "\\1{}\\\\rightharpoondown{}\\2")
	("\\([^_]\\)\\<DOWNHARPOONRIGHT\\>\\([^_]\\)" "\\1{}\\\\downharpoonright{}\\2")
	("\\([^_]\\)\\<DOWNHARPOONLEFT\\>\\([^_]\\)" "\\1{}\\\\downharpoonleft{}\\2")
	("\\([^_]\\)\\<RIGHTLEFTARROWS\\>\\([^_]\\)" "\\1{}\\\\rightleftarrows{}\\2")
	("\\([^_]\\)\\<LEFTRIGHTARROWS\\>\\([^_]\\)" "\\1{}\\\\leftrightarrows{}\\2")
	("\\([^_]\\)\\<LEFTLEFTARROWS\\>\\([^_]\\)" "\\1{}\\\\leftleftarrows{}\\2")
	("\\([^_]\\)\\<UPUPARROWS\\>\\([^_]\\)" "\\1{}\\\\upuparrows{}\\2")
	("\\([^_]\\)\\<RIGHTRIGHTARROWS\\>\\([^_]\\)" "\\1{}\\\\rightrightarrows{}\\2")
	("\\([^_]\\)\\<DOWNDOWNARROWS\\>\\([^_]\\)" "\\1{}\\\\downdownarrows{}\\2")
	("\\([^_]\\)\\<RIGHTLEFTHARPOONS\\>\\([^_]\\)" "\\1{}\\\\rightleftharpoons{}\\2")
	("\\([^_]\\)\\<DEL\\>\\([^_]\\)" "\\1{}\\\\partial{}\\2")
	("\\([^_]\\)\\<PRODUCT\\>\\([^_]\\)" "\\1{}\\\\prod{}\\2")
	("\\([^_]\\)\\<COPRODUCT\\>\\([^_]\\)" "\\1{}\\\\coprod{}\\2")
	("\\([^_]\\)\\<SUM\\>\\([^_]\\)" "\\1{}\\\\sum{}\\2")
	("\\([^_]\\)\\<BULLET\\>\\([^_]\\)" "\\1{}\\\\bullet{}\\2")
	("\\([^_]\\)\\<PROPTO\\>\\([^_]\\)" "\\1{}\\\\propto{}\\2")
	("\\([^_]\\)\\<DIVIDES\\>\\([^_]\\)" "\\1{}\\\\mid{}\\2")
	("\\([^_]\\)\\<PARALLEL\\>\\([^_]\\)" "\\1{}\\\\parallel{}\\2")
	("\\([^_]\\)\\<NPARALLEL\\>\\([^_]\\)" "\\1{}\\\\nparallel{}\\2")
	("\\([^_]\\)\\<WREATH\\>\\([^_]\\)" "\\1{}\\\\wr{}\\2")
	("\\([^_]\\)\\<BUMPEQ\\>\\([^_]\\)" "\\1{}\\\\bumpeq{}\\2")
	("\\([^_]\\)\\<DOTEQ\\>\\([^_]\\)" "\\1{}\\\\doteq{}\\2")
	("\\([^_]\\)\\<CIRCLEDRING\\>\\([^_]\\)" "\\1{}\\\\circledcirc{}\\2")
	("\\([^_]\\)\\<DASHV\\>\\([^_]\\)" "\\1{}\\\\dashv{}\\2")
	("\\([^_]\\)\\<DIAMOND\\>\\([^_]\\)" "\\1{}\\\\diamond{}\\2")
	("\\([^_]\\)\\<STAR\\>\\([^_]\\)" "\\1{}\\\\star{}\\2")
	("\\([^_]\\)\\<BIGODOT\\>\\([^_]\\)" "\\1{}\\\\bigodot{}\\2")
	("\\([^_]\\)\\<BIGOPLUS\\>\\([^_]\\)" "\\1{}\\\\bigoplus{}\\2")
	("\\([^_]\\)\\<BIGUPLUS\\>\\([^_]\\)" "\\1{}\\\\biguplus{}\\2")
	("\\([^_]\\)\\<BIGOTIMES\\>\\([^_]\\)" "\\1{}\\\\bigotimes{}\\2")
	("\\([^_]\\)\\<JOIN\\>\\([^_]\\)" "\\1{}\\\\Join{}\\2")
	("\\([^_]\\)\\<UPPLUS\\>\\([^_]\\)" "\\1{}\\\\upplus{}\\2")
	("\\([^_]\\)\\<UPMINUS\\>\\([^_]\\)" "\\1{}\\\\upminus{}\\2")
	("\\([^_]\\)\\<UPTIMES\\>\\([^_]\\)" "\\1{}\\\\uptimes{}\\2")
	("\\([^_]\\)\\<UPDOT\\>\\([^_]\\)" "\\1{}\\\\updot{}\\2")
	("\\([^_]\\)\\<UPSLASH\\>\\([^_]\\)" "\\1{}\\\\upslash{}\\2")
	("\\([^_]\\)\\<DOWNPLUS\\>\\([^_]\\)" "\\1{}\\\\downplus{}\\2")
	("\\([^_]\\)\\<DOWNMINUS\\>\\([^_]\\)" "\\1{}\\\\downminus{}\\2")
	("\\([^_]\\)\\<DOWNTIMES\\>\\([^_]\\)" "\\1{}\\\\downtimes{}\\2")
	("\\([^_]\\)\\<DOWNDOT\\>\\([^_]\\)" "\\1{}\\\\downdot{}\\2")
	("\\([^_]\\)\\<DOWNSLASH\\>\\([^_]\\)" "\\1{}\\\\downslash{}\\2")
	("\\([^_]\\)\\<CHOPPLUS\\>\\([^_]\\)" "\\1{}\\\\chopplus{}\\2")
	("\\([^_]\\)\\<CHOPMINUS\\>\\([^_]\\)" "\\1{}\\\\chopminus{}\\2")
	("\\([^_]\\)\\<CHOPTIMES\\>\\([^_]\\)" "\\1{}\\\\choptimes{}\\2")
	("\\([^_]\\)\\<CHOPDOT\\>\\([^_]\\)" "\\1{}\\\\chopdot{}\\2")
	("\\([^_]\\)\\<CHOPSLASH\\>\\([^_]\\)" "\\1{}\\\\chopslash{}\\2")
	("\\([^_]\\)\\<EXACTPLUS\\>\\([^_]\\)" "\\1{}\\\\exactplus{}\\2")
	("\\([^_]\\)\\<EXACTMINUS\\>\\([^_]\\)" "\\1{}\\\\exactminus{}\\2")
	("\\([^_]\\)\\<EXACTTIMES\\>\\([^_]\\)" "\\1{}\\\\exacttimes{}\\2")
	("\\([^_]\\)\\<EXACTDOT\\>\\([^_]\\)" "\\1{}\\\\exactdot{}\\2")
	("\\([^_]\\)\\<EXACTSLASH\\>\\([^_]\\)" "\\1{}\\\\exactslash{}\\2")
	("\\([^_]\\)\\<TOTALLT\\>\\([^_]\\)" "\\1{}\\\\totallss{}\\2")
	("\\([^_]\\)\\<TOTALLE\\>\\([^_]\\)" "\\1{}\\\\totalleq{}\\2")
	("\\([^_]\\)\\<TOTALGE\\>\\([^_]\\)" "\\1{}\\\\totalgeq{}\\2")
	("\\([^_]\\)\\<TOTALGT\\>\\([^_]\\)" "\\1{}\\\\totalgtr{}\\2")
	("\\([^_]\\)\\<BITAND\\>\\([^_]\\)" "\\1{}\\\\twointersectand{}\\2")
	("\\([^_]\\)\\<BITOR\\>\\([^_]\\)" "\\1{}\\\\twointersector{}\\2")
	("\\([^_]\\)\\<BITXOR\\>\\([^_]\\)" "\\1{}\\\\twointersectxor{}\\2")
	("\\([^_]\\)\\<BITNOT\\>\\([^_]\\)" "\\1{}\\\\twointersectnot{}\\2")
	;; These are the old forms for floor, ceiling, etc.  New forms |\ /| etc. are below, with the brackets.
	("\\([^_]\\)\\<LF\\>\\([^_]\\)" "\\1{}\\\\lfloor{}\\2")
	("\\([^_]\\)\\<RF\\>\\([^_]\\)" "\\1{}\\\\rfloor{}\\2")
	("\\([^_]\\)\\<LC\\>\\([^_]\\)" "\\1{}\\\\lceil{}\\2")
	("\\([^_]\\)\\<RC\\>\\([^_]\\)" "\\1{}\\\\rceil{}\\2")
	("\\([^_]\\)\\<LHF\\>\\([^_]\\)" "\\1{}\\\\lhfloor{}\\2")
	("\\([^_]\\)\\<RHF\\>\\([^_]\\)" "\\1{}\\\\rhfloor{}\\2")
	("\\([^_]\\)\\<LHC\\>\\([^_]\\)" "\\1{}\\\\lhceil{}\\2")
	("\\([^_]\\)\\<RHC\\>\\([^_]\\)" "\\1{}\\\\rhceil{}\\2")
	("\\([^_]\\)\\<LHHF\\>\\([^_]\\)" "\\1{}\\\\lhhfloor{}\\2")
	("\\([^_]\\)\\<RHHF\\>\\([^_]\\)" "\\1{}\\\\rhhfloor{}\\2")
	("\\([^_]\\)\\<LHHC\\>\\([^_]\\)" "\\1{}\\\\lhhceil{}\\2")
	("\\([^_]\\)\\<RHHC\\>\\([^_]\\)" "\\1{}\\\\rhhceil{}\\2")
	("\\([^_]\\)\\<PP_SUM\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\sum}}\\2")
	("\\([^_]\\)\\<PP_PRODUCT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\prod}}\\2")
	("\\([^_]\\)\\<PP_AND\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\bigwedge}}\\2")
	("\\([^_]\\)\\<PP_OR\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\bigvee}}\\2")
	("\\([^_]\\)\\<PP_CAP\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\bigcap}}\\2")
	("\\([^_]\\)\\<PP_INTERSECT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\bigcap}}\\2")
	("\\([^_]\\)\\<PP_CUP\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\bigcup}}\\2")
	("\\([^_]\\)\\<PP_UNION\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\bigcup}}\\2")
	("\\([^_]\\)\\<PP_ODOT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\bigodot}}\\2")
	("\\([^_]\\)\\<PP_OPLUS\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\bigoplus}}\\2")
	("\\([^_]\\)\\<PP_UPLUS\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\biguplus}}\\2")
	("\\([^_]\\)\\<PP_OTIMES\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\bigotimes}}\\2")
	("\\([^_]\\)\\<PP_MAX\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\mathtt{MAX}}}\\2")
	("\\([^_]\\)\\<PP_MIN\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overrightarrow{\\\\mathtt{MIN}}}\\2")
	("\\([^_]\\)\\<PS_SUM\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\sum}}\\2")
	("\\([^_]\\)\\<PS_PRODUCT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\prod}}\\2")
	("\\([^_]\\)\\<PS_AND\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\bigwedge}}\\2")
	("\\([^_]\\)\\<PS_OR\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\bigvee}}\\2")
	("\\([^_]\\)\\<PS_CAP\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\bigcap}}\\2")
	("\\([^_]\\)\\<PS_INTERSECT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\bigcap}}\\2")
	("\\([^_]\\)\\<PS_CUP\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\bigcup}}\\2")
	("\\([^_]\\)\\<PS_UNION\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\bigcup}}\\2")
	("\\([^_]\\)\\<PS_ODOT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\bigodot}}\\2")
	("\\([^_]\\)\\<PS_OPLUS\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\bigoplus}}\\2")
	("\\([^_]\\)\\<PS_UPLUS\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\biguplus}}\\2")
	("\\([^_]\\)\\<PS_OTIMES\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\bigotimes}}\\2")
	("\\([^_]\\)\\<PS_MAX\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\mathtt{MAX}}}\\2")
	("\\([^_]\\)\\<PS_MIN\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrow{\\\\mathtt{MIN}}}\\2")
	("\\([^_]\\)\\<XPP_SUM\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\sum}}\\2")
	("\\([^_]\\)\\<XPP_PRODUCT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\prod}}\\2")
	("\\([^_]\\)\\<XPP_AND\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\bigwedge}}\\2")
	("\\([^_]\\)\\<XPP_OR\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\bigvee}}\\2")
	("\\([^_]\\)\\<XPP_CAP\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\bigcap}}\\2")
	("\\([^_]\\)\\<XPP_INTERSECT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\bigcap}}\\2")
	("\\([^_]\\)\\<XPP_CUP\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\bigcup}}\\2")
	("\\([^_]\\)\\<XPP_UNION\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\bigcup}}\\2")
	("\\([^_]\\)\\<XPP_ODOT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\bigodot}}\\2")
	("\\([^_]\\)\\<XPP_OPLUS\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\bigoplus}}\\2")
	("\\([^_]\\)\\<XPP_UPLUS\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\biguplus}}\\2")
	("\\([^_]\\)\\<XPP_OTIMES\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\bigotimes}}\\2")
	("\\([^_]\\)\\<XPP_MAX\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\mathtt{MAX}}}\\2")
	("\\([^_]\\)\\<XPP_MIN\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overcirclerightarrow{\\\\mathtt{MIN}}}\\2")
	("\\([^_]\\)\\<XPS_SUM\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\sum}}\\2")
	("\\([^_]\\)\\<XPS_PRODUCT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\prod}}\\2")
	("\\([^_]\\)\\<XPS_AND\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\bigwedge}}\\2")
	("\\([^_]\\)\\<XPS_OR\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\bigvee}}\\2")
	("\\([^_]\\)\\<XPS_CAP\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\bigcap}}\\2")
	("\\([^_]\\)\\<XPS_INTERSECT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\bigcap}}\\2")
	("\\([^_]\\)\\<XPS_CUP\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\bigcup}}\\2")
	("\\([^_]\\)\\<XPS_UNION\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\bigcup}}\\2")
	("\\([^_]\\)\\<XPS_ODOT\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\bigodot}}\\2")
	("\\([^_]\\)\\<XPS_OPLUS\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\bigoplus}}\\2")
	("\\([^_]\\)\\<XPS_UPLUS\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\biguplus}}\\2")
	("\\([^_]\\)\\<XPS_OTIMES\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\bigotimes}}\\2")
	("\\([^_]\\)\\<XPS_MAX\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\mathtt{MAX}}}\\2")
	("\\([^_]\\)\\<XPS_MIN\\>\\([^_]\\)" "\\1{}\\\\mathop{\\\\overleftarrowcircle{\\\\mathtt{MIN}}}\\2")
	("\\.\\.\\." "{}\\\\ldots{}")
	;; Various brackets, observing interior space (important that llbracket come after langle)
	("\\[ " "[{}\\\\,")
	(" \\]" "{}\\\\,]")
	("<| " "<|\\\\,")
	("<|\\([^)]\\)" "{}\\\\langle{}\\1")
	(" |>" "{}\\\\,|>")
	("|>" "{}\\\\rangle{}")
	("<<| " "<<|\\\\,")
	("<<|" "{}\\\\langle\\\\!\\\\langle{}")
	(" |>>" "{}\\\\,|>>")
	("|>>" "{}\\\\rangle\\\!\\\\rangle{}")
	("</ " "</\\\\,")
	("</" "{}\\\\ulcorner{}")
	(" />" "{}\\\\,/>")
	("/>" "{}\\\\urcorner{}")
	("\\[\\\\ " "[\\\\\\\\,")
	("\\[\\\\" "{}\\\\llbracket{}")
	(" \\\\\\]" "{}\\\\,\\\\]")
	("\\\\\\]" "{}\\\\rrbracket{}")
	("\\(^\\|[^[{<|/\\.*]\\)|||\\\\" "\\1{}\\\\lhhfloor{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)|||\\\\" "\\1{}\\\\lhhfloor{}\\2")
	("\\(^\\|[^/\\.*]\\)/|||" "\\1{}\\\\rhhfloor{}\\2")
	("\\(^\\|[^/\\.*]\\)/|||" "\\1{}\\\\rhhfloor{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)|||/" "\\1{}\\\\lhhceil{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)|||/" "\\1{}\\\\lhhceil{}\\2")
	("\\(^\\|[^/\\.*]\\)\\\\|||" "\\1{}\\\\rhhceil{}\\2")
	("\\(^\\|[^/\\.*]\\)\\\\|||" "\\1{}\\\\rhhceil{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)||\\\\" "\\1{}\\\\lhfloor{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)||\\\\" "\\1{}\\\\lhfloor{}\\2")
	("\\(^\\|[^/\\.*]\\)/||" "\\1{}\\\\rhfloor{}\\2")
	("\\(^\\|[^/\\.*]\\)/||" "\\1{}\\\\rhfloor{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)||/" "\\1{}\\\\lhceil{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)||/" "\\1{}\\\\lhceil{}\\2")
	("\\(^\\|[^/\\.*]\\)\\\\||" "\\1{}\\\\rhceil{}\\2")
	("\\(^\\|[^/\\.*]\\)\\\\||" "\\1{}\\\\rhceil{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)|\\\\" "\\1{}\\\\lfloor{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)|\\\\" "\\1{}\\\\lfloor{}\\2")
	("\\(^\\|[^/\\.*]\\)/|" "\\1{}\\\\rfloor{}\\2")
	("\\(^\\|[^/\\.*]\\)/|" "\\1{}\\\\rfloor{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)|/" "\\1{}\\\\lceil{}\\2")
	("\\(^\\|[^[{<|/\\.*]\\)|/" "\\1{}\\\\lceil{}\\2")
	("\\(^\\|[^/\\.*]\\)\\\\|" "\\1{}\\\\rceil{}\\2")
	("\\(^\\|[^/\\.*]\\)\\\\|" "\\1{}\\\\rceil{}\\2")
	;; Literals
	("\\<\\([0-9.]+\\)_\\([0-9][0-9]+\\)\\>" "{}\\\\mathtt{\\1}_{\\2}")
	("\\<\\([0-9.]+\\)_\\([0-9]\\)\\>" "{\\1}_{\\\\,\\2}")
	("\\<\\([0-9A-Fa-f.]+\\)_\\([0-9]+\\)\\>" "{}\\\\mathtt{\\1}_{\\2}")
	("\\<\\([0-9.]+\\)_TWO\\>" "{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc two}}")
	("\\<\\([0-9A-Fa-f.]+\\)_TWO\\>" "{}\\\\mathrm{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc two}}")
	("\\<\\([0-9.]+\\)_THREE\\>" "{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc three}}")
	("\\<\\([0-9A-Fa-f.]+\\)_THREE\\>" "{}\\\\mathrm{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc three}}")
	("\\<\\([0-9.]+\\)_FOUR\\>" "{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc four}}")
	("\\<\\([0-9A-Fa-f.]+\\)_FOUR\\>" "{}\\\\mathrm{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc four}}")
	("\\<\\([0-9.]+\\)_FIVE\\>" "{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc five}}")
	("\\<\\([0-9A-Fa-f.]+\\)_FIVE\\>" "{}\\\\mathrm{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc five}}")
	("\\<\\([0-9.]+\\)_SIX\\>" "{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc six}}")
	("\\<\\([0-9A-Fa-f.]+\\)_SIX\\>" "{}\\\\mathrm{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc six}}")
	("\\<\\([0-9.]+\\)_SEVEN\\>" "{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc seven}}")
	("\\<\\([0-9A-Fa-f.]+\\)_SEVEN\\>" "{}\\\\mathrm{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc seven}}")
	("\\<\\([0-9.]+\\)_EIGHT\\>" "{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc eight}}")
	("\\<\\([0-9A-Fa-f.]+\\)_EIGHT\\>" "{}\\\\mathrm{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc eight}}")
	("\\<\\([0-9.]+\\)_NINE\\>" "{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc nine}}")
	("\\<\\([0-9A-Fa-f.]+\\)_NINE\\>" "{}\\\\mathrm{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc nine}}")
	("\\<\\([0-9.]+\\)_TEN\\>" "{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc ten}}")
	("\\<\\([0-9A-Fa-f.]+\\)_TEN\\>" "{}\\\\mathrm{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc ten}}")
	("\\<\\([0-9.]+\\)_ELEVEN\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc eleven}}")
	("\\<\\([0-9A-Fa-f.]+\\)_ELEVEN\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc eleven}}")
	("\\<\\([0-9.]+\\)_TWELVE\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc twelve}}")
	("\\<\\([0-9A-Fa-fXx.]+\\)_TWELVE\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc twelve}}")
	("\\<\\([0-9.]+\\)_THIRTEEN\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc thirteen}}")
	("\\<\\([0-9A-Fa-f.]+\\)_THIRTEEN\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc thirteen}}")
	("\\<\\([0-9.]+\\)_FOURTEEN\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc fourteen}}")
	("\\<\\([0-9A-Fa-f.]+\\)_FOURTEEN\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc fourteen}}")
	("\\<\\([0-9.]+\\)_FIFTEEN\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc fifteen}}")
	("\\<\\([0-9A-Fa-f.]+\\)_FIFTEEN\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc fifteen}}")
	("\\<\\([0-9.]+\\)_SIXTEEN\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc sixteen}}")
	("\\<\\([0-9A-Fa-f.]+\\)_SIXTEEN\\>" "{}\\\\mathtt{\\1}_{\\\\,\\\\\hbox{\\\\small\\\\sc sixteen}}")
	;; Keywords
	("\\(^\\|[^{_\\\\]\\)\\<\\(trait\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(value\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(object\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(extends\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(comprises\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(excludes\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(where\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(do\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(at\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(also\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(end\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(for\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(if\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(then\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(else\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(elif\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(while\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(case\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(of\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(as\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(asif\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(fn\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(label\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(exit\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(with\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(atomic\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(tryatomic\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(pure\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(io\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(throw\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(throws\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(try\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(catch\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(forbid\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(finally\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(dispatch\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(in\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(typecase\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(requires\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(ensures\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(provided\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(invariant\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(property\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(idiom\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(hidden\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(settable\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(wrapped\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(transient\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(getter\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(setter\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(opr\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(var\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(nat\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(int\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(bool\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(juxtaposition\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(coerce\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(coerces\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(widens\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(or\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(dim\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(unit\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(default\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(absorbs\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(SI_unit\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{SI{\\\\char'137}unit}\\3")  ;*** special handling
	("\\(^\\|[^{_\\\\]\\)\\<\\(spawn\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(test\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(static\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(component\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(api\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(import\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(export\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(except\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(public\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(private\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(abstract\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(override\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(syntax\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(largest\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(smallest\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(type\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(self\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\mathord{\\\\KWD{\\2}}\\3")
	;; Not sure about this next group
	("\\(^\\|[^{_\\\\]\\)\\<\\(from\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\KWD{\\2}\\3")
	;; Operators for units
	("\\(^\\|[^{_\\\\]\\)\\<\\(per\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\mathbin{\\\\TYP{\\2}}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(square\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\mathbin{\\\\TYP{\\2}}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(cubic\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\mathbin{\\\\TYP{\\2}}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(squared\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\TYP{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(cubed\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\TYP{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(inverse\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\mathbin{\\\\TYP{\\2}}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(reciprocal\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\mathbin{\\\\TYP{\\2}}\\3");
	("\\(^\\|[^{_\\\\]\\)\\<\\(dimensionless\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\mathbin{\\\\TYP{\\2}}\\3")
	;; Standard units
	("\\(^\\|[^{_\\\\]\\)\\<\\(\\|yotta\\|zetta\\|exa\\|peta\\|tera\\|giga\\|mega\\|kilo\\|hecto\\|deka\\|deci\\|centi\\|milli\\|micro\\|nano\\|pico\\|femto\\|atto\\|zepto\\|yocto\\)\\(\\|meter\\|meters\\|gram\\|grams\\|second\\|seconds\\|ampere\\|amperes\\|kelvin\\|kelvins\\|mole\\|moles\\|candela\\|candelas\\|radian\\|radians\\|steradian\\|steradians\\|hertz\\|newton\\|newtons\\|pascal\\|pascals\\|joule\\|joules\\|watt\\|watts\\|coulomb\\|coulombs\\|volt\\|volts\\|farad\\|farads\\|ohm\\|ohms\\|siemens\\|weber\\|webers\\|tesla\\|teslas\\|henry\\|henries\\|lumen\\|lumens\\|lux\\|becquerel\\|becquerels\\|gray\\|grays\\|katal\\|katals\\|metricTon\\|metricTons\\|tonne\\|tonnes\\|liter\\|liters\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\TYP{\\2\\3}\\4")
	("\\(^\\|[^{_\\\\]\\)\\<\\(kilohm\\|kilohms\\|megohm\\|megohms\\|minute\\|minutes\\|hour\\|hours\\|day\\|days\\|degreeOfAngle\\|degrees\\|minuteOfAngle\\|minutesOfAngle\\|secondOfAngle\\|secondsOfAngle\\|bit\\|bits\\|byte\\|bytes\\|inch\\|inches\\|foot\\|feet\\|yard\\|yards\\|mile\\|miles\\|rod\\|rods\\|furlong\\|furlongs\\|surveyFoot\\|surveyFeet\\|surveyMile\\|surveyMiles\\|nauticalMile\\|nauticalMiles\\|knot\\|knots\\|week\\|weeks\\|fortnight\\|fortnights\\|microfortnight\\|microfortnights\\|gallon\\|gallons\\|fluidQuart\\|fluidQuarts\\|fluidPint\\|fluidPints\\|fluidCup\\|fluidCups\\|fluidOunce\\|fluidOunces\\|fluidDram\\|fluidDrams\\|minim\\|minims\\|traditionalTablespoon\\|traditionalTablespoons\\|traditionalTeaspoon\\|traditionalTeaspoons\\|federalTablespoon\\|federalTablespoons\\|federalTeaspoon\\|federalTeaspoons\\|dryPint\\|dryPints\\|dryQuart\\|dryQuarts\\|peck\\|pecks\\|bushel\\|bushels\\|acre\\|imperialGallon\\|imperialQuart\\|imperialPint\\|imperialGill\\|imperialFluidOunce\\|imperialFluidDrachm\\|imperialFluidDRam\\|imperialFluidScruple\\|imperialMinim\\|pound\\|pounds\\|ounce\\|ounces\\|grain\\|grains\\|troyPound\\|troyPounds\\|troyOunce\\|troyOunces\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\TYP{\\2}\\3")
	;; Variables (any identifier followed by a } has already been processed)
	;;   Subscripted relational operators
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9_]*\\)\\>_LT\\($\\|\\>[^}_]\\)" "\\1\\2{}_{<}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9_]*\\)\\>_LE\\($\\|\\>[^}_]\\)" "\\1\\2{}_{\\\\leq}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9_]*\\)\\>_EQ\\($\\|\\>[^}_]\\)" "\\1\\2{}_{=}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9_]*\\)\\>_GE\\($\\|\\>[^}_]\\)" "\\1\\2{}_{\\\\geq}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9_]*\\)\\>_GT\\($\\|\\>[^}_]\\)" "\\1\\2{}_{>}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9_]*\\)\\>_NE\\($\\|\\>[^}_]\\)" "\\1\\2{}_{\\\\neq}\\3")
        ;;   Stars, splats, etc.
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9_]*\\)\\>_star\\($\\|\\>[^}_]\\)" "\\1\\2^*\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9_]*\\)\\>_plus\\($\\|\\>[^}_]\\)" "\\1\\2^+\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9_]*\\)\\>_minus\\($\\|\\>[^}_]\\)" "\\1\\2^-\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9_]*\\)\\>_splat\\($\\|\\>[^}_]\\)" "\\1\\2^{\\\\#}\\3")
	;;   Blackboard font
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Z]\\)\\2\\>\\($\\|[^}_]\\)" "\\1{}\\\\mathbb{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Z]\\)\\2\\([0-9]+\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\mathbb{\\2}\\3\\4")
	;;   Operators
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Z][A-Z]+\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\OPR{\\2}\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Z]+\\)_\\([A-Z]+\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\OPR{\\2{\\\\char'137}\\3}\\4")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Z]+\\)_\\([A-Z]+\\)_\\([A-Z]+\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\OPR{\\2{\\\\char'137}\\3{\\\\char'137}\\4}\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Z]+\\)_\\([A-Z]+\\)_\\([A-Z]+\\)_\\([A-Z]+\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\OPR{\\2{\\\\char'137}\\3{\\\\char'137}\\4{\\\\char'137}\\5}\\6")
	;;   Types
	("\\(^\\|[^{_\\\\]\\)\\(\\<[A-Z][A-Z0-9]*[a-z][A-Za-z0-9]*\\)\\([']*\\>\\)\\($\\|[^}_]\\)" "\\1{}\\\\TYP{\\2}\\3\\4")
	;;   Variables with _bar
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z]\\)_bar\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\bar{\\\\mathbf{\\2}}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z][A-Za-z]+\\)_bar\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overline{\\\\mathbf{\\2}}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z]\\)\\([0-9]+\\)_bar\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\bar{\\\\mathbf{\\2}}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z][A-Za-z]+\\)\\([0-9]+\\)_bar\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overline{\\\\mathbf{\\2}}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z]\\)_bar_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\bar{\\\\mathbf{\\2}}_{\\\\mathrm{\\3}}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z][A-Za-z]+\\)_bar_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overline{\\\\mathbf{\\2}}_{\\\\mathrm{\\3}}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z]\\)_bar\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\bar{\\\\VAR{\\2}}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)\\(\\\\GREEK{[A-Za-z0-9\\\\_]+}\\)_bar\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\bar{\\2}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z]+\\)_bar\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overline{\\\\VAR{\\2}}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z]\\)\\([0-9]+\\)_bar\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\bar{\\\\VAR{\\2}}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\(\\\\GREEK{[A-Za-z0-9\\\\_]+}\\)\\([0-9]+\\)_bar\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\bar{\\2}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z]+\\)\\([0-9]+\\)_bar\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overline{\\\\VAR{\\2}}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z]\\)_bar_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\bar{\\\\VAR{\\2}}_{\\\\mathrm{\\3}}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\(\\\\GREEK{[A-Za-z0-9\\\\_]+}\\)_bar_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\bar{\\2}_{\\\\mathrm{\\3}}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z]+\\)_bar_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overline{\\\\VAR{\\2}}_{\\\\mathrm{\\3}}\\4\\5")
	;;   Variables with _vec
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z]\\)_vec\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\vec{\\\\mathbf{\\2}}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z][A-Za-z]+\\)_vec\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overrightarrow{\\\\mathbf{\\2}}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z]\\)\\([0-9]+\\)_vec\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\vec{\\\\mathbf{\\2}}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z][A-Za-z]+\\)\\([0-9]+\\)_vec\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overrightarrow{\\\\mathbf{\\2}}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z]\\)_vec_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\vec{\\\\mathbf{\\2}}_{\\\\mathrm{\\3}}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)_\\([A-Za-z][A-Za-z]+\\)_vec_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overrightarrow{\\\\mathbf{\\2}}_{\\\\mathrm{\\3}}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z]\\)_vec\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\vec{\\\\VAR{\\2}}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)\\(\\\\GREEK{[A-Za-z0-9\\\\_]+}\\)_vec\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\vec{\\2}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z]+\\)_vec\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overrightarrow{\\\\VAR{\\2}}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z]\\)\\([0-9]+\\)_vec\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\vec{\\\\VAR{\\2}}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\(\\\\GREEK{[A-Za-z0-9\\\\_]+}\\)\\([0-9]+\\)_vec\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\vec{\\2}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z]+\\)\\([0-9]+\\)_vec\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overrightarrow{\\\\VAR{\\2}}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z]\\)_vec_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\vec{\\\\VAR{\\2}}_{\\\\mathrm{\\3}}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\(\\\\GREEK{[A-Za-z0-9\\\\_]+}\\)_vec_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\vec{\\2}_{\\\\mathrm{\\3}}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z]+\\)_vec_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\overrightarrow{\\\\VAR{\\2}}_{\\\\mathrm{\\3}}\\4\\5")
	;;   Other cases
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z]+\\)\\([0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{\\2}_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\(\\\\GREEK{[A-Za-z0-9\\\\_]+}\\)\\([0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1\\2_{\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z]\\)_\\([A-Za-z0-9]+\\)\\([']*\\)\\($\\|[^}_]\\)" "\\1{\\2}_{\\\\mathrm{\\3}}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\(\\\\GREEK{[A-Za-z0-9\\\\_]+}\\)_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1\\2_{\\\\mathrm{\\3}}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9]+\\)_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)"
	 "\\1{}\\\\VAR{\\2{\\\\tt\\\\_}\\3}\\4\\5")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9]+\\)_\\([A-Za-z0-9]+\\)_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)"
	 "\\1{}\\\\VAR{\\2{\\\\tt\\\\_}\\3{\\\\tt\\\\_}\\4}\\5\\6")
	("\\(^\\|[^{_\\\\]\\)\\<\\([A-Za-z][A-Za-z0-9]+\\)_\\([A-Za-z0-9]+\\)_\\([A-Za-z0-9]+\\)_\\([A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)"
	 "\\1{}\\\\VAR{\\2{\\\\tt\\\\_}\\3{\\\\tt\\\\_}\\4{\\\\tt\\\\_}\\5}\\6\\7")
	("\\(^\\|[^{_\\\\]\\)\\<\\(\\<[A-Za-z]+\\>\\)_\\($\\|[^}_]\\)" "\\1{}\\\\mathrm{\\2}\\3")
	;;   Trailing underscore quietly disappears after a Greek letter
	("\\(^\\|[^{_\\\\]\\)\\(\\\\GREEK{[A-Za-z0-9\\\\_]+}\\)_\\($\\|[^{}_A-Za-z0-9]\\)" "\\1\\2\\3")
	("\\(^\\|[^{_\\\\]\\)\\<\\(\\<[A-Za-z]\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{\\2}\\3\\4")
	("\\(^\\|[^{_\\\\]\\)\\<\\(\\<[A-Za-z][A-Za-z0-9]+\\)\\([']*\\)\\>\\($\\|[^}_]\\)" "\\1{}\\\\VAR{\\2}\\3\\4")
	;;   Plain boldface variables (must come after "other cases")
	("\\(^\\|[^{A-Za-z0-9_\\\\]\\)_\\([A-Za-z]+\\)\\([']*\\)\\($\\|[^}_]\\)" "\\1{}\\\\mathbf{\\2}\\3\\4")
	("\\(^\\|[^{A-Za-z0-9_\\\\]\\)_\\([A-Za-z]+\\)\\([0-9]+\\)\\([']*\\)\\($\\|[^}_]\\)" "\\1{}\\\\mathbf{\\2}_{\\3}\\4\\5")
	("\\(^\\|[^{A-Za-z0-9_\\\\]\\)_\\([A-Za-z]+\\)_\\([A-Za-z0-9]+\\)\\([']*\\)\\($\\|[^}_]\\)" "\\1{}\\\\mathbf{\\2}_{\\\\mathrm{\\3}}\\4\\5")
	;; Figuring out left, right, and infix | and ||
	("\\(^\\| \\|\t\\|(\\|\\[\\|\\[\\\\\\|<\\\\\\|<<\\\\\\|,\\|;\\)||\\([^ \t\n]\\)" "\\1{}\\\\left\\\\|\\2")
	("\\(^\\| \\|\t\\|(\\|\\[\\|\\[\\\\\\|<\\\\\\|<<\\\\\\|,\\|;\\)|\\([^ \t\n|]\\)" "\\1{}\\\\left\\2")
	("\\([^ \t\n]\\)||\\($\\| \\|\t\\|)\\|\\]\\|\\\\\\]\\|\\\\>\\|\\\\>>\\|,\\|;\\)" "\\1{}\\\\right\\\\|\\2")
	("\\([^ \t\n\\\\|]\\)|\\($\\| \\|\t\\|)\\|\\]\\|\\\\\\]\\|\\\\>\\|\\\\>>\\|,\\|;\\)" "\\1{}\\\\right|\\2")
	("" "|")
	(" || " "{}\\\\,\\\\|\\\\,")
	("||" "{}\\\\|")
	(" | " " \\\\mid ")
	;; ******* Here is a major divide.
	;; ** Before this point, a replacement string beginning with \\\\ generally has {} in front of it,
	;; ** and a replacement string beginning with \\1\\\\ is really \\1{}\\\\ .  Not so after this point.
	;; Delete redundant {} delimiters
	;; Need two (or more?) of this first one.
	("{}\\([^A-Za-z0-9{]\\)" "\\1")
	("{}\\([^A-Za-z0-9{]\\)" "\\1")
	("\\(\\llbracket\\){}\\([A-Za-z]\\)" "\\1 \\2")
	("\\(\\^\\|{\\){}" "\\1")
	;; ******* "Fixups" come after this
	;; Fixup for consecutive numbers separated by whitespace (ugh, bletch)
	("\\([0-9]\\)[ ]+\\([0-9]\\)" "\\1{}\\\\;\\2")
	;; Fixup for consecutive keywords (two copies in case there are three or more consecutive keywords)
	("\\(\\\\KWD{[A-Za-z\\\\_]+}\\)[ ]*\\(\\\\KWD{[A-Za-z\\\\_]+}\\)" "\\1\\\\;\\\\;\\2")
	("\\(\\\\KWD{[A-Za-z\\\\_]+}\\)[ ]*\\(\\\\KWD{[A-Za-z\\\\_]+}\\)" "\\1\\\\;\\\\;\\2")
	;; Fixup for \rightarrow or \Rightarrow followed by keyword
	("\\(\\\\[rR]ightarrow\\)[ ]*\\(\\\\KWD{[A-Za-z\\\\_]+}\\)" "\\1\\\\;\\2")
	;; Fixup for consecutive \VAR or \\TYP or \mathbb or \mathit or \mathbf or \mathrm or \GREEK items (two copies in case there are three or more consecutive items)
	("\\(\\(\\\\VAR\\|\\\\TYP\\|\\\\mathbb\\|\\\\mathit\\|\\\\mathbf\\|\\\\mathrm\\|\\\\GREEK\\){[A-Za-z0-9\\\\_]+}\\)[ ]*\\(\\(\\\\VAR\\|\\\\TYP\\|\\\\mathbb\\|\\\\mathit\\|\\\\mathbf\\|\\\\mathrm\\|\\\\GREEK\\){[A-Za-z0-9\\\\_]+}\\|{[A-Za-z]}\\)"
	 "\\1\\\\:\\3")
	("\\(\\(\\\\VAR\\|\\\\TYP\\|\\\\mathbb\\|\\\\mathit\\|\\\\mathbf\\|\\\\mathrm\\|\\\\GREEK\\){[A-Za-z0-9\\\\_]+}\\)[ ]*\\(\\(\\\\VAR\\|\\\\TYP\\|\\\\mathbb\\|\\\\mathit\\|\\\\mathbf\\|\\\\mathrm\\|\\\\GREEK\\){[A-Za-z0-9\\\\_]+}\\|{[A-Za-z]}\\)"
	 "\\1\\\\:\\3")
	;; Fixup for right encloser then space then \VAR or \\TYP or \mathbb or \mathit or \mathbf or \mathrm or \GREEK item
	("\\([])}]\\)[ ]+\\(\\(\\\\VAR\\|\\\\TYP\\|\\\\mathbb\\|\\\\mathit\\|\\\\mathbf\\|\\\\mathrm\\|\\\\GREEK\\){[A-Za-z0-9\\\\_]+}\\|{[A-Za-z]}\\)"
	 "\\1\\\\:\\2")
	;; Fixup for decimal literal followed by \VAR or \TYP or \mathbb or \mathtt or \mathit or \mathrm
	("\\([0-9][.]?\\)[ ]*\\(\\\\\\(VAR\\|TYP\\|mathbb\\|mathtt\\|mathit\\|mathrm\\){\\)" "\\1\\\\,\\2")
	;; Fixup for unary \OPR{...} followed by \mathit or \mathbf or \VAR
	("\\([([{=][ ]*\\\\OPR{[A-Za-z0-9\\\\_]+}\\)[ ]*\\(\\(\\\\mathit\\|\\\\MATHBF\\|\\\\VAR\\){[A-Za-z0-9\\\\_]+}\\|{[A-Za-z]}\\)"
	 "\\1\\\\:\\2")
	;; Fixup for unary \mathbin{\TYP{...}} followed by \TYP{...} or \mathrm{...}
	("\\([([{=][ ]*\\\\mathbin{\\\\TYP{[A-Za-z0-9\\\\_]+}}\\)[ ]*\\(\\\\\\(TYP\\|mathrm\\){[A-Za-z0-9\\\\_]+}\\|{[A-Za-z]}\\)"
	 "\\1\\\\:\\2")
	;; Fixup for two consecutive occurrences of \mathbin{\TYP{...}} followed by \TYP or \mathrm
	("\\(\\\\mathbin{\\\\TYP{[A-Za-z0-9\\\\_]+}}[ ]*\\\\mathbin{\\\\TYP{[A-Za-z0-9\\\\_]+}}\\)[ ]*\\(\\\\\\(TYP\\|mathrm\\){[A-Za-z0-9\\\\_]+}\\|{[A-Za-z]}\\)"
	 "\\1\\\\:\\2")
	;; Fixup for "opr" followed by an operator
	("\\(\\\\KWD{opr}[ ]*\\)\\(\\\\OPR{[A-Za-z0-9\\\\_]*}\\|\\\\not\\\\[A-Za-z]+\\>\\|\\\\[A-Za-z]+\\>\\|[=<>:]\\)" "\\1\\\\mathord{\\2}")
	;; Counter-fixups for previous one when "operator" was \VAR or \left or something
	("\\(\\\\KWD{opr}[ ]*\\)\\\\mathord{\\(\\\\left\\)}" "\\1\\2")
	("\\(\\\\KWD{opr}[ ]*\\)\\\\mathord{\\(\\\\lfloor\\)}" "\\1\\2")
	("\\(\\\\KWD{opr}[ ]*\\)\\\\mathord{\\(\\\\lceil\\)}" "\\1\\2")
	("\\(\\\\KWD{opr}[ ]*\\)\\\\mathord{\\(\\\\lhfloor\\)}" "\\1\\2")
	("\\(\\\\KWD{opr}[ ]*\\)\\\\mathord{\\(\\\\lhceil\\)}" "\\1\\2")
	("\\(\\\\KWD{opr}[ ]*\\)\\\\mathord{\\(\\\\lhhfloor\\)}" "\\1\\2")
	("\\(\\\\KWD{opr}[ ]*\\)\\\\mathord{\\(\\\\lhhceil\\)}" "\\1\\2")
	("\\(\\\\KWD{opr}[ ]*\\)\\\\mathord{\\(\\\\llbracket\\)}" "\\1\\2")
	("\\(\\\\KWD{opr}[ ]*\\)\\\\mathord{\\(\\\\[A-Za-z]+\\)}\\({[^}]\\)" "\\1\\2\\3")
	;; Fixup for "self" where it's in an innocuous place
	("\\([(,][ ]*\\)\\\\mathord{\\\\KWD{self}}\\([ ]*[,)]\\)" "\\1\\\\KWD{self}\\2")
	;; Fixup for "=" followed by a keyword
	("\\(=[ ]*\\)\\(\\\\KWD{[A-Za-z\\\\_]*}\\)" "\\1\\\\;\\2")
	;; Fixup for operator followed by "="
	("\\(\\\\[A-Za-z]+{}\\|[-!@#$%*+?]\\)=" "\\\\mathrel{\\1}=")
	;; Fixups for operator followed by subscript
	("\\\\OPR{BIG}[ ]*\\(\\\\OPR{[A-Z_]+}\\)\\\\limits_" "\\\\mathop{\\1}\\\\limits_")
	("\\(\\\\OPR{[A-Z_]+}\\)\\\\limits_" "\\\\mathop{\\1}\\\\limits_")
	;; Fixups for operator symbol followed by subscript
	("\\\\OPR{BIG}[ ]*\\(\\\\[a-z]+\\)\\({}\\)*\\\\limits_" "\\\\mathop{\\1}\\\\limits_")
	("\\(\\\\[a-z]+\\)\\({}\\)*\\\\limits_" "\\\\mathop{\\1}\\\\limits_")
	("\\\\mathop{\\\\sum}" "\\\\sum")
	("\\\\mathop{\\\\prod}" "\\\\prod")
	("\\\\mathop{\\\\wedge}" "\\\\bigwedge")
	("\\\\mathop{\\\\vee}" "\\\\bigvee")
	("\\\\mathop{\\\\cap}" "\\\\bigcap")
	("\\\\mathop{\\\\cup}" "\\\\bigcup")
	("\\\\mathop{\\\\odot}" "\\\\bigodot")
	("\\\\mathop{\\\\otimes}" "\\\\bigotimes")
	("\\\\mathop{\\\\oplus}" "\\\\bigoplus")
	("\\\\mathop{\\\\uplus}" "\\\\biguplus")
	;; Fixup for defining opr |self|
	("\\\\KWD{opr} \\\\left|\\\\mathord{\\\\KWD{self}}|" "\\\\KWD{opr} \\\\left|\\\\mathord{\\\\KWD{self}}\\\\right|")
	;; Fixup for defining opr ^(self, other)
	("\\\\KWD{opr} \\^(" "\\\\KWD{opr} \\\\mathord{\\\\hbox{\\\\tt\\\\char'136}}(")
	;; # operator
	(" # " " \\\\mathrel{\\\\hbox{\\\\tt\\\\char'43}} ")
	("\\([^@\\]\\)#\\([^@]\\)" "\\1\\\\mathinner{\\\\hbox{\\\\tt\\\\char'43}}\\2")
	;; Get rid of \GREEK
	("\\\\GREEK{o}" "{o}")
	("\\\\GREEK{\\([ABEHIKMNOPTXZ]\\)}" "\\\\mathrm{\\1}")
	("\\\\GREEK{\\([A-Za-z0-9\\\\_]*\\)}" "\\1{}")
	;; Make single-letter variables look good by deleting redundant braces
	("\\([^A-Za-z0-9]\\){\\([A-Za-z]\\)}\\([^A-Za-z0-9]\\)" "\\1\\2\\3")
	;; Surround lines with \( and \) \\
	("\n" "\\\\)\\\\\\\\\n\\\\(")
	;; Move \( to after leading tildes
	("^\\\\(\\({\\\\tt~*}\\)" "\\1\\\\(")
	;; Delete empty \(\) pairs
	("\\\\([ ]*\\\\)" "")
	;; Move poptabs to correct position (may need to add a \\)
	("\\\\-\\(\\(\\\\poptabs\\)+\\)\\($\\|\\\\\\\\\\)" "\\\\-\\\\\\\\\\1")
	;; Make blank lines pretty
	("\\\\\\\\\\(\\(\\\\poptabs\\)*\\)\n\\(\\(\\\\-\\)*\\)\\\\\\\\" "\\3\\\\\\\\[4pt]\\1")
	;; Delete extraneous spaces within braces
	("{ " "{")
	(" }" "}")
	))

(setq fortify-final-fixups
      '(
	;; Fixups for space before and after strings
	(" \\\\hbox{\\\\rm``" "\\\\;\\\\;\\\\hbox{\\\\rm``")
	("}''} " "}''}\\\\;\\\\;")
	;; Make single keywords look good
	("\\\\EXP{\\\\KWD{\\([^}]*\\)}}" "\\\\KWD{\\1}")
	;; Make single operators look good
	("\\\\EXP{\\\\OPR{\\(\\([A-Za-z0-9\\\\]\\|{\\\\char'137}\\)*\\)}}" "\\\\OPR{\\1}")
	;; Make single variables look good
	("\\\\EXP{\\\\VAR{\\([^}]*\\)}}" "\\\\VAR{\\1}")
	;; Make single types look good
	("\\\\EXP{\\\\TYP{\\([^}]*\\)}}" "\\\\TYP{\\1}")
	("\\\\EXP{\\\\mathbin{\\\\TYP{\\([^}]*\\)}}}" "\\\\TYP{\\1}")
	;; Make single variables look good
	("\\\\EXP{\\\\mathit{\\([^}]*\\)}}" "\\\\VAR{\\1}")
	("\\\\EXP{\\([A-Za-z]\\)}" "\\\\VAR{\\1}")
	))

;;; We assume these TeX macro definitions:
;;; \def\Method#1{...}

(defun method-fortify (prefix-arg)
  "Format the region as Fortress method definition headers.
The region is assumed to be a sequence of method declarations.
Each method declaration may span one or more lines of text.
Any line whose indentation matches that of the first line
is assumed to start a new method declaration.  Each method
declaration is formatted as Fortress code and becomes the
argument of the TeX macro \\Method (but \\Method* is used
for each declaration other than the first, except that a
blank line means that the next method uses \\Method again).
If the first character in the region is %, then the region
is first copied, and within the copy all lines have any
initial % character removed before the text is formatted,
but if the first two characters in the region are %%, then
C-U M-X comment-region is called instead, to do a cleverer job
of removing the comment characters; thus the net result is
that the original region precedes the formatted code.
In this way, Fortress code can be kept within a TeX source file
as a comment and easily altered and reformatted as necessary."
  (interactive "*p")
  (if (not (mark)) (keyboard-quit))
  (let* ((case-fold-search nil)
	 (old-point (copy-marker (point)))
	 (old-mark (copy-marker (mark)))
	 (overall-end (copy-marker (region-end)))
	 (overall-start (copy-marker (region-beginning)))
	 (chunk-start (make-marker))
	 (start (make-marker))
	 (end (make-marker)))
    (goto-char (marker-position overall-end))
    (while (> (point) (marker-position overall-start))
      ;; Find a nonblank line
      (while (and (> (point) (marker-position overall-start))
		  (or (= (point) (+ 1 (marker-position overall-start)))
		      (= (char-before (- (point) 1)) ?\n)))
	(forward-line -1))
      (set-marker end (point))
      ;; Find a blank line or overall start
      (while (and (> (point) (+ 1 (marker-position overall-start)))
		  (not (= (char-before (- (point) 1)) ?\n)))
	(forward-line -1))
      (set-marker chunk-start (point))
      (set-marker start (point))
      (cond ((and (< start end) (= (char-after (- (marker-position end) 1)) ?\n))
	     (set-marker end (- (marker-position end) 1))))
      (untabify start end)
      (cond ((and (= (char-after (marker-position start)) ?\%)
		  (or (null (char-before (marker-position start)))
		      (= (char-before (marker-position start)) ?\n)))
	     ;; Make a copy
	     (let ((region-data (buffer-substring (marker-position start) (marker-position end))))
	       (goto-char (marker-position end))
	       (insert "\n")
	       (set-marker start (point))
	       (insert region-data)
	       (set-marker end (point))
	       (goto-char (marker-position start))
	       (cond ((looking-at "%%") (comment-region (marker-position start) (marker-position end) '(4)))
		     (t (delete-char 1)
			;; Remove leading % from each line
			(process-fortify-rules '(("\n%" "\n")) (marker-position start) (marker-position end)))))))
      (let ((method-indent (current-indentation))
	    (first-method t))
	(goto-char (marker-position start))
	(while (< (point) (marker-position end))
	  (cond ((= (current-indentation) method-indent)
		 (delete-char (current-indentation))
		 (if first-method (insert "\\Method{") (insert "\\Method*{"))
		 (setq first-method nil)
		 (set-mark (point))
		 (end-of-line)
		 (fortify 4)
		 (insert "}")
		 (forward-line))
		(t (backward-char)
		   (delete-char -1)
		   (insert "\\\\")
		   (forward-char)
		   (delete-char (min (current-indentation) method-indent))
		   (insert "\\hbox{\\tt")
		   (while (= (char-after (point)) ?\ )
		     (delete-char 1)
		     (insert "~"))
		   (insert "}")
		   (set-mark (point))
		   (end-of-line)
		   (fortify 4)
		   (insert "}")
		   (forward-line)))))
      (goto-char (marker-position chunk-start)))
    (set-marker chunk-start nil)
    (set-marker start nil)
    (set-marker end nil)
    (set-marker overall-start nil)
    (set-marker overall-end nil)
    (set-mark (marker-position old-mark))
    (set-marker old-mark nil)
    (let ((old-point-position (marker-position old-point)))
      (set-marker old-point nil)
      (goto-char old-point-position))))

(defun batch-fortify ()
    "Fortify the whole buffer and write to a filename in pwd, with
extension .tex."
    (remove-copyright)
    (print-header "TOOL BATCH-FORTIFY")
    (mark-whole-buffer)
    (fortify 4)
    (write-as-tex-file))

(defun fortick ()
  "Fortify all sections of a buffer delimited with backticks, and
write to a file in the same location as the read file, but with
extension '.tex'."
  (remove-copyright)
  (print-header "TOOL FORTICK")
  (let ((pos (point-min))
	(at-end nil))
    (while (not at-end)
      (let ((next-opening (find-next-tick pos)))
	     (if (equal next-opening (point-max))
		 (setq at-end t)
	       (progn
		 (let ((next-closing (find-next-tick (+ 1 next-opening))))
		   (cond ((equal next-closing (point-max))
			  (signal-error "Mismatched tick"))
			 ((equal next-closing (+ 1 next-opening))
			  ;; Two adjacent ticks denotes an escaped tick.
			  (delete-char-at next-opening)
			  (setq pos next-closing))
			 (t (delete-char-at next-opening)
			    ;; The left tick has been deleted, so the right tick
			    ;; has moved to the left.
			    (setq next-closing (- next-closing 1))
			    (delete-char-at next-closing)
			    (fortify-region next-opening next-closing)
			    (setq pos next-closing)))))))))
  (write-as-tex-file))

(defun delete-char-at (pos)
  (save-excursion
    (goto-char pos)
    (delete-char 1)))

(defun find-next-tick (pos)
  (while (and (< pos (point-max))
	      (not (equal (char-to-string (char-after pos)) "`")))
    (setq pos (+ 1 pos)))
  pos)

(defun fortify-region (left right)
  (save-excursion
    (goto-char left)
    (push-mark)
    (goto-char right)
    (fortify-if-not-blank-space)))

(defun fortex ()
  "Fortify the whole buffer expect for stylized comments. Strip
  stylized comments of leading comment characters. Write the result to
  a file in the same location as the read file, but with extension
  .tex. Using this function, it is possible to write a legal Fortress
  file with doc comments (possibly containing embedded LaTeX commands)
  and produce a LaTeX file where all Fortress code is fortified and
  all doc comments are written as LaTeX prose describing the code."
  (remove-copyright)
  (print-header "TOOL FORTEX")
  (let ((more-lines t))
    (goto-start-of-buffer)
    (while more-lines
       (cond ((at-start-of-doc-comment)
	      (remove-doc-comment-chars)
	      (setq more-lines (down-left-if-more-lines)))
	     ((at-start-of-tests)
	      (setq more-lines (omit-tests)))
	     (t (fortify-next-code-block)
		(setq more-lines (down-left-if-more-lines)))))
  (write-as-tex-file)))

(defun foreg ()
  "Fortify the region of the buffer delimited by special doc comments
  '(** EXAMPLE **)' and '(** END EXAMPLE **)'. Remove all other text,
  and save the result to a file at the same location as the read file,
  but with extension .tex."
  (remove-copyright)
  (print-header "TOOL FOREG")
  (let ((more-lines t))
    (goto-start-of-buffer)
    (while more-lines
      (if (at-start-of-example)
	  (setq more-lines (fortify-example))
	(setq more-lines (delete-line))))
  (write-as-tex-file)))

(defun remove-doc-comment-chars ()
  "Removes beginning comment characters of the contiguous doc comment
  starting at point. Once finished, point is at the very end of the
  processed doc comment."
  (requires (at-start-of-doc-comment))

  (let ((more-lines t))
    (remove-start-of-doc-comment)
    (while (and more-lines (not (at-end-of-doc-comment)))
      (remove-middle-of-doc-comment)
      (setq more-lines (down-left-if-more-lines)))
    (if (at-end-of-doc-comment)
	(remove-end-of-doc-comment)
      (signal-error "Missing end of doc comment"))))

(defun fortify-next-code-block ()
  "Finds and fortifies the contiguous block of Fortress code start at
  point and ending at the next doc comment (or the end of the
  file). Once finished, point is at the very end of the block of
  code."
  (requires (not (at-start-of-doc-comment)))
  (let ((more-lines t))
    (push-mark)
    (while (and more-lines
		(not (or (at-start-of-doc-comment)
			 (at-start-of-tests))))
      (setq more-lines (down-left-if-more-lines)))
    (if (or (at-start-of-doc-comment)
	    (at-start-of-tests))
	(progn (forward-line -1)
	       (end-of-line))
      (end-of-line))
    (fortify-if-not-blank-space)))

(defun fortify-if-not-blank-space ()
  "Checks that the region isn't empty before calling fortify."
  (if (not (all-blank-spacep (region-beginning) (region-end)))
      (fortify 4)))

(defun fortify-example ()
  "Fortifies example code delimited by special doc comments
  '(** EXAMPLE **)' and '(** END EXAMPLE **)'."
  (requires (at-start-of-example))

  (delete-line)
  (push-mark)
  (let ((more-lines t))
    (while (and more-lines (not (at-end-of-example)))
      (setq more-lines (down-left-if-more-lines)))
    (if (at-end-of-example)
	(progn (fortify-if-not-blank-space)
	       (delete-line))
      (signal-error "Example must be terminated with '(* END EXAMPLE *)'."))))

(defun remove-start-of-doc-comment ()
  "Simple helper function that removes the leading whitespace and '('
of a doc comment."
  (requires (at-start-of-doc-comment))

  (beginning-of-line)
  (delete-whitespace)
  ;; Precondition assures that the next character is "("
  (delete-char 1))

(defun remove-middle-of-doc-comment ()
  "Simple helper function that removes the leading asterisks and whitespace
of a line in the middle of a doc comment."
  (beginning-of-line)
  (delete-whitespace)
  (delete-asterisks)
  (delete-whitespace)

  ;; Check if this line is the end of the doc comment.
  ;; If so, move the end of the comment to the next line,
  ;; so it'll be picked up when that line is processed.
  (end-of-line)
  (skip-preceding-whitespace)
  (if (<= (line-beginning-position)
	  (- (point) 3))
      (progn (forward-char -3)
	     (if (at-end-of-doc-comment)
		 (progn (insert-char `?\n' 1)
			(forward-char -1))))))


(defun remove-end-of-doc-comment ()
  "Simple helper function that removes the ending '**)' at the end of a
doc comment."
  (requires (at-end-of-doc-comment))

  (beginning-of-line)
  (delete-whitespace)
  ;; Precondition assures that the next three characters are "**)"
  (delete-char 3)
  ;; Ensure that no text occurs on line, after the end of the doc comment
  (skip-leading-whitespace)
  (if (not (eolp))
      (signal-error
       "No extra text allowed on last line of a doc comment"))
  (delete-line)
  ;; There must be at least one line above us (i.e., the former start
  ;; of the doc comment)
  (forward-line -1))

(defun at-start-of-copyright () (at-line "(** COPYRIGHT **)"))
(defun at-end-of-copyright () (at-line "(** END COPYRIGHT **)"))

(defun at-start-of-example () (at-line "(** EXAMPLE **)"))
(defun at-end-of-example () (at-line "(** END EXAMPLE **)"))

(defun at-start-of-tests () (at-line "(** TESTS **)"))
(defun at-end-of-tests () (at-line "(** END TESTS **)"))

(defun remove-block (test-for-start test-for-end error-string)
  "Removes everything up to the line designated by `test-for-end`.
If that line isn't found, signal the error message `error-string`.
This function should only be called at a position where `test-for-start`
is true."
  (requires (funcall test-for-start))

  (let ((not-at-eof t))
    (while (and not-at-eof
		(not (funcall test-for-end)))
      (setq not-at-eof (delete-line)))
    (if (funcall test-for-end)
	(delete-line)
      (signal-error error-string))))

(defun remove-copyright ()
  (if (at-start-of-copyright)
      (remove-block 'at-start-of-copyright 'at-end-of-copyright
		    (concat "Copyright notice must be terminated with"
			    "'(** END COPYRIGHT **)'."))))

(defun omit-tests ()
  (remove-block 'at-start-of-tests 'at-end-of-tests
		"Tests must be terminated with '(** END TESTS **)'"))

(defun at-line (line)
    "Boolean function that determines whether point is at the
beginning of an example, delimited by the doc comment '(** EXAMPLE **)'."
    (save-excursion
      (beginning-of-line)
      (skip-leading-whitespace)
      (let ((left (point)))
	(end-of-line)
	(skip-preceding-whitespace)
	(let* ((right (point))
	       (candidate (buffer-substring left right)))
	  (equal candidate line)))))

(defun at-start-of-doc-comment ()
  "Boolean function that determines whether point is at the beginning of a
doc comment."
  (save-excursion
    (beginning-of-line)
    (skip-leading-whitespace)
    (and (char-after (+ 2 (point)))
	 (equal "(" (char-to-string (char-after (point))))
	 (equal "*" (char-to-string (char-after (+ 1 (point)))))
	 (equal "*" (char-to-string (char-after (+ 2 (point))))))))

(defun at-end-of-doc-comment ()
  "Boolean function that determines whether point is at the en of a
doc comment."
  (save-excursion
    (skip-leading-whitespace)
    (and (char-after (+ 2 (point)))
	 (equal "*" (char-to-string (char-after (point))))
	 (equal "*" (char-to-string (char-after (+ 1 (point)))))
	 (equal ")" (char-to-string (char-after (+ 2 (point))))))))

(defun delete-whitespace ()
  "Simple helper function that deletes all whitespace immediately following
point."
  (while (and (char-after (point))
	      (whitespacep (char-to-string (char-after (point)))))
    (delete-char 1)))

(defun skip-leading-whitespace ()
  "Simple helper function that moves point to the next non-whitespace
character."
  (while (and (char-after (point))
	      (whitespacep (char-to-string (char-after (point)))))
    (forward-char)))

(defun skip-preceding-whitespace ()
  "Simple helper function that moves point to the previous non-whitespace
character."
  (while (and (char-before (point))
	      (whitespacep (char-to-string (char-before (point)))))
    (backward-char)))


(defun whitespacep (string)
  (or (equal string " ")
      (equal string "\t")))

(defun blank-spacep (string)
  (or (whitespacep string)
      (equal string "\n")
      (equal string "\f")
      (equal string "\r")))

(defun all-blank-spacep (left right)
  (let ((result t))
    (while (< left right)
      (setq result (and result
			(blank-spacep (char-to-string (char-after left)))))
      (setq left (1+ left)))
    result))

(defun delete-asterisks ()
  "Simple helper function that deletes a sequence of asterisks immediately
after point."
  (while (and (char-after (point))
	      (equal "*" (char-to-string (char-after (point)))))
    (delete-char 1)))

(defun delete-line ()
  "Delete the current line and, if successful, return t. If current
  line is the last line in the buffer (i.e., there is no newline
  character at the end of it), delete its contents and return nil."
  (beginning-of-line)
    (let ((left (point)))
      (end-of-line)
      (let ((right (point)))
	(if (char-after right)
	    ;; Delete the newline character immediately to the right
	    ;; of the line.
	    (progn (delete-region left (1+ right))
		   t)
	  (progn (delete-region left right)
		 nil)))))

(defun down-left-if-more-lines ()
  "If there is a next line after point, moves point to the beginning of the
next line and returns true. Returns false otherwise."
  (let ((last-line nil))
    (save-excursion
      (end-of-line)
      (setq not-last-line (not (equal (point) (point-max)))))
    (if not-last-line
	(progn (forward-line 1)
	       (beginning-of-line)))
    not-last-line))

(defun goto-start-of-buffer ()
  "Simple helper function that moves point to the start of the buffer."
  (goto-char (point-min)))

(defun write-as-tex-file ()
  "Writes the current buffer to a new file with the same name and location,
but with suffix '.tex'."
  (write-file (concat
	       (file-name-sans-extension
		(file-name-nondirectory (buffer-file-name)))
	       ".tex")))

;; This isn't working yet
(defun print-header (author)
;;  (insert (concat "%% THIS FILE WAS AUTOGENERATED BY " author
;;		 " AT FORTRESS_HOME/Fortify/fortify.el"))
;;  (newline)
;;  (insert (concat "%% FROM SOURCE FILE" (buffer-file-name) "\n"))
;;  (newline)
;;  (forward-line))
)

(defun requires (condition)
  "Takes a condition and signals an error if the condition is false.
Intended to be used at the beginning of a function definition,
as a poor man's contract facility."
  (if (not condition)
      (signal-error "Precondition violated")))

(defun signal-error (msg)
  "Signals an error, indicating the file name of the current buffer,
the line position of point, and the given message."
  (error (concat (buffer-file-name) ": " msg)))
