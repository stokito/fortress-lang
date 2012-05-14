case class MethodName(s:String)
case class FieldName(s:String)
case class TraitName(s:String)
case class ObjectName(s:String)

abstract sealed class Expression
case class ExVar(x:FieldName) extends Expression
case class ExSelf() extends Expression
case class ExObject(O:ObjectName, taus:List[Type], es:List[Expression]) extends Expression
case class ExFieldInvoke(e:Expression, x:FieldName) extends Expression
case class ExMethodInvoke(e:Expression, f:MethodName, taus:List[Type], es:List[Expression]) extends Expression

abstract sealed class Value extends Expression
case class VObject(O:ObjectName, taus:List[Type], vs:List[Value]) extends Value

abstract sealed class Type
abstract sealed class N extends Type
case class TypeVariable(s:String) extends Type
case class TyObject(O:ObjectName, taus:List[Type]) extends Type
case class TyTrait(T:TraitName, taus:List[Type]) extends N
case class Object() extends N

case class MethodDef(f:MethodName, tvs:Env[TypeVariable, N], params:Env[FieldName, Type], r:Type, body:Expression)
case class TraitDef(T:TraitName, tvs:Env[TypeVariable, N], supers:List[N], fds:Env[MethodName, MethodDef])
case class ObjectDef(O:ObjectName, tvs:Env[TypeVariable, N], fields:Env[FieldName, Type], supers:List[N], fds:Env[MethodName, MethodDef])

case class Program(tds:Env[TraitName, TraitDef], ods:Env[ObjectName, ObjectDef], e:Expression)


object BasicCoreFortress {

  def subtype(tau1:Type, tau2:Type, Delta:Env[TypeVariable, N], p:Program):Boolean = 
    if (tau1.equals(tau2)) 
      true
    else 
      (tau1, tau2) match {
	case (_, Object()) => true
	case (alpha:TypeVariable, tau2) => Delta.lookup(alpha) == Some(tau2)
	case (TyTrait(c, targs), tau2) => 
	  p.tds.lookup(c) match {
	    case Some(TraitDef(_, Env(alphas, _), supers, _)) => 
	      supers.exists(subst(Env(alphas, targs), _) == tau2) ||
	      supers.exists(subtype(_, tau2, Delta, p)) //Depth-first search
	    case None => false
	  }
	case (TyObject(c, targs), tau2) => 
	  p.ods.lookup(c) match {
	    case Some(ObjectDef(_, Env(alphas, _), _, supers, _)) =>
	      supers.exists(subst(Env(alphas, targs), _) == tau2) ||
	      supers.exists(subtype(_, tau2, Delta, p)) //Depth-first search
	    case None => false
	  }
	case _ => false
      }

  def bound(tau:Type, Delta:Env[TypeVariable, N]):Option[Type] =
    tau match {
      case TypeVariable(s) => Delta.lookup(TypeVariable(s))
      case nontv => Some(nontv)
    }

  def typeof(p:Program):Option[Type] = {
    val mtGamma = Env(MtCons[FieldName](), MtCons[Type]())
    val mtDelta = Env(MtCons[TypeVariable](), MtCons[N]())
    if (p.tds.values.forall(ok(_, p)) && 
	p.ods.values.forall(ok(_, p))) 
      typeof(p.e, None, mtGamma, mtDelta, p)
    else None
  }

