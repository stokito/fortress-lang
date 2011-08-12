/**
 *  CFTest.scala
 * 
 *  Run with argument "test" to run unit tests for BasicCoreFortress.scala
 *
 *  Run without arguments to start the interactive interpreter
 *
 */

import BasicCoreFortress._
import UnParser._
import scala.util.parsing.combinator._
import scala.io.Source

object CFTest {

  val debug = true

  // CFP - variable for the current CFParser, set to different values throughout tests to change the trait and object name contexts in which terms are parsed
  var CFP = PreParser.makeCFParser("")


  def main(args:Array[String]) {

    if (args.length > 0 && args(0) == "test")
      test()
    else
      interactive()
  }

  //
  // Interactive "shell"
  //
  def interactive() {
  
    val welcome = """
*******************************
Basic Core Fortress Interpreter
*******************************
:load <file>  - Load trait and object definitions from file
:defs - Show loaded definitions
:quit  - Exit interpreter

Rules to keep parser happy:

 * a space is required before the opening bracket for static args
   Example: \"A [\\]\" OK, \"A[\\]\" Bad

 * a space is required before the closing bracket in nonempty static args
   Example: \"[\T extends A \]\" OK, \"[\T extends A\]\" Bad

 * All trait defs must precede all object defs in a program

 * object and method declarations and instantiations must always have (dynamic) params/args in parentheses.
   But static params, supertraits are optional

Enter an expression to have it typechecked and evaluated
    """
    
    var tds = Env[TraitName, TraitDef](MtCons(), MtCons())
    var ods = Env[ObjectName, ObjectDef](MtCons(), MtCons())
    
    var defs = ""
    var command = ""

    println(welcome)
    do {
      print("BCF> ")
      command = readLine()
      if (command.length >= 5 && command.substring(0, 5) == ":load") {
	// :load command
	try {
	  val filename = command.substring(6)
	  val newDefs = Source.fromFile(filename).mkString

	  println(filename + ":\n")
	  println(newDefs + "\n")

	  val restoreCFP = CFP
	  val newCFP = PreParser.makeCFParser(newDefs)
	  CFP = new CFParser(newCFP.traits ++ CFP.traits,
			     newCFP.objects ++ CFP.objects)
	  
	  try {
	    val newP = program(newDefs + " dummy")
	    tds = newP.tds.prepend(tds)
	    ods = newP.ods.prepend(ods)
	    val defsp = Program(tds, ods, e("dummy"))

	    if (tds.values.forall(ok(_, defsp)) && 
		ods.values.forall(ok(_, defsp)))
	      {
		CFP = new CFParser(tds.keys.toScalaList, ods.keys.toScalaList)
		unparseTds(tds) + unparseOds(ods)
		println("Typecheck passed.\nLoaded " + filename)
	      }
	    else 
	      {
		CFP = restoreCFP
		unparseTds(tds) + unparseOds(ods)
		println("Typecheck failed. " + filename + " was not loaded.")
	      }
	  } catch {
	    case e => 
	      {
		CFP = restoreCFP
		println(e)
		println("\n" + filename + " was not loaded.")
	      }
	  }
	}
	catch {
	  case e => println(e)
	}

      } else if (command == ":defs") {
	// :defs command
	println("\n" + unparseTds(tds) + unparseOds(ods))

      } else if (command == ":quit") {
	// :quit command
	println("Goodbye")

      } else { 
	// Interpret command as expression
	try {
	  val p = program(unparseTds(tds) + "  " + unparseOds(ods) + " \n" + command)
	  typeof(p) match {
	    case Some(ty) => 
	      eval(p) match {
		case Some(v) => println(unparse(v) + " :" + unparse(ty))
		case None => println("Uh-oh. Typecheck succeeded but evaluation failed. This shouldn't happen if BCF is type-safe.")
	      }
	    case None => println("Untypable")
	  }

	} catch {
	  case e => println(e)
	}
      }	
    } while (command != ":quit")
  }

  //
  // Run unit tests
  //
  def test() {

    testList()
    testEnv()
    testBasicCoreFortress()

    // Done
    println("All tests passed!")
  }

