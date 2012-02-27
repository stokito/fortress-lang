import BasicCoreFortress._
import scala.util.parsing.combinator._

// This is a parser for the Basic Core Fortress

// Rules to keep parser happy:
//
// a space is required before the opening bracket for static args.
// Example: """A [\\]""" OK, """A[\\]""" Bad

// a space is required before the closing bracket in nonempty static args
// Example: """[\T extends A \]""" OK, """[\T extends A\]""" Bad
//
// All trait defs must precede all object defs in a program
//
// object and method declarations and instantiations must always have (dynamic) params/args in parentheses.
// But static params, supertraits are optional


// PreParser parses out the object and trait namespaces

object PreParser extends RegexParsers {

  val id: Parser[String] = """[a-zA-z0-9]+""".r

  def nontraits = rep(regex("""(t(?!rait\s)|[^t])\S*""".r))
  def nonobjects = rep(regex("""(o(?!bject\s)|[^o])\S*""".r))

  def preparseT: Parser[scala.List[TraitName]] = nontraits~>repsep("trait"~> traitName, nontraits)<~nontraits

  def preparseO: Parser[scala.List[ObjectName]] = nonobjects~>repsep("object"~> objectName, nonobjects)<~nonobjects
    
  def traitName: Parser[TraitName] = id ^^ { s => TraitName(s) }

  def objectName: Parser[ObjectName] = id ^^ { s => ObjectName(s) }

  def makeCFParser(s:java.lang.CharSequence):CFParser = 
    new CFParser( parseAll(preparseT, s) match {
                    case Success(traits, _) => traits
		    case no:NoSuccess => throw new RuntimeException("Failed to preparse traits\n" + no)
		  },
		  parseAll(preparseO, s) match {
		    case Success(objects, _) => objects
		    case no:NoSuccess => throw new RuntimeException("Failed to preparse objects\n" + no)
		  })
}


// p ::= { td } { od } e
// td ::= "trait" tn ("[\" { tv ("extends" bound)? } "\]")? ("extends {" { bound } "}")? { fd } "end"
// od ::= "object" on ("[\" { tv ("extends" bound)? } "\]")? "(" { x ":" type } ")" ("extends {" { bound } "}")? { fd } "end"
// fd ::= f ("[\" { tv ("extends" bound)? } "\]")? "(" { x ":" type } "):" type "=" e
// e ::= x | "self" | on ("[\" { type } "\]")? "(" { e } ")" | e "." x | e "." f ("[\" { type } "\]")? "(" { e } ")"
// bound ::= tn ("[\" { type } "\]")? | "Object"
// type ::= tv | bound | on ("[\" { type } "\]")?
// td, od, tv, x, f ::= id

class CFParser(val traits:scala.List[TraitName], val objects:scala.List[ObjectName]) extends RegexParsers {

  val id: Parser[String] = """[a-zA-z0-9]+""".r

  def program: Parser[Program] = rep(traitDef)~rep(objectDef)~expression ^^ { case tds~ods~e => Program(Env(toList(tds).map(_.T), toList(tds)), Env(toList(ods).map(_.O), toList(ods)), e) }

  def traitDef: Parser[TraitDef] = "trait"~> traitName~staticParams~extendsClause~rep(methodDef) <~"end" ^^ { case tn~tvs~supers~fds => TraitDef(tn, tvs, supers, Env(toList(fds).map(_.f), toList(fds))) }

  def objectDef: Parser[ObjectDef] = "object"~> objectName~staticParams~params~extendsClause~rep(methodDef) <~"end" ^^ { case on~tvs~params~supers~fds => ObjectDef(on, tvs, params, supers, Env(toList(fds).map(_.f), toList(fds))) }

  def methodDef: Parser[MethodDef] = methodName~staticParams~params~":"~typ~"="~expression ^^ { case f~tvs~params~":"~r~"="~body => MethodDef(f, tvs, params, r, body) }