  def typeof(e:Expression, typeofSelf:Option[Type], Gamma:Env[FieldName, Type], Delta:Env[TypeVariable, N], p:Program):Option[Type] = 
    e match {
      case VObject(o, taus, _) => Some(TyObject(o, taus))
      case ExVar(x) => Gamma.lookup(x)
      case ExSelf() => typeofSelf
      case ExObject(o, taus, es) => 
	p.ods.lookup(o) match {
	  case Some(ObjectDef(_, Env(alphas, bounds), fields @ Env(_, taups), supers, fds)) => 
	    if (ok(TyObject(o, taus), Delta, p) &&
		(try {
		  es.zip(taups).forall( 
		    { case (e, taup) =>
		      typeof(e, typeofSelf, Gamma, Delta, p) match {
			case Some(taupp) => subtype(taupp,
						    subst(Env(alphas, taus), taup),
						    Delta, p)
			case None => false
		      }
		   } )
		} catch {
		  case e: UnevenZip => false
		} ))
	      Some(TyObject(o, taus))
	    else None
	  case None => None
	}
      case ExFieldInvoke(e, x) => 
	typeof(e, typeofSelf, Gamma, Delta, p) match {
	  case Some(tau0) => 
	    tau0 match {
	      case TyObject(o, taups) => 
		p.ods.lookup(o) match {
		  case Some(ObjectDef(_, Env(alphas, bounds), fields, _, _)) =>
		    fields.lookup(x) match {
		      case Some(tau) => Some(subst(Env(alphas, taups), tau))
		      case None => None
		    }
		  case None => None
		}
	      case _ => None
	    }
	  case None => None
	}
      case ExMethodInvoke(e0, f, targs, es) =>
	typeof(e0, typeofSelf, Gamma, Delta, p) match {
	  case Some(tau0) => 
	    bound(tau0, Delta) match {
	      case Some(bound) => 
		mtype(f, bound, p) match {
		  case Some((Env(alphas, bounds), taups, tau0p)) => 
		    val sub = Env(alphas, targs)
		    if (targs.forall(ok(_, Delta, p)) &&
			(try {
			  targs.zip(bounds).forall( 
			    { case (tau, n) => subtype(tau, 
						       subst(sub, n), 
						       Delta, p) } ) && //substitution necessary?
			  es.zip(taups).forall( 
			    { case (e,taup) => 
			      typeof(e, typeofSelf, Gamma, Delta, p) match {
				case Some(taupp) => subtype(taupp, 
							    subst(sub, taup), 
							    Delta, p)
				case None => false
			      } } )
			} catch {
			  case e: UnevenZip => false
			}) )
		      Some(subst(sub, tau0p))
		    else None
		  case None => None
		}
	      case None => None
	    }
	   case None => None
	 }
     }

  // moverride: can a method with given signature be declared in a class with given supers? 
  def moverride(f:MethodName, supers:List[N], tvs:Env[TypeVariable, N], taus:List[Type], tau0:Type, Delta:Env[TypeVariable, N], p:Program):Boolean = 
    supers.forall( 
      mtype(f, _, p) match {
	case Some((Env(betas, boundsM), taups, tau0p)) => {
	  val alphas = tvs.keys
	  val boundsN = tvs.values
	  val sub = Env(betas, alphas)
	  try {
	    boundsN.zip(boundsM).forall( { case (n, m) => n == subst(sub, m) } ) &&
	    taus.zip(taups).forall( { case (tau, taup) => tau == subst(sub, taup) } ) &&
	    subtype(tau0, subst(sub, tau0p), Env(alphas, boundsN), p)
	  } catch {
	    case e: UnevenZip => false
	  }
	}
	case None => true
      } )