  //
  // test List[T]
  //
  def testList() {

    assert(! MtCons[Int].exists(_ == 0) )
    assert( Cons(1, MtCons()).exists(_ == 1) )
    assert(! Cons(1, MtCons()).exists(_ == 0) )

    assert( Cons(1, Cons(2, MtCons())).map(x => x + 1) ==
	    Cons(2, Cons(3, MtCons())) )

    assert( Cons(1, MtCons()).zip(Cons(2, MtCons())).exists(_ == (1,2)) )

    assert( Cons(1, Cons(2, MtCons())).foldr(3, ((_:Int) + (_:Int))) == 6)
    assert( Cons(1, Cons(2, MtCons())).foldl(3, ((_:Int) + (_:Int))) == 6)

    assert( MtCons[Int].forall(x => false) )
    assert( Cons(true, Cons(true, MtCons())).forall(x => x) )
    assert(! Cons(false, MtCons()).forall(x => x) )
    assert(! (Cons(true, Cons(false, MtCons())).forall(x => x)) )
    
    println("List tests passed!")

  }

  //
  // test Env[A,B]
  //
  def testEnv() {

    val m = Env(Cons(1, Cons(2, MtCons())), Cons('a', Cons('b', MtCons())))
    val mt1 = Env.mt[Int, Char]

    assert( Env(MtCons[Int], MtCons()).lookup(0) == None )
    assert( Env(Cons(1, MtCons()), Cons(2, MtCons())).lookup(1) == Some(2) )
    assert( m.lookup(1) == Some('a') )
    assert( m.lookup(2) == Some('b') )
    assert( m.lookup(3) == None )

    assert( mt1.prepend(m) == m )
    assert( m.prepend(mt1) == m )
    assert( mt1.prepend(mt1) == mt1 )
    assert( Env(Cons(2, MtCons()), Cons('b', MtCons())).prepend(Env(Cons(1, MtCons()), Cons('a', MtCons()))) == m )

    println("Env tests passed!")

  } 

  //
  // test BasicCoreFortress
  //
  def testBasicCoreFortress() {

    testSubst()
    testSubtype()
    testMtype()
    testMoverride()
    testOkType()
    testTypeof()
    testOkMethodDef()
    testOkTraitDef()
    testOkObjectDef()
    testMbody()
    testEval()
    testOneOwner()

    //Done
    println("BasicCoreFortress tests passed!")
    
  }
  
  // test BasicCoreFortress.subst
  def testSubst() {
    
    val defs1 = """
      trait A end
      trait B end
      object C [\T, S \] () end """

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting subst")

    // delta = { A1 => A, A2 => B }
    //
    val alphas = Cons(TypeVariable("A1"), Cons(TypeVariable("A2"), MtCons()))
    val taus = Cons(typ("A"), Cons(typ("B"), MtCons()))
    val delta = Env(alphas, taus)

    //Testing subst in types
    testeq(subst(delta, typ("A1")),
	   typ("A")) 
	   // [A/a1, B/a2] a1 = A
    testeq(subst(delta, typ("""C [\A1, A2 \]""")),
	   typ("""C [\A, B \]""")) 
	   // [A/a1, B/a2] C[\a1, a2\] = C[\A, B\]
    
    //Testing subst in expressions
    testeq(subst(delta, e("""C [\A1, A2 \]().f [\A1 \]().g [\Object \]().x""")),
	   e("""C [\A, B \]().f [\A \]().g [\Object \]().x"""))

    assert(CFP == cfp1)
    println("subst passed!")
  }

  // test BasicCoreFortress.subtype
  def testSubtype() {

    val defs1 = """
      trait A end
      trait B extends {A} end
      trait C extends {B} end """

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting subtype")

    val mtDelta = Env(MtCons[TypeVariable](), MtCons[N]())
    val edummy = "dummy"
    val p1 = program(defs1 + edummy)

    assert(subtype(typ("A"), typ("Object"), mtDelta, p1)) // A <: Object [S-OBJ]
    assert(subtype(typ("A"), typ("A"), mtDelta, p1)) // A <: A [S-REFL]
    assert(subtype(typ("B"), typ("A"), mtDelta, p1)) // B <: A [S-TAPP]
    assert(! subtype(typ("A"), typ("B"), mtDelta, p1)) // !(A <: B) 
    assert(subtype(typ("C"), typ("A"), mtDelta, p1)) // C <: A [S-TRANS]

    // delta = { A1 => A }
    val delta = Env(Cons(TypeVariable("A1"), MtCons()), Cons(boundTyp("A"), MtCons()))

    assert(subtype(typ("A1"), typ("A"), delta, p1)) // A1 <: A in delta [S-VAR]

    assert(CFP == cfp1)
    println("subtype passed!")
  }

