(* Exceptions *)
trait Exception comprises { CheckedException, UncheckedException }
  settable message: Maybe[\String\]
  settable chain: Maybe[\Exception\]
end

trait CheckedException
  extends { Exception }
  excludes { UncheckedException }
end

trait UncheckedException
  extends { Exception }
  excludes { CheckedException }
end

(* Threads *)
trait Thread[\T extends Any\]
  val(): T
  wait(): ()
  ready(): Boolean
  stop(): () throws Stopped
end

(* Tests *)
test object TestSuite(testFunctions = {})
  add(f: () -> ()): ()
  run(): ()
end

test fail(message: String): ()