  // mtype: what is type of the method with name f in class tau?
  def mtype(f:MethodName, tau:Type, p:Program):Option[(Env[TypeVariable, N], List[Type], Type)] = 
    tau match {
      case Object() => None
      case TyTrait(c, targs) => 
	p.tds.lookup(c) match {
	  case Some(TraitDef(_, Env(alphas, _), supers, fds)) => 
	    val sub = Env(alphas, targs)
	    fds.lookup(f) match {
	      //[MT-SELF]
	      case Some(MethodDef(_, Env(betas, bounds), params, r, body)) => 
		Some((Env(betas, bounds.map(substN(sub, _))), 
		      params.values.map(subst(sub, _)), subst(sub, r)))
	      //[MT-SUPER]
	      case None => supers.foldr(None,
					( (z:Option[(Env[TypeVariable, N], List[Type], Type)], N:N) => z match {
					  case Some(thing) => z
					  case None => mtype(f, subst(sub, N), p)
					} ))
	    }
	  case None => None
	}
      case TyObject(c, targs) => 
	p.ods.lookup(c) match {
	  case Some(ObjectDef(_, Env(alphas, _), _, supers, fds)) => 
	    val sub = Env(alphas, targs)
	    fds.lookup(f) match {
	      //[MT-SELF]
	      case Some(MethodDef(_, Env(betas, bounds), params, r, body)) => 
		Some((Env(betas, bounds.map(substN(sub, _))), 
		      params.values.map(subst(sub, _)), subst(sub, r)))
	      //[MT-SUPER]
	      case None => supers.foldr(None,
					( (z:Option[(Env[TypeVariable, N], List[Type], Type)], N:N) => z match {
					  case Some(thing) => z
					  case None => mtype(f, subst(sub, N), p)
					} ))
	    }
	  case None => None
	}
      case _ => None
    }

  def ok(tau:Type, Delta:Env[TypeVariable, N], p:Program):Boolean =
    tau match {
      case Object() => true
      case TypeVariable(s) => Delta.keys.exists(_ == tau)
      case TyTrait(c, targs) => 
	p.tds.lookup(c) match {
	  case Some(TraitDef(_, Env(alphas, bounds), _, _)) => 
	    targs.forall( ok(_, Delta, p) ) && {
	      try {
		targs.zip(bounds).forall( 
		  { case (tau1, tau2) => 
		    subtype(tau1, 
			    subst(Env(alphas, targs), tau2), 
			    Delta, p) } )
	      } catch {
		case e: UnevenZip => false
	      } 
	    }
	  case None => false
      }
      case TyObject(c, targs) => 
	p.ods.lookup(c) match {
	  case Some(ObjectDef(_, Env(alphas, bounds), _, _, _)) => 
	    targs.forall( ok(_, Delta, p) ) && {
	      try {
		targs.zip(bounds).forall( 
		  { case (tau1, tau2) => 
		    subtype(tau1, 
			    subst(Env(alphas, targs), tau2), 
			    Delta, p) } ) // subst necessary?
	      } catch {
		case e: UnevenZip => false
	      } 
	    }
	  case None => false
	}
      case _ => false
    }

  def ok(fd:MethodDef, C:TraitName, typeofSelf:Option[Type], Gamma:Env[FieldName, Type], Delta:Env[TypeVariable, N], p:Program):Boolean =
    fd match {
      case MethodDef(f, tvs @ Env(alphas, bounds), params @ Env(xs, taus), r, e) => 
	val Deltap = Delta.prepend(tvs)
	p.tds.lookup(C) match {
	  case Some(TraitDef(_, _, supers, _)) =>
	    ( moverride(f, supers, tvs, taus, r, Delta, p) && 
	       bounds.forall(ok(_, Deltap, p)) && 
	       taus.forall(ok(_, Deltap, p)) && 
	       ok(r, Deltap, p) && 
	       (typeof(e, typeofSelf, Gamma.prepend(params), Deltap, p) match {
		 case Some(taup) => subtype(taup, r, Deltap, p)
		 case None => false
	     }) )
	  case None => false
	}
    }

  def ok(fd:MethodDef, C:ObjectName, typeofSelf:Option[Type], Gamma:Env[FieldName, Type], Delta:Env[TypeVariable, N], p:Program):Boolean =
    fd match {
      case MethodDef(f, tvs @ Env(alphas, bounds), params @ Env(xs, taus), r, e) => 
	val Deltap = Delta.prepend(tvs)
	p.ods.lookup(C) match {
	  case Some(ObjectDef(_, _, _, supers, _)) =>
	    ( moverride(f, supers, tvs, taus, r, Delta, p) && 
	       bounds.forall(ok(_, Deltap, p)) && 
	       taus.forall(ok(_, Deltap, p)) && 
	       ok(r, Deltap, p) && 
	       (typeof(e, typeofSelf, Gamma.prepend(params), Deltap, p) match {
		 case Some(taup) => subtype(taup, r, Deltap, p)
		 case None => false
	       }) )
	  case None => false
	}
    }

