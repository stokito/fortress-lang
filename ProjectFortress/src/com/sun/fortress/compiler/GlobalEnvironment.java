/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler;

import com.sun.fortress.repository.FortressRepository;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.exceptions.FortressException;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.useful.NI;

/**
 * Environment for mapping APINames to ApiIndices.
 * Before looking up an APIName, the client is required to first ensure that
 * the APIName is in the environment. This can be done by calling the
 * definesApi method.
 */
public abstract class GlobalEnvironment {
    abstract public Map<APIName, ApiIndex> apis();

    abstract public boolean definesApi(APIName name);

    abstract public ApiIndex api(APIName name);

    abstract public void print();

    public ApiIndex lookup(APIName name) { return api(name); }

    public String toString() {
        return this.getClass().getSimpleName() + " " + apis();
    }

    public Iterable<Api> apiAsts() {
        Set<Api> result = new HashSet<Api>();

        for (ApiIndex apiIndex : apis().values()) {
            result.add((Api)apiIndex.ast());
        }
        return result;
    }

    public boolean contains(APIName api1, APIName api2) {
        // Degeneratively, every API contains itself.
        if (api1.equals(api2)) {
            return true;
        }
        else {
            for (APIName constituent : api(api1).comprises()) {
                if (contains(constituent, api2)) { return true; }
            }
            return false;
        }
    }

    public static class FromMap extends GlobalEnvironment {
        private Map<APIName, ApiIndex> _apis;

        public FromMap(Map<APIName, ApiIndex> apis) { _apis = apis; }

        public Map<APIName, ApiIndex> apis() { return _apis; }

        public boolean definesApi(APIName name) { return _apis.containsKey(name); }

        public ApiIndex api(APIName name) {
            ApiIndex result = _apis.get(name);
            if (result == null) {
                throw new IllegalArgumentException("Undefined API: " + NodeUtil.nameString(name));
            }
            else { return result; }
        }

        public void print() {
            for (APIName name : apis().keySet()) {
                System.out.println(name);
                System.out.println(apis().get(name));
                System.out.println();
            }
        }
    }

    public static class FromRepository extends GlobalEnvironment {

        final private FortressRepository repository;

        public FromRepository(FortressRepository fr) {
            repository = fr;
        }

        @Override
        public ApiIndex api(APIName name) {

            try {
                return repository.getApi(name);
            } catch (FileNotFoundException e) {
                throw new WrappedException(e);
            } catch (IOException e) {
                throw new WrappedException(e);
            }
        }

        @Override
        public Map<APIName, ApiIndex> apis() {
            return repository.apis();
        }

        @Override
        public boolean definesApi(APIName name) {
            try {
                return null != repository.getApi(name);
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public void print() {
            System.out.println("GlobalEnvironmentFromRepository " + repository);
        }

    }

    public static class FromRepositoryPair extends GlobalEnvironment {

        final private FortressRepository r1, r2;

        /**
         * Tries to use the result from the first repository, but does not try hard.
         * If missing, tries (harder) to use the second repository.
         * @param fr1
         * @param fr2
         */
        public FromRepositoryPair(FortressRepository fr1, FortressRepository fr2) {
            r1 = fr1;
            r2 = fr2;
        }

        @Override
        public ApiIndex api(APIName name) {

            try {
                ApiIndex ai = r1.apis().get(name);
                if (ai != null)
                    return ai;
                return r2.getApi(name);
            } catch (FileNotFoundException e) {
                throw new WrappedException(e);
            } catch (IOException e) {
                throw new WrappedException(e);
            }
        }

        @Override
        public Map<APIName, ApiIndex> apis() {
            Map<APIName, ApiIndex> apis = new HashMap<APIName, ApiIndex>();
            apis.putAll(r1.apis());
            apis.putAll(r2.apis());
            return apis;
        }

        @Override
        public boolean definesApi(APIName name) {
            try {
                return (r1.getApi(name) != null) || (r2.getApi(name) != null);
            } catch (FileNotFoundException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public void print() {
            System.out.println("GlobalEnvironmentFromRepositoryPair " + r1 + ", " + r2);
        }

    }

}
