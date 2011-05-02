package au.edu.usq.fascinator.portal.velocity;

import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.util.introspection.VelMethod;
import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JythonVelMethod implements VelMethod {

    private Logger log = LoggerFactory.getLogger(JythonVelMethod.class);

    private String methodName;

    public JythonVelMethod(String methodName) {
        this.methodName = methodName;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Class getReturnType() {
        return Object.class;
    }

    @Override
    public Object invoke(Object o, Object[] params) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("invoke:" + o);
            if (params != null) {
                for (Object param : params) {
                    log.trace("       param:" + param);
                }
            }
        }
        Object retVal = null;
        PyObject pyObject = (PyObject) o;
        PyObject method = pyObject.__findattr__(methodName);
        if (method != null) {
            log.trace("method:" + method);
            if (params == null || params.length < 1) {
                retVal = method.__call__();
            } else {
                List<PyObject> args = new ArrayList<PyObject>();
                for (Object param : params) {
                    if (param == null) {
                        return null;
                    }
                    log.trace("param:" + param + ":" + param.getClass());
                    if (param instanceof String) {
                        args.add(new PyString(param.toString()));
                    } else if (param instanceof Integer) {
                        args.add(new PyInteger(((Integer) param).intValue()));
                    } else if (param instanceof PyObject) {
                        args.add((PyObject) param);
                    } else if (param instanceof List) {
                        args.add(new PyList((List) param));
                    } else {
                        log.trace("Converting param type: {} to PyObject",
                                param.getClass().getName());
                        args.add(Py.java2py(param));
                    }
                }
                retVal = method.__call__(args.toArray(new PyObject[] {}));
            }
        } else {
            log.debug("No such method: {}", methodName);
        }
        return JythonUberspect.toJava(retVal);
    }

    @Override
    public boolean isCacheable() {
        return true;
    }
}
