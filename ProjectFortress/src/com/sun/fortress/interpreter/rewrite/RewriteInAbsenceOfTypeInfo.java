/*
 * Created on Sep 9, 2007
 *
 */
package com.sun.fortress.interpreter.rewrite;

import java.util.List;

import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes._RewriteFnRef;

import edu.rice.cs.plt.tuple.Option;

public class RewriteInAbsenceOfTypeInfo extends Rewrite {

    public static RewriteInAbsenceOfTypeInfo Only = new RewriteInAbsenceOfTypeInfo();
    
    static Expr translateQualifiedToFieldRef(VarRef vr) {
        QualifiedIdName qidn = vr.getVar();
        if (qidn.getApi().isNone())
            return vr;
        
        List<Id> ids = Option.unwrap(qidn.getApi()).getIds();
        
        return new FieldRef(vr.getSpan(),
                false,
                translateQualifiedToFieldRef(ids), qidn.getName());
    }
    
    static Expr translateFnRef(FnRef fr) {
        List<QualifiedIdName> fns = fr.getFns();
        List<StaticArg> sargs = fr.getStaticArgs();
        QualifiedIdName qidn = fns.get(0);
        if (sargs.size() == 0) {
            // Call it a var or field ref for now.
            if (qidn.getApi().isNone()) {
                return new VarRef(qidn.getSpan(), qidn);
                       
            } else {
                List<Id> ids = Option.unwrap(qidn.getApi()).getIds();
                
                return new FieldRef(fr.getSpan(),
                                        false,
                                        translateQualifiedToFieldRef(ids),
                                        qidn.getName());
            }
        } else {
        if (qidn.getApi().isNone()) {
            return new _RewriteFnRef(fr.getSpan(),
                    false,
                    new VarRef(qidn.getSpan(), qidn), 
                    sargs);
        } else {
            List<Id> ids = Option.unwrap(qidn.getApi()).getIds();
            
            return new _RewriteFnRef(fr.getSpan(),
                        false,
                        new FieldRef(fr.getSpan(),
                                    false,
                                    translateQualifiedToFieldRef(ids),
                                    qidn.getName()),
                        sargs);
        }
        }
    }
    
    static Expr translateQualifiedToFieldRef(List<Id> ids) {
        // id is trailing (perhaps only) id
        Id id = ids.get(ids.size()-1);
    
        if (ids.size() == 1) {
            return new VarRef(id.getSpan(), new QualifiedIdName(id.getSpan(), new IdName(id.getSpan(), id)));

        }
        // TODO fix span -- it needs to cover the whole list.
        return new FieldRef(id.getSpan(),
                false,
                translateQualifiedToFieldRef(ids.subList(0, ids.size()-1)),
                new IdName(id.getSpan(), id)
                );
    }

    @Override
    public AbstractNode visit(AbstractNode node) {
        if (node instanceof VarRef) 
            return translateQualifiedToFieldRef((VarRef)node);
        
        if (node instanceof FnRef) 
         
            return visit(translateFnRef((FnRef)node));
            
            return visitNode(node);
    }

}