  def ok(td:TraitDef, p:Program):Boolean = {
    val mtGamma = Env(MtCons[FieldName](), MtCons[Type]())
    td match {
      case TraitDef(c, delta @ Env(alphas, bounds), supers, fds) =>
	( bounds.forall( ok(_, delta, p) ) && 
	   supers.forall( ok(_, delta, p) ) && 
	   fds.values.forall( ok(_, c, Some(TyTrait(c, alphas)), mtGamma, delta, p) ) && 
	   oneOwner(c, p) )
      
    }
  }

  def ok(od:ObjectDef, p:Program):Boolean =
    od match {
      case ObjectDef(c, delta @ Env(alphas, bounds), fields @ Env(xs, taus), supers, fds) => 
	( bounds.forall( ok(_, delta, p) ) && 
 	   taus.forall( ok(_, delta, p) ) && 
	   supers.forall( ok(_, delta, p) ) && 
	   fds.values.forall( ok(_, c, Some(TyObject(c, alphas)), fields, delta, p) ) && 
	   oneOwner(c, p) )
    }

  def oneOwner(C:TraitName, p:Program):Boolean = 
    visible(C, p).foldl((MtCons(), true), 
			(seen:(List[MethodName], Boolean), f:MethodName) =>
			  // if either already found a double or if this one is a double, then we are done searching
			  if(!seen._2 || seen._1.exists(_ == f)) 
			    (seen._1, false)
			  else
			    (Cons(f, seen._1), true))._2
			  
				    

  def oneOwner(C:ObjectName, p:Program):Boolean = 
    visible(C, p).foldl((MtCons(), true), 
			(seen:(List[MethodName], Boolean), f:MethodName) =>
			  // if either already found a double or if this one is a double, then we are done searching
			  if(!seen._2 || seen._1.exists(_ == f)) 
			    (seen._1, false)
			  else
			    (Cons(f, seen._1), true))._2

  def visible(C:TraitName, p:Program):List[MethodName] =
    p.tds.lookup(C) match {
      case Some(TraitDef(_, _, supers, Env(fnames, _))) =>
	supers.foldl(MtCons(), 
		    (z:List[MethodName], N:N) =>
		      N match {
			case TyTrait(c, _) =>
			  z.prepend(visible(c, p).filter(f => !fnames.exists(_ == f)))
			case Object() => z
		      } ).prepend(fnames)
      case None => throw new RuntimeException(C + " not found in tds")
    }

  def visible(C:ObjectName, p:Program):List[MethodName] =
    p.ods.lookup(C) match {
      case Some(ObjectDef(_, _, _, supers, Env(fnames, _))) =>
	supers.foldl(MtCons(), (z:List[MethodName], N:N) =>
				N match {
				  case TyTrait(c, _) =>
				    z.prepend(visible(c, p).filter(f => !(fnames.exists(_ == f))))
				  case Object() => z
				}).prepend(fnames)
      case None => throw new RuntimeException(C + " not found in tds")
    }
	 
  // recursively make substitutions of env.values for env.keys in tau
  def subst(sub:Env[TypeVariable, Type], tau:Type):Type = 
    tau match {
      case TyTrait(c, taus) => TyTrait(c, taus.map( subst(sub, _) ))
      case TyObject(c, taus) => TyObject(c, taus.map( subst(sub, _) ))
      case Object() => Object()
      case TypeVariable(s) =>
	sub.lookup(TypeVariable(s)) match {
	  case Some(taup) => taup
	  case None => tau
	}
    }

  def substN(sub:Env[TypeVariable, Type], tau:N):N = 
    tau match {
      case TyTrait(c, taus) => TyTrait(c, taus.map( subst(sub, _) ))
      case Object() => Object()
    }