  // test BasicCoreFortress.mtype
  def testMtype() {

    val defs1 = """
      trait A end
      trait B extends {A} f():Object = dummy end
      trait C extends {B} end 
      object D () extends {C} end
      """

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting mtype")

    val mtDelta = Env(MtCons[TypeVariable](), MtCons[N]())
    val edummy = "dummy"
    val p1 = program(defs1 + edummy)

    assert(mtype(MethodName("f"), typ("Object"), p1) == None) // [MT-OBJ]
    assert(mtype(MethodName("f"), typ("A"), p1) == None) //f !in A
    assert(mtype(MethodName("f"), typ("B"), p1) == Some((mtDelta, MtCons(), Object()))) //f():Object in B
    assert(mtype(MethodName("f"), typ("C"), p1) == mtype(MethodName("f"), typ("B"), p1)) //f():Object inherited by trait C
    assert(mtype(MethodName("f"), typ("D"), p1) == mtype(MethodName("f"), typ("B"), p1)) //f():Object inherited by object D

    assert(CFP == cfp1)

    val defs2 = """
      trait A end
      trait B end

      trait List [\T \]
	zip [\S \] (o:List [\S \]):List [\Pair [\T , S \] \] = dummy 
      end
      object Pair [\X, Y \] () end 
      """

    val cfp2 = PreParser.makeCFParser(defs2)

    CFP = cfp2

    assert(CFP == cfp2)
    val p2 = program(defs2 + edummy)

    testeq(mtype(MethodName("zip"), typ("""List [\A \]"""), p2).get, (Env(Cons(TypeVariable("S"), MtCons()), Cons(boundTyp("Object"), MtCons())), Cons(typ("""List [\S \]"""), MtCons()), typ("""List [\Pair [\A, S \] \]""")))

    assert(CFP == cfp2)
    println("mtype passed!")
  }

  // test BasicCoreFortress.moverride
  def testMoverride() {

    val defs1 = """
      trait A f():A = dummy end
      trait B extends {A} end """

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting moverride")

    val mtDelta = Env(MtCons[TypeVariable](), MtCons[N]())
    val edummy = "dummy"
    val p1 = program(defs1 + edummy)

    assert(moverride(MethodName("h"), MtCons(), mtDelta, MtCons(), typ("Object"), mtDelta, p1)) // h():Object ok in class with no supers
    assert(moverride(MethodName("h"), Cons(boundTyp("A"), MtCons()), mtDelta, Cons(typ("Object"), MtCons()), typ("Object"), mtDelta, p1)) //h(x:Object):Object in <: A ok
    assert(moverride(MethodName("f"), Cons(boundTyp("A"), MtCons()), mtDelta, MtCons(), typ("A"), mtDelta, p1)) //f():A in <: A ok
    assert(moverride(MethodName("f"), Cons(boundTyp("B"), MtCons()), mtDelta, MtCons(), typ("A"), mtDelta, p1)) //f():A in <: B ok
    assert(moverride(MethodName("f"), Cons(boundTyp("A"), MtCons()), mtDelta, MtCons(), typ("B"), mtDelta, p1)) //f():B <: A ok
    assert(! moverride(MethodName("f"), Cons(boundTyp("A"), MtCons()), mtDelta, MtCons(), typ("Object"), mtDelta, p1)) //f():Object <: A not ok
    assert(! moverride(MethodName("f"), Cons(boundTyp("A"), MtCons()), mtDelta, Cons(typ("B"), MtCons()), typ("A"), mtDelta, p1)) //f(x:B):A in <: A not ok

    assert(CFP == cfp1)

    val defs2 = """
      trait A g(x:B, y:C):A = dummy end
      trait B end 
      trait C end """

    val cfp2 = PreParser.makeCFParser(defs2)

    CFP = cfp2

    assert(CFP == cfp2)

    val p2 = program(defs2 + edummy)

    assert(moverride(MethodName("g"), Cons(boundTyp("A"), MtCons()), mtDelta, Cons(typ("B"), Cons(typ("C"), MtCons())), typ("A"), mtDelta, p2)) //g(x:B, y:C):A in <: A ok
    assert(! moverride(MethodName("g"), Cons(boundTyp("A"), MtCons()), mtDelta, Cons(typ("B"), MtCons()), typ("A"), mtDelta, p2)) //g(x:B):A in <: A not ok
    assert(! moverride(MethodName("g"), Cons(boundTyp("A"), MtCons()), mtDelta, MtCons(), typ("A"), mtDelta, p2)) //g():A in <: A not ok
    assert(! moverride(MethodName("g"), Cons(boundTyp("A"), MtCons()), mtDelta, Cons(typ("B"), Cons(typ("B"), MtCons())), typ("A"), mtDelta, p2)) //g(x:B, y:B):A in <: A not ok
    assert(! moverride(MethodName("g"), Cons(boundTyp("A"), MtCons()), mtDelta, Cons(typ("C"), Cons(typ("B"), MtCons())), typ("A"), mtDelta, p2)) //g(x:C, y:B):A in <: A not ok

    assert(CFP == cfp2)

    val defs3 = """
      trait A
	f [\A1 extends B \] (): List [\A1 \] = dummy
	g [\A1 extends B \] (x:A1): Object = dummy
      end
      trait B end
      trait List [\T \] end
    """

    val cfp3 = PreParser.makeCFParser(defs3)

    CFP = cfp3

    assert(CFP == cfp3)

    val p3 = program(defs3 + edummy)
    val delta = Env(Cons(TypeVariable("B1"), MtCons()), Cons(boundTyp("B"), MtCons())) // delta = { B1 => B }
    
    assert(moverride(MethodName("f"), Cons(boundTyp("A"), MtCons()), delta, MtCons(), typ("""List [\B1 \]"""), mtDelta, p3)) 
    // f [\B1 extends B \] () List [\B1 \] ok in <: A
    assert(moverride(MethodName("g"), Cons(boundTyp("A"), MtCons()), delta, Cons(typ("B1"), MtCons()), typ("Object"), mtDelta, p3)) 
    // g [\B1 extends B \] (x:B1): Object ok in <: A

    assert(CFP == cfp3)

    println("moverride passed!")
  }

