(*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************)

api CaseInsensitiveString

(** A %CaseInsensitiveString% is a wrapper around a %String% that
    totally orders the underlying string case-insensitively.  This is
    useful when using strings in a data structure that expects them to
    extend StandardTotalOrder, for example as keys in a map or
    elements of a set. **)
value object CaseInsensitiveString(s:String)
        extends { StandardTotalOrder[\CaseInsensitiveString\],
                  ZeroIndexed[\Char\], DelegatedIndexed[\Char,ZZ32\] }
    (** %toString% returns the underlying non-case-insensitive %String%, %s%. **)
    getter toString() : String
    opr juxtaposition(self, other:CaseInsensitiveString): CaseInsensitiveString
end

end