  def expression: Parser[Expression] = 
    eReceiver~opt("."~>repsep(eInvocation,".")) ^^
      { case head~tail => 
	tail match {
	  case Some(invokes) =>
	    invokes.foldLeft(head)( (z, x) => x match {
	      case ExMethodInvoke(_, f, taus, es) => ExMethodInvoke(z, f, taus, es)
	      case ExFieldInvoke(_, x) => ExFieldInvoke(z, x)
	    })
	  case None => head
	}
      } 

  def eReceiver: Parser[Expression] =
    ( "self" ^^ { case _ => ExSelf() }
     | id~staticArgs~dArgs ^^ { case n~tvs~es => if (objects.contains(ObjectName(n))) 
						     ExObject(ObjectName(n), tvs, es) 
						 else 
						     throw new RuntimeException(n + " is not the name of an object")
					   }
     | fieldName ^^ { case x => ExVar(x) } )

  def eInvocation = {
    val edummy = ExSelf()
    ( methodName~staticArgs~dArgs ^^ { case f~taus~es => ExMethodInvoke(edummy, f, taus, es) }
     | fieldName ^^ { ExFieldInvoke(edummy, _) } )
  }
  
  def value: Parser[Value] = 
    objectName~staticArgs~vArgs ^^ 
      { case on~tvs~vs => VObject(on, tvs, vs) }
    
  def boundTyp: Parser[N] = 
    ( "Object" ^^ { case _ => Object() } 
     | traitName~staticArgs ^^ { case tn~tvs => TyTrait(tn, tvs) } )

  def typ: Parser[Type] = 
    ( "Object" ^^ { case _ => Object() } 
     | id~staticArgs ^^ { case c~tvs => if (traits.contains(TraitName(c))) TyTrait(TraitName(c), tvs) 
					else if (objects.contains(ObjectName(c))) TyObject(ObjectName(c), tvs) 
					else TypeVariable(c) } ) // should error if tvs nonempty?

  def typeVar: Parser[TypeVariable] = id ^^ { s => TypeVariable(s) }


  def staticParams: Parser[Env[TypeVariable, N]] = opt("[\\"~> repsep(typeVar~opt("extends"~boundTyp), ",") <~"\\]") ^^ 
    { _ match {
      case Some(sps) => 
	sps.foldRight(Env(MtCons[TypeVariable](), MtCons[N]())) ( (sp:TypeVariable~Option[String~N], z:Env[TypeVariable, N] ) => 
	  sp match { 
	    case a~Some("extends"~n) => Env(Cons(a, z.keys), Cons(n, z.values))
	    case a~None => Env(Cons(a, z.keys), Cons(Object(), z.values))
	  } )
      case None => Env(MtCons(), MtCons())
    } }

  def extendsClause: Parser[List[N]] = opt("extends {"~> repsep(boundTyp, ",") <~"}") ^^ 
    { _ match {
      case Some(supers) => toList(supers)
      case None => MtCons() 
    } }

  def params: Parser[Env[FieldName, Type]] = "("~> repsep(fieldName~":"~typ, ",") <~")" ^^ 
    { _.foldRight(Env(MtCons[FieldName](), MtCons[Type]())) ( (param, z:Env[FieldName, Type]) => 
	param match {
	  case x~":"~typ => Env(Cons(x, z.keys), Cons(typ, z.values))
	} ) 
   }

  def staticArgs: Parser[List[Type]] = opt("[\\"~> repsep(typ, ",") <~"\\]") ^^ 
    { _ match {
      case Some(taus) => toList(taus)
      case None => MtCons()
    } }

  def dArgs: Parser[List[Expression]] = "("~> repsep(expression, ",") <~")" ^^ { toList(_) }

  def vArgs: Parser[List[Value]] = "("~> repsep(value, ",") <~")" ^^ { toList(_) }
		
    
  def traitName: Parser[TraitName] = id ^^ { s => if (traits.contains(TraitName(s))) TraitName(s) 
						  else throw new RuntimeException(s+" is not the name of a trait") }

  def objectName: Parser[ObjectName] = id ^^ { s => if (objects.contains(ObjectName(s))) ObjectName(s)
						    else throw new RuntimeException(s+" is not the name of an object") }

  def fieldName: Parser[FieldName] = id ^^ { s => FieldName(s) }