  // test BasicCoreFortress.ok(Type, ...)
  def testOkType() {

    val defs1 = """
      trait A end
      trait B [\T \] end
      trait C [\T extends A \] end 
      object D () extends {A} end """

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting ok (type)")

    // delta = { A1 => A }
    val delta = Env(Cons(TypeVariable("A1"), MtCons()), Cons(boundTyp("A"), MtCons()))
    val edummy = "dummy"
    val p1 = program(defs1 + edummy)

    //[W-OBJ]
    assert(ok(typ("Object"), delta, p1))
    //[W-VAR]
    assert(ok(typ("A1"), delta, p1))
    assert(! ok(typ("A2"), delta, p1))
    //[W-TAPP]
    assert(ok(typ("A"), delta, p1))
    assert(ok(typ("""B [\Object \]"""), delta, p1))
    assert(! ok(typ("B"), delta, p1))
    assert(! ok(typ("""B [\Object, Object \]"""), delta, p1))
    assert(ok(typ("""C [\A \]"""), delta, p1))
    assert(ok(typ("""C [\D \]"""), delta, p1))
    assert(ok(typ("""C [\A1 \]"""), delta, p1))
    assert(! ok(typ("""C [\B \]"""), delta, p1))

    assert(CFP == cfp1)
    println("ok (type) passed!")
  }