  def subst(sub:Env[TypeVariable, Type], expr:Expression):Expression = 
    expr match {
      case v:Value => v
      case ExVar(x) => ExVar(x)
      case ExSelf() => ExSelf()
      case ExObject(o, taus, es) => ExObject(o, 
					     taus.map(subst(sub, _)), 
					     es.map(subst(sub, _)))
      case ExFieldInvoke(e, x) => ExFieldInvoke(subst(sub, e), x)
      case ExMethodInvoke(e, f, taus, es) => 
	ExMethodInvoke(subst(sub, e),
		       f,
		       taus.map(subst(sub, _)),
		       es.map(subst(sub, _)))
    }

  // mbody - lookup body of method with name f in class tau, given type parameters to the method taups
  // Assumes conflicting inherited methods are overriden (i.e. oneOwner(tau,p))
  def mbody(f:MethodName, taups:List[Type], tau:Type, p:Program):Option[(List[FieldName], Expression)] =
    tau match {
      case Object() => None
      case TyTrait(c, taus) => 
	p.tds.lookup(c) match {
	  case Some(TraitDef(_, Env(alphas, _), supers, fds)) =>
	    fds.lookup(f) match {
	      //[MB-SELF]
	      case Some(MethodDef(_, Env(alphaps, _), Env(xps, _), _, body)) => 
		Some( (xps, 
		       subst(Env(alphaps, taups), 
			     subst(Env(alphas, taus),
				   body))) )
	      //[MB-SUPER]
	      case None =>
		supers.foldl(None,
			    ( (z:Option[(List[FieldName], Expression)], N) => 
			      z match {
				case Some(thing) => z
				case None => mbody(f, 
						   taus, 
						   subst(Env(alphas, taus), N),
						   p)
			      } ))
	    }
	  case None => None
	}
      case TyObject(c, taus) => 
	p.ods.lookup(c) match {
	  case Some(ObjectDef(_, Env(alphas, _), _, supers, fds)) =>
	    fds.lookup(f) match {
	      //[MB-SELF]
	      case Some(MethodDef(_, Env(alphaps, _), Env(xps, _), _, body)) => 
		Some( (xps, 
		       subst(Env(alphaps, taups), 
			     subst(Env(alphas, taus), 
				   body))) )
	      //[MB-SUPER]
	      case None =>
		supers.foldl(None,
			    ( (z:Option[(List[FieldName], Expression)], N) => 
			      z match {
				case Some(thing) => z
				case None => mbody(f, 
						   taus, 
						   subst(Env(alphas, taus), N),
						   p)
			      } ))
	    }
	  case None => None
	}
      case _ => None
    }

  def eval(p:Program):Option[Value] = 
    eval(p.e, Env(MtCons[FieldName](), MtCons[Value]()), None, p)

  def eval(e:Expression, env:Env[FieldName, Value], self:Option[Value], p:Program):Option[Value] =
    e match {
      case ExVar(x) => env.lookup(x)
      case ExSelf() => self
      case ExObject(o, taus, es) => 
	val vs = es.map( eval(_, env, self, p) )
	if (vs.forall(
	      _ match {
		case Some(thing) => true
		case None => false
	      } ))
	  Some(VObject(o, taus, vs.map(_.get)))
	else None
      case ExFieldInvoke(VObject(o, taus, vs), x) =>
	p.ods.lookup(o) match {
	  case Some(ObjectDef(_, _, fields @ Env(xs, taus), _, _)) => Env(xs, vs).lookup(x)
	  case None => None
	}
      case ExFieldInvoke(e, x) => 
	eval(e, env, self, p) match {
	  case Some(v) => eval(ExFieldInvoke(v, x), env, self, p)
	  case None => None
	}
      case ExMethodInvoke(selfp @ VObject(o, taus, vs), f, taups, vps:List[Value]) =>
	p.ods.lookup(o) match {
	  case Some(ObjectDef(_, _, Env(xs, _), _, _)) =>
	    mbody(f, taups, TyObject(o, taus), p) match {
	      case Some((xps, e)) => eval(e, env.prepend(Env(xs, vs)).prepend(Env(xps, vps)), Some(selfp), p)
	      case None => None
	    }
	  case None => None
	}
      case ExMethodInvoke(e, f, taus, es) =>
	eval(e, env, self, p) match {
	  case Some(v:Value) => {
	    val vs = es.map( eval(_, env, self, p) )
	    if (vs.forall(
	      _ match {
		case Some(thing) => true
		case None => false
	      } ))
	      eval(ExMethodInvoke(v, f, taus, vs.map(_.get)), env, self, p)
	    else None
	  }
	  case None => None
	}
      case _ => None
    }
}

