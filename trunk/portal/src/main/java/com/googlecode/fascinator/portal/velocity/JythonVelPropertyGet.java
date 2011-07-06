package com.googlecode.fascinator.portal.velocity;

import org.apache.velocity.util.StringUtils;
import org.apache.velocity.util.introspection.VelPropertyGet;
import org.python.core.PyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JythonVelPropertyGet implements VelPropertyGet {

    private Logger log = LoggerFactory.getLogger(JythonVelPropertyGet.class);

    private String fieldName;

    public JythonVelPropertyGet(String identifier) {
        fieldName = identifier;
    }

    @Override
    public String getMethodName() {
        return "get" + StringUtils.capitalizeFirstLetter(fieldName);
    }

    @Override
    public Object invoke(Object o) throws Exception {
        PyObject pyObject = (PyObject) o;
        Object retVal = pyObject.__findattr__(fieldName);
        if (retVal == null) {
            log.trace("No such attribute: {}, trying {}()...", fieldName,
                    getMethodName());
            JythonVelMethod getMethod = new JythonVelMethod(getMethodName());
            if (getMethod != null) {
                retVal = getMethod.invoke(o, null);
            }
        } else {
            retVal = JythonUberspect.toJava(retVal);
        }
        if (retVal == null) {
            log.debug("No such attribute: {}", fieldName);
        }
        return retVal;
    }

    @Override
    public boolean isCacheable() {
        return true;
    }
}
