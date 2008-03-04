;;;
;;; fortress-mode.el - Fortress mode for Emacs
;;;
;;;   Copyright (c) 2007 Yuto Hayamizu (y.hayamizu[at]gmail.com)
;;;
;;;   Redistribution and use in source and binary forms, with or without
;;;   modification, are permitted provided that the following conditions
;;;   are met:
;;;
;;;   1. Redistributions of source code must retain the above copyright
;;;      notice, this list of conditions and the following disclaimer.
;;;
;;;   2. Redistributions in binary form must reproduce the above copyright
;;;      notice, this list of conditions and the following disclaimer in the
;;;      documentation and/or other materials provided with the distribution.
;;;
;;;   3. Neither the name of the authors nor the names of its contributors
;;;      may be used to endorse or promote products derived from this
;;;      software without specific prior written permission.
;;;
;;;   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
;;;   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
;;;   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
;;;   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
;;;   OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
;;;   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
;;;   TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
;;;   PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
;;;   LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
;;;   NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
;;;   SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
;;;

;;
;; Memorandum
;;
;; * candidate features
;;  - RR -> R(blackboard bold face) translation
;;  - ...
;;


(defvar fortress-mode-map (make-sparse-keymap))

(if fortress-mode-map
    (let ((km fortress-mode-map))
      (define-key km "\t" 'fortress-indent-command)
      nil))

(defvar fortress-mode-syntax-table nil "")

(if fortress-mode-syntax-table
    ()
  (setq fortress-mode-syntax-table (make-syntax-table))
  (modify-syntax-entry ?  " " fortress-mode-syntax-table)
  (modify-syntax-entry ?\t " " fortress-mode-syntax-table)
  (modify-syntax-entry ?\' "\"" fortress-mode-syntax-table)
  (modify-syntax-entry ?\" "\"" fortress-mode-syntax-table)
  (modify-syntax-entry ?_ "_" fortress-mode-syntax-table)
  (modify-syntax-entry ?+ "." fortress-mode-syntax-table)
  (modify-syntax-entry ?- "." fortress-mode-syntax-table)
  (modify-syntax-entry ?* ". 23" fortress-mode-syntax-table)
  (modify-syntax-entry ?/ ". " fortress-mode-syntax-table)
  (modify-syntax-entry ?(  "() 1" fortress-mode-syntax-table)
  (modify-syntax-entry ?)  ")( 4" fortress-mode-syntax-table)
;  (modify-syntax-entry ?  "" fortress-mode-syntax-table)
  )

(facep 'highlight)

(defcustom fortress-indent-level 2
  "Indentation of fortress statements."
  :type 'integer :group 'fortress)

(defcustom fortress-comment-column 32
  "Indentation column of comments"
  :type 'integer :group 'fortress)

(defcustom fortress-command "fortress"
  "Shell command of fortress"
  :type 'string :group 'fortress)

(defun fortress-mode-init-variables ()
  (make-variable-buffer-local 'comment-start)
  (setq comment-start "(* ")
  (make-variable-buffer-local 'comment-end)
  (setq comment-end " *)")
  (make-variable-buffer-local 'comment-column)
  (setq comment-column fortress-comment-column)
  (make-variable-buffer-local 'indent-line-function)
  (setq indent-line-function 'fortress-indent-command)
  (make-variable-buffer-local 'fortress-render-mode)
  (setq fortress-render-mode nil)
  (defface font-lock-fortress-type-face
    `((t nil))
    "Face for fortress builtin types."
    :group 'fortress-faces :group 'faces)
  (copy-face 'font-lock-type-face 'font-lock-fortress-type-face)
  (set-face-attribute 'font-lock-fortress-type-face nil :weight 'normal)
  )

(defvar fortress-font-lock-keywords nil "")
(defvar fortress-font-lock-syntactic-keywords nil "")

(if fortress-font-lock-keywords
    ()
  (defvar fortress-font-lock-reserved-words nil)
  (defvar fortress-builtin-types
    (list
     "Any" "Number" "Integral"
     "ZZ32" "ZZ32Range" "ZZ64"
     "RR64"
     "String"
     "IntLiteral"
     "FloatLiteral"
     "Boolean"))

  (defvar fortress-builtin-functions nil)
  (setq 
   fortress-builtin-functions
   (list
    "println"

    ;; mathematical functions
    "sin" "cos" "tan" "cot" "sec" "csc"
    "sinh" "cosh" "tanh" "coth" "sech" "csch"
    "arcsin" "arccos" "arctan" "arccot" "arcsec" "arccsc"
    "arsinh" "arcosh" "artanh" "arcoth" "arsech" "arcsch"
    "asin" "acos" "atan" "acot" "asec" "acsc"
    "asinh" "acosh" "atanh" "acoth" "asech" "acsch"
    "arg" "deg" "det" "exp" "inf" "sup" "lg" "ln" "log"
    "gcd" "max" "min"
    ))
  
  (setq
   fortress-font-lock-keywords
   (list
    ;; reserved words
    (cons
     (concat
      "\\<\\("
      (regexp-opt
       (list
	"BIG" "SI_unit" "absorbs" "abstract also" "api" "as" "asif"
	"at" "atomic" "bool" "case" "catch" "coerces" "coercion"
	"component" "comprises" "default" "dim" "do" "elif" "else"
	"end" "ensures" "except" "excludes" "exit" "export" "extends"
	"finally" "fn" "for" "forbid" "from" "getter" "hidden" "ident"
	"idiom" "if" "import" "in" "int" "invariant" "io" "juxtaposition"
	"label" "largest" "nat" "object" "of" "or" "opr" "private"
	"property" "provided" "requires" "self" "settable" "setter"
	"smallest" "spawn" "syntax" "test" "then" "throw" "throws"
	"trait" "transient" "try" "tryatomic" "type" "typecase" "unit"
	"value" "var" "where" "while" "widening" "widens" "with" "wrapped"))
      "\\)\\>")
     'font-lock-keyword-face)

    ;; functions
    '("\\<\\([_A-Za-z]+\\)\\>([^)]*)\\(?::()\\)?\\s-*="
      1 font-lock-function-name-face)

    ;; variables
    '("\\<\\([_A-Za-z]+\\)\\>\\s-*=" 1 font-lock-variable-name-face)
    `(,(concat
      "\\<\\([_A-Za-z]+\\)\\s-*:\\s-*\\(?:"
      (regexp-opt fortress-builtin-types)
      "\\)") 1 font-lock-variable-name-face)

    ;; component name
    '("\\<component\\>\\s-+\\<\\(\\w+\\)\\>" 1 font-lock-type-face)

    ;; api name
    '("\\<export\\>\\s-+\\<\\(\\w+\\)\\>" 1 font-lock-type-face)

    ;; builtin functions
    `(,(concat "\\<\\(" (regexp-opt fortress-builtin-functions) "\\)\\>")
      1 'font-lock-builtin-face)

    ;; experimental code for pretty mathematical type-name rendering
    `("\\<\\(RR\\)64\\>"
      1 (progn (compose-region (match-beginning 1) (match-end 1)
			       (fortress-with-unicode-char MATHBB-R))
	       'font-lock-fortress-type-face))
    `("\\<\\(ZZ\\)\\(64\\|32\\)\\>"
      1 (progn (compose-region (match-beginning 1) (match-end 1)
			       (fortress-with-unicode-char MATHBB-Z))
	       'font-lock-fortress-type-face))

    ;; builtin type name
    `(,(concat "\\<\\(" (regexp-opt fortress-builtin-types) "\\)\\>")
      1 'font-lock-fortress-type-face)

    ;; leftward arrow
    `("\\(<-\\)"
      1 (progn (compose-region (match-beginning 1) (match-end 1)
			       (fortress-with-unicode-char LEFTWARDS-ARROW))
	       nil))
    ))

  (defun fortress-set-font-lock-keywords ()
    (setq font-lock-defaults
	  (list 'fortress-font-lock-keywords nil nil nil nil)))

  (add-hook 'fortress-mode-hook 'fortress-set-font-lock-keywords)
  t)

(defun fortress-mode ()
  "Major mode for editing Fortress code to run in Emacs. "
  (interactive)
  (kill-all-local-variables)
  (fortress-mode-init-variables)
  (use-local-map fortress-mode-map)
  (setq major-mode 'fortress-mode)
  (setq mode-name "Fortress")
  (set-syntax-table fortress-mode-syntax-table)
  (run-hooks 'fortress-mode-hook)
  )

(defun fortress-indent-command ()
  (interactive)
  (fortress-indent-line t))

(defun fortress-indent-line (&optional flag)
  "Correct indentation of the current fortress line."
   (fortress-indent-to (fortress-calculate-indent)))

(defun fortress-indent-to (col)
  (let (top shift bol-mark)
    (setq shift (current-column))
    (beginning-of-line)
    (setq bol-mark (point))
    (back-to-indentation)
    (setq top (current-column))
    (if (< shift top)
	(setq shift 0)
      (setq shift (- shift top)))
    (if (and (bolp) (= 0 col))
	(move-to-column shift)
      (delete-region bol-mark (point))
      (indent-to col)
      (move-to-column (+ shift col)))))

;; (defun fortress-calculate-indent ()
;;   (let (top-col)
;;     (save-excursion
;;       (beginning-of-line)
;;       (back-to-indentation)
;;       (setq top-col (current-column)))
;;     (+ 2 top-col)))

(defun fortress-calculate-indent ()
  (defun get-line-relative (line)
    (save-excursion
      (forward-line line)
      (buffer-substring
       (line-beginning-position 1)
       (line-end-position 1))))
  
  (let ((nb-rel -1) ;; relative point of nearest non-blank line before current line
	(nb-str nil) ;; string of nb-rel line
	(nb-indent nil)) ;; indentation of nb-rel line

    ;; find the nearest non-blank line before current line
    (while (string-match "^\\s-*$" (get-line-relative nb-rel))
      (setq nb-rel (- nb-rel 1)))
    ;; and get the string of the line
    (setq nb-str (get-line-relative nb-rel))
    ;; and also get the point of the line
    (save-excursion
      (forward-line nb-rel)
      (beginning-of-line)
      (setq nb-indent (* 2 (floor (/ (current-indentation) 2)))))

    (cond
     ((string-match "do\\s-*$" nb-str) ;; in a block
      (+ nb-indent fortress-indent-level))
     ((string-match "component\\s-+[a-z]+\\s-*$" nb-str) ;; in a component
      (+ nb-indent fortress-indent-level))
     ((string-match "end\\s-*$" (get-line-relative 0))
      (max 0 (- nb-indent fortress-indent-level)))
     (t
      nb-indent))))

(defun fortress-run (filename)
  (interactive)
  filename)

(defmacro fortress-with-unicode-char (body &rest bodies)
  (let ((ucs-str-pairs
	 (mapcar
	  (lambda (pair)
	    `(,(car pair) ,(decode-char 'ucs (cadr pair))))
	  '((MATHBB-C #x2102)
	    (MATHBB-H #x210d)
	    (MATHBB-N #x2115)
	    (MATHBB-P #x2119)
	    (MATHBB-Q #x211A)
	    (MATHBB-R #x211D)
	    (MATHBB-Z #x2124)
	    (HORIZONTAL-ELLIPSIS #x2026)
	    (RIGHTWARDS-ARROW-FROM-BAR #x21A6)
	    (FOR-ALL #x2200)
	    (THERE-EXISTS #x2203)
	    (COLON-EQUALS #x2254)
	    (RIGHTWARDS-ARROW #x2192)
	    (LEFTWARDS-ARROW #x2190)
	    (RIGHTWARDS-DOUBLE-ARROW #x21D2)
	    (MATHEMATICAL-LEFT-WHITE-SQUARE-BRACKET #x27e6)
	    (MATHEMATICAL-RIGHT-WHITE-SQUARE-BRACKET #x27e7)
	    (INFINITY #x221E)
	    (DOWN-TACK #x22A4)
	    (UP-TACK #x22A5)
	    ))
	 ))
    `(let ,ucs-str-pairs ,body ,@bodies)))

; (macroexpand '(fortress-with-unicode-char 'hoge 'fuga))

(provide 'fortress-mode)
