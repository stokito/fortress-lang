(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
    (** %asString% returns the underlying non-case-insensitive %String%, %s%. **)
 
    opr juxtaposition(self, other:CaseInsensitiveString): CaseInsensitiveString
end

end