  // test BasicCoreFortress.typeof(Expression, ...)
  def testTypeof() {

    val defs1 = """
      trait A 
	a():A = dummy end
      object B () extends {A}
	b(x:B):B = dummy end
      object C (a:A, b:B) extends {A}
	c():C = dummy end 
      object D (a:A, c:C)
	f [\T extends A \]():T = dummy end """

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting typeof")

    // delta = { A1 => A }
    val delta = Env(Cons(TypeVariable("A1"), MtCons()), Cons(boundTyp("A"), MtCons()))
    // gamma = { a => A }
    val gamma = Env(Cons(FieldName("a"), MtCons()), Cons(typ("A"), MtCons()))
    val mtDelta = Env(MtCons[TypeVariable](), MtCons[N]())
    val mtGamma = Env(MtCons[FieldName](), MtCons[Type]())
    val edummy = "dummy"
    val p1 = program(defs1 + edummy)

    //[T-VAR]
    testeq(typeof(e("a"), None, mtGamma, mtDelta, p1), None) //a untypable in empty Gamma
    testeq(typeof(e("a"), None, gamma, mtDelta, p1), Some(typ("A"))) //a:A in gamma
    //[T-SELF]
    testeq(typeof(e("self"), Some(typ("A")), mtGamma, mtDelta, p1), Some(typ("A")))
    testeq(typeof(e("self"), None, mtGamma, mtDelta, p1), None)
    //[T-OBJECT]
    testeq(typeof(e("B()"), None, mtGamma, mtDelta, p1), Some(typ("B"))) //B():B
    testeq(typeof(e("C(a, B())"), None, gamma, mtDelta, p1), Some(typ("C")))
    testeq(typeof(e("C(B(), B())"), None, gamma, mtDelta, p1), Some(typ("C")))
    testeq(typeof(e("C(a, a)"), None, gamma, mtDelta, p1), None)
    //[T-FIELD]
    testeq(typeof(e("C(B(), B()).b"), None, gamma, delta, p1), Some(typ("B")))
    testeq(typeof(e("D(B(), C(B(), B())).a"), None, gamma, delta, p1), Some(typ("A")))
    testeq(typeof(e("D(B(), C(B(), B())).c"), None, gamma, delta, p1), Some(typ("C")))
    testeq(typeof(e("D(C(B(), B()), C(B(), B())).a.b"), None, gamma, delta, p1), None)
    testeq(typeof(e("D(C(B(), B()), C(B(), B())).c.b"), None, gamma, delta, p1), Some(typ("B")))
    //[T-METHOD]
    testeq(typeof(e("a.a()"), None, gamma, delta, p1), Some(typ("A")))
    testeq(typeof(e("B().a()"), None, gamma, delta, p1), Some(typ("A")))
    testeq(typeof(e("B().a(B())"), None, gamma, delta, p1), None)
    testeq(typeof(e("B().b(B())"), None, gamma, delta, p1), Some(typ("B")))
    testeq(typeof(e("B().b(C(B(), B()))"), None, gamma, delta, p1), None)
    testeq(typeof(e("B().b()"), None, gamma, delta, p1), None)
    testeq(typeof(e("C(B(), B()).c()"), None, gamma, delta, p1), Some(typ("C")))
    testeq(typeof(e("C(B(), B()).c().b"), None, gamma, delta, p1), Some(typ("B")))
    testeq(typeof(e("""D(C(B(), B()), C(B(), B())).f [\B \]()"""), None, gamma, delta, p1), Some(typ("B")))

    assert(CFP == cfp1)

    val defs2 = """
      trait A end
      trait B extends {A} end
      object C () extends {A}
	g [\T extends A \](x:T):T = edummy end
      object D () extends {B} end
      object E () end
      """

    val cfp2 = PreParser.makeCFParser(defs2)

    CFP = cfp2

    assert(CFP == cfp2)
    val p2 = program(defs2 + edummy)

    testeq(typeof(e("""C().g [\A \](C())"""), None, gamma, delta, p2), Some(typ("A")))
    testeq(typeof(e("""C().g [\A \](D())"""), None, gamma, delta, p2), Some(typ("A")))
    testeq(typeof(e("""C().g [\B \](D())"""), None, gamma, delta, p2), Some(typ("B")))
    testeq(typeof(e("""C().g [\B \](C())"""), None, gamma, delta, p2), None)
    testeq(typeof(e("""C().g [\Object \](E())"""), None, gamma, delta, p2), None)
    testeq(typeof(e("""C().g [\A \](E())"""), None, gamma, delta, p2), None)
    
    assert(CFP == cfp2)
    println("typeof passed!")
  }