// Generic cons list
sealed abstract class List[+T] {	     
  def map[S](f:T => S):List[S]
  def zip[S](other:List[S]):List[(T,S)]
  def foldr[Z](z:Z, f:(Z,T)=>Z):Z
  def foldl[Z](z:Z, f:(Z,T)=>Z):Z
  def exists(p:T => Boolean):Boolean = foldr(false,
					     ( (z:Boolean, x:T) =>
					       if (z) true
					       else p(x) ))
  def forall(p:T => Boolean):Boolean
  def prepend[U >: T](other:List[U]):List[U] = other.foldl(this,
							   ( (z:List[U], x:U) => Cons(x, z) ))
  def filter(p:T => Boolean):List[T]
  def toScalaList():scala.collection.immutable.List[T]
}

class UnevenZip extends IllegalArgumentException

case class Cons[+T](x1:T, xrest:List[T]) extends List[T] {
  def map[S](f:T => S):List[S] = Cons[S](f(x1), xrest.map[S](f))
  def zip[S](other:List[S]):List[(T,S)] = 
    other match {
      case Cons(y1, yrest) => Cons[(T,S)]( (x1,y1), xrest.zip(yrest) )
      case MtCons() => throw new UnevenZip //
    }
  def foldr[Z](z:Z, f:(Z,T)=>Z):Z = xrest.foldr(f(z, x1), f)
  def foldl[Z](z:Z, f:(Z,T)=>Z):Z = f(xrest.foldl(z, f), x1)
  def forall(p:T => Boolean):Boolean = p(x1) && xrest.forall(p)
  def filter(p:T => Boolean):List[T] = if (p(x1)) 
					 Cons(x1, xrest.filter(p))
				       else
					 xrest.filter(p)
  def toScalaList():scala.collection.immutable.List[T] = x1 :: xrest.toScalaList()
}
case class MtCons[+T]() extends List[T]{
  def map[S](f:T => S):List[S] = MtCons[S]
  def zip[S](other:List[S]):List[(T,S)] = 
    other match {
      case Cons(_,_) => throw new UnevenZip
      case MtCons() => MtCons[(T,S)]
    }
  def foldr[Z](z:Z, f:(Z,T)=>Z):Z = z
  def foldl[Z](z:Z, f:(Z,T)=>Z):Z = z
  def forall(p:T => Boolean):Boolean = true
  def filter(p:T => Boolean):List[T] = MtCons[T]()
  def toScalaList():scala.collection.immutable.List[T] = scala.collection.immutable.Nil
}

// Generic Environment datastructure
// currently allows construct uneven environments
case class Env[A,+B](keys:List[A], values:List[B]) {
  def prepend[C >: B](a:A, c:C):Env[A, C] = Env(Cons(a, keys), Cons(c, values)) //unused?
  def prepend[C >: B](other:Env[A,C]):Env[A,C] = Env(keys.prepend(other.keys), 
					     values.prepend(other.values))
  def lookup(x:A):Option[B] = (keys, values) match {
    case (Cons(x1, xrest), Cons(v1, vrest)) => 
      if (x1 == x) Some(v1)
      else Env(xrest, vrest).lookup(x)
    case _ => None
  }
}

object Env {
  def mt[A,B] = Env[A,B](MtCons[A](), MtCons[B]())
}
