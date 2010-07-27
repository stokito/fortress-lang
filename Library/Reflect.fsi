(*******************************************************************************
    Copyright 2010 Kang Seonghoon, KAIST
    All rights reserved.
 ******************************************************************************)

api Reflect

trait AnyReflect (* %comprises Reflect[\T\] where [\T\]% *)
end

object Reflect[\T\]() extends AnyReflect
    getter asString():String
    join(self, other:AnyReflect):AnyReflect
    meet(self, other:AnyReflect):AnyReflect
end

end