  // test BasicCoreFortress.ok(MethodDef, ...)
  def testOkMethodDef() {

    val defs1 = """
      trait A 
	f():Object = dummy
      end
      trait D extends {A} end
      object B () end 
      object C [\T \] () end """

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting ok (method def)")

    val mtGamma = Env(MtCons[FieldName](), MtCons[Type]())
    val mtDelta = Env(MtCons[TypeVariable](), MtCons[N]())
    val cDelta = Env(Cons(TypeVariable("T"), MtCons()), Cons(boundTyp("Object"), MtCons()))
    val edummy = "dummy"
    val p1 = program(defs1 + edummy)

    assert(ok(mdef("g():Object = B()"), TraitName("A"), Some(typ("A")), mtGamma, mtDelta, p1))
    assert(ok(mdef("g():B = B()"), TraitName("A"), Some(typ("A")), mtGamma, mtDelta, p1))
    assert(! ok(mdef("g():A = B()"), TraitName("A"), Some(typ("A")), mtGamma, mtDelta, p1)) // bad body type
    assert(! ok(mdef("g():Object = dummy"), TraitName("A"), Some(typ("A")), mtGamma, mtDelta, p1)) // untypable body
    assert(! ok(mdef("""g(x:A [\B \]):Object = dummy"""), TraitName("A"), Some(typ("A")), mtGamma, mtDelta, p1)) // ill-formed param type
    assert(ok(mdef("g(x:A):A = x"), TraitName("A"), Some(typ("A")), mtGamma, mtDelta, p1))
    assert(ok(mdef("g():A = self"), TraitName("A"), Some(typ("A")), mtGamma, mtDelta, p1))
    assert(ok(mdef("""g():C [\T \] = self"""), ObjectName("C"), Some(typ("""C [\T \]""")), mtGamma, cDelta, p1))
    assert(ok(mdef("""g [\S \]():C [\S \] = C [\S \]()"""), TraitName("A"), Some(typ("A")), mtGamma, mtDelta, p1)) 

    assert(! ok(mdef("f(x:Object):Object = B()"), TraitName("D"), Some(typ("D")), mtGamma, mtDelta, p1)) // check override

    assert(CFP == cfp1)
    println("ok (method def) passed!")
  }

  // test BasicCoreFortress.ok(TraitDef, ...)
  def testOkTraitDef() {

    val defs1 = """
      trait A end
      trait B extends {A} end
      trait D end
      trait E [\T \] end
      trait F [\T extends Foo \] end
      trait G [\T \] end

      trait Foo 
	foo():Object = self
      end """

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting ok (trait def)")

    val edummy = "dummy"
    val p1 = program(defs1 + edummy)

    assert(ok(tdef("""trait A end"""), p1))

    assert(ok(tdef("""trait B extends {A} end"""), p1))
    assert(! ok(tdef("""trait B extends {A [\B \]} end"""), p1)) // bad super type

    assert(ok(tdef("""trait D f():D = self end"""), p1))
    assert(ok(tdef("""trait E [\T \] f():E [\T \] = self end"""), p1))

    assert(ok(tdef("""trait F [\T extends Foo \] f(x:T):Object = x.foo() end"""), p1))
    assert(! ok(tdef("""trait G [\T \] f(x:T):Object = x.foo() end"""), p1)) // test type parameter

    assert(CFP == cfp1)
    println("ok (trait def) passed!")
  }

  // test BasicCoreFortress.ok(ObjectDef, ...)
  def testOkObjectDef() {

    val defs1 = """
      trait Foo 
	foo():Object = self
      end 

      object A () end
      object B () extends {Foo} end
      object D () end
      object E [\T \]() end
      object F [\T extends Foo \]() end
      object G [\T \]() end 

      object H (x:Foo, y:Foo) end """

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting ok (object def)")

    val edummy = "dummy"
    val p1 = program(defs1 + edummy)

    assert(ok(odef("""object A () end"""), p1))

    assert(ok(odef("""object B () extends {Foo} end"""), p1))
    assert(! ok(odef("""object B () extends {Foo [\B \]} end"""), p1)) // bad super type

    assert(ok(odef("""object D () f():D = self end"""), p1))
    assert(ok(odef("""object E [\T \]() f():E [\T \] = self end"""), p1))

    assert(ok(odef("""object F [\T extends Foo \]() f(x:T):Object = x.foo() end"""), p1))
    assert(! ok(odef("""object G [\T \]() f(x:T):Object = x.foo() end"""), p1)) //test type parameter

    assert(ok(odef("""object H (x:Foo, y:Foo) f():Object = x.foo() end"""), p1))
    assert(! ok(odef("""object H (x:Foo, y:Foo) f():Object = z.foo() end"""), p1))

    assert(CFP == cfp1)
    println("ok (object def) passed!")
  }