  def methodName: Parser[MethodName] = id ^^ { s => MethodName(s) }

  
  def toList[T](slist: scala.collection.immutable.List[T]):List[T] =
    slist.foldRight[List[T]](MtCons())( (x:T, z:List[T]) => Cons(x, z) )
}

object UnParser {

  def unparse(f:MethodName):String = f.s
  def unparse(x:FieldName):String = x.s
  def unparse(T:TraitName):String = T.s
  def unparse(O:ObjectName):String = O.s

  def unparse(expr:Expression):String =
    expr match {
      case ExVar(x) => unparse(x)
      case ExSelf() => "self"
      case ExObject(c, taus, es) => unparse(c) + unparseSargs(taus) + unparseDargs(es)
      case ExFieldInvoke(e, x) => unparse(e) + "." + unparse(x)
      case ExMethodInvoke(e, f, taus, es) => unparse(e) + "." + unparse(f) + unparseSargs(taus) + unparseDargs(es)
      case VObject(c, taus, vs) => unparse(c) + unparseSargs(taus) + unparseDargs(vs)
    }

  def unparse(ty:Type):String =
    ty match {
      case TypeVariable(s) => s
      case TyObject(c, taus) => unparse(c) + unparseSargs(taus)
      case TyTrait(c, taus) => unparse(c) + unparseSargs(taus)
      case Object() => "Object"
    }

  def unparse(fd:MethodDef):String =
    fd match {
      case MethodDef(f, tvs, params, r, body) =>
	unparse(f) + unparseSparams(tvs) + unparseDparams(params) + ":" + unparse(r) + " = " + unparse(body)
    }

  def unparseFds(fds:Env[MethodName, MethodDef]):String =
    fds.values.foldr("", (z:String, fd:MethodDef) => z + "  " + unparse(fd) + "\n")

  def unparse(td:TraitDef):String =
    td match {
      case TraitDef(c, tvs, supers, fds) =>
	"trait " + unparse(c) + unparseSparams(tvs) + " " + unparseSupers(supers) + "\n" + unparseFds(fds) + "end\n"
    } 

  def unparseTds(tds:Env[TraitName, TraitDef]):String =
    tds.values.foldr("", (z:String, td:TraitDef) => z + unparse(td) + "\n")

  def unparse(od:ObjectDef):String =
    od match {
      case ObjectDef(c, tvs, fields, supers, fds) =>
	"object " + unparse(c) + unparseSparams(tvs) + unparseDparams(fields) + " " + unparseSupers(supers) + "\n" + unparseFds(fds) + "end\n"
    } 

  def unparseOds(ods:Env[ObjectName, ObjectDef]):String =
    ods.values.foldr("", (z:String, od:ObjectDef) => z + unparse(od) + "\n")


  def unparseSargs(sargs:List[Type]):String =
    sargs match {
      case nonempty:Cons[_] =>
	" [\\" + nonempty.foldr("", (z:String, ty:Type) => z + unparse(ty) + ", " ).dropRight(2) + " \\]"
      case MtCons() => ""
    }

  def unparseDargs(dargs:List[Expression]):String =
    "(" + dargs.foldr("", (z:String, e:Expression) => z + unparse(e) + ", " ).dropRight(2) + ")"

  def unparseSparams(sparams:Env[TypeVariable, N]):String =
    sparams.keys.zip(sparams.values) match {
      
      case nonempty:Cons[_] =>
	" [\\" + nonempty.foldr("", (z:String, pair:(TypeVariable, N)) => z + unparse(pair._1) + " extends " + unparse(pair._2) + ", " ).dropRight(2) + " \\]"
      case MtCons() => ""
    }

  def unparseDparams(dparams:Env[FieldName, Type]):String =
    "(" + dparams.keys.zip(dparams.values).foldr("", (z:String, pair:(FieldName, Type)) => z + unparse(pair._1) + ":" + unparse(pair._2) + ", " ).dropRight(2) + ")"

  def unparseSupers(supers:List[N]):String =
    supers match {
      case nonempty:Cons[_] =>
	"extends {" + nonempty.foldr("", (z:String, n:N) => z + unparse(n) + ", " ).dropRight(2) + "}"
      case MtCons() => ""
    }


}