  // test BasicCoreFortress.mbody
  def testMbody() {

    val defs1 = """
      trait A
	f():Object = self
      end
      trait B1 extends {A} end
      trait B2 extends {A}
	f():Object = O1(self)
      end
      trait C
	h(x:Object):Object = O1(x)
      end

      object D ()
	f [\T \]():Object = dummy
	g [\S \]():Object = self.f [\S \]()
      end

      object O1(x:Object) end"""

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting mbody")

    val edummy = "dummy"
    val p1 = program(defs1 + edummy)

    testeq(mbody(MethodName("f"), MtCons(), typ("Object"), p1),
	   None)
    testeq(mbody(MethodName("g"), MtCons(), typ("A"), p1),
	   None)
    testeq(mbody(MethodName("f"), MtCons(), typ("A"), p1).get, 
	   (MtCons(), e("self")))
    testeq(mbody(MethodName("f"), MtCons(), typ("B1"), p1).get, 
	   (MtCons(), e("self")))
    testeq(mbody(MethodName("f"), MtCons(), typ("B2"), p1).get, 
	   (MtCons(), e("O1(self)")))
    testeq(mbody(MethodName("h"), MtCons(), typ("C"), p1).get, 
	   (Cons(FieldName("x"), MtCons()), e("O1(x)")))
    testeq(mbody(MethodName("g"), Cons(typ("A"), MtCons()), typ("D"), p1).get, 
	   (MtCons(), e("""self.f [\A \]()""")))

    assert(CFP == cfp1)
    println("mbody passed!")
  }

  // test BasicCoreFortress.eval
  def testEval() {

    val defs1 = """
      trait Num 
	inc ():Num = Succ(self)
      end
      
      object Zero () extends {Num}
	dec ():Num = self
	plus (o:Num):Num = o
      end
      object Succ (n:Num) extends {Num} 
	dec ():Num = n
	plus (o:Num):Num = Succ(n.plus(o))
	
      end """

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting eval")

    val edummy = "dummy"
    val p1 = program(defs1 + edummy)

    testeq(eval(program(defs1 + "Zero().plus(Zero())")).get, v("Zero()"))
    testeq(eval(program(defs1 + "Zero().plus(Succ(Zero()))")).get, v("Succ(Zero())"))
    testeq(eval(program(defs1 + "Succ(Zero()).plus(Succ(Zero()))")).get, v("Succ(Succ(Zero()))"))
    testeq(eval(program(defs1 + "Zero().plus(Succ(Succ(Zero()))).dec()")).get, v("Succ(Zero())")) // test order of method invocation: (0+2)-- = 1
    testeq(eval(program(defs1 + "Zero().dec().plus(Succ(Succ(Zero())))")).get, v("Succ(Succ(Zero()))")) // test order of method invocation: (0--) + 2 = 2

    assert(CFP == cfp1)
    println("eval passed!")
  }

  // test BasicCoreFortress.oneOwner
  def testOneOwner() {

    val edummy = "dummy"

    val defs1 = """
      trait A end 
      """ 
      // A should pass

    val cfp1 = PreParser.makeCFParser(defs1)

    CFP = cfp1

    assert(CFP == cfp1)
    println("\nTesting oneOwner")

    assert(oneOwner(TraitName("A"), program(defs1 + edummy)))

    assert(CFP == cfp1)

    val defs2 = """
      trait A
	f ():Object = dummy
      end
      trait B
	f ():Object = dummy
      end
      trait C extends {A, B}
	f ():Object = dummy2
      end 
      """ 
      // C should pass

    val cfp2 = PreParser.makeCFParser(defs2)

    CFP = cfp2

    assert(CFP == cfp2)

    assert(oneOwner(TraitName("C"), program(defs2 + edummy)))

    assert(CFP == cfp2)

    val defs3 = """
      trait A
	f():Object = dummy
      end
      trait B
	f():Object = dummy
      end
      trait C extends {A, B}
      end
      """
      // C should fail

    val cfp3 = PreParser.makeCFParser(defs3)

    CFP = cfp3

    assert(CFP == cfp3)

    assert(! oneOwner(TraitName("C"), program(defs3 + edummy)))

    assert(CFP == cfp3)

    val defs3b = """
      trait A
	f():Object = dummy
      end
      trait B
	f():Object = dummy
      end
      trait C extends {A, B}
	g():Object = dummy
      end
      """
      // C should fail

    val cfp3b = PreParser.makeCFParser(defs3b)

    CFP = cfp3b

    assert(CFP == cfp3b)

    assert(! oneOwner(TraitName("C"), program(defs3b + edummy)))

    assert(CFP == cfp3b)

    val defs4 = """
      trait AA
	f():Object = dummy
      end
      trait A extends {AA}
      end
      trait B
	f():Object = dummy
      end
      trait C extends {A, B}
      end
      """
      // C should fail

    val cfp4 = PreParser.makeCFParser(defs4)

    CFP = cfp4

    assert(CFP == cfp4)

    assert(! oneOwner(TraitName("C"), program(defs4 + edummy)))

    assert(CFP == cfp4)

    val defs5 = """
      trait A
	f():Object = dummy
      end
      trait B
	f():Object = dummy
      end
      trait C extends {A, B}
      end
      object D () extends {C}
      end
      """
      // D should fail

    val cfp5 = PreParser.makeCFParser(defs5)

    CFP = cfp5

    assert(CFP == cfp5)

    assert(! oneOwner(ObjectName("D"), program(defs5 + edummy)))

    assert(CFP == cfp5)

    val defs6 = """
      trait A
	f():Object = dummy
      end
      trait B
	f():Object = dummy
      end
      trait C extends {A, B}
      end
      object D () extends {C}
	f():Object = dummy
      end
      """
      // D should pass
    
    val cfp6 = PreParser.makeCFParser(defs6)

    CFP = cfp6

    assert(CFP == cfp6)

    assert(oneOwner(ObjectName("D"), program(defs6 + edummy)))

    assert(CFP == cfp6)

    println("oneOwner passed!")
  }



  def testeq(x:Any, y:Any) {
    if (debug && x == y) {
      println("GOOD:   " + x)
      println("     == " + y)
    }
    else {
      println("BAD:    " + x)
      println("     != " + y)
      assert(x == y) // fail on purpose
    }
  }

  //
  // Parser methods
  //

  def typ(s:java.lang.CharSequence):Type = {
    val cfp = CFP
    val r = cfp.parseAll[Type](cfp.typ, s)
    if (r.successful)
      r.get
    else {
      println("Failed to parse type: ")
      println(s + "\n")
      println(r)
      r.get
    }
  }

  def boundTyp(s:java.lang.CharSequence):N = {
    val cfp = CFP
    val r = cfp.parseAll[N](cfp.boundTyp, s)
    if (r.successful)
      r.get
    else {
      println("Failed to parse bound type: ")
      println(s + "\n")
      println(r)
      r.get
    }
  }

  def e(s:java.lang.CharSequence):Expression = {
    val cfp = CFP
    val r = cfp.parseAll[Expression](cfp.expression, s)
    if (r.successful)
      r.get
    else {
      println("Failed to parse expression: ")
      println(s + "\n")
      println(r)
      r.get
    }
  }

  def v(s:java.lang.CharSequence):Value = {
    val cfp = CFP
    val r = cfp.parseAll[Value](cfp.value, s)
    if (r.successful)
      r.get
    else {
      println("Failed to parse value: ")
      println(s + "\n")
      println(r)
      r.get
    }
  }

  def mdef(s:java.lang.CharSequence):MethodDef = {
    val cfp = CFP
    val r = cfp.parseAll[MethodDef](cfp.methodDef, s)
    if (r.successful)
      r.get
    else {
      println("Failed to parse methodDef: ")
      println(s + "\n")
      println(r)
      r.get
    }
  }

  def tdef(s:java.lang.CharSequence):TraitDef = {
    val cfp = CFP
    val r = cfp.parseAll[TraitDef](cfp.traitDef, s)
    if (r.successful)
      r.get
    else {
      println("Failed to parse traitDef: ")
      println(s + "\n")
      println(r)
      r.get
    }
  }

  def odef(s:java.lang.CharSequence):ObjectDef = {
    val cfp = CFP
    val r = cfp.parseAll[ObjectDef](cfp.objectDef, s)
    if (r.successful)
      r.get
    else {
      println("Failed to parse objectDef: ")
      println(s + "\n")
      println(r)
      r.get
    }
  }

  def program(s:java.lang.CharSequence):Program = {
    val cfp = CFP
    val r = cfp.parseAll[Program](cfp.program, s)
    if (r.successful)
      r.get
    else {
      println("Failed to parse program: ")
      println(s + "\n")
      println(r)
      r.get
    }
  }

  /*def parse[T](parser:cfp.Parser[T], s:java.lang.CharSequence):T = {
    val cfp = CFP
    val r = cfp.parseAll[T](parser, s)
    if (r.successful)
      r.get
    else {
      println("Failed to parse: ")
      println(s + "\n")
      println(r)
      r.get
    }
    /*cfp.parseAll[Program](cfp.program, s) match {
      case cfp.Success(r, _) => r
      case cfp.NoSuccess(msg, _) => throw new RuntimeException(msg)
    }*/
  }*/

}
