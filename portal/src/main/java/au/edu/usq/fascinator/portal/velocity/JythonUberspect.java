/* 
 * The Fascinator - Portal
 * Copyright (C) 2008-2009 University of Southern Queensland
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package au.edu.usq.fascinator.portal.velocity;

import java.util.Iterator;

import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.apache.velocity.util.introspection.VelMethod;
import org.apache.velocity.util.introspection.VelPropertyGet;
import org.apache.velocity.util.introspection.VelPropertySet;
import org.python.core.PyBoolean;
import org.python.core.PyDictionary;
import org.python.core.PyInteger;
import org.python.core.PyJavaType;
import org.python.core.PyList;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PyObjectDerived;
import org.python.core.PySequence;
import org.python.core.PyString;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jython/Velocity integration
 * 
 * Based on http://wiki.apache.org/jakarta-velocity/JythonUberspect
 * 
 * @author Oliver Lucido
 */
public class JythonUberspect extends UberspectImpl {

    private static Logger log = LoggerFactory.getLogger(JythonUberspect.class);

    @Override
    @SuppressWarnings("unchecked")
    public Iterator getIterator(Object obj, Info i) throws Exception {
        log.trace("getIterator obj:" + obj + " i:" + i);

        if (obj instanceof PyObject) {
            PyObject pyObject = (PyObject) obj;
            PyType pyType = pyObject.getType();
            if (pyObject instanceof PySequence) {
                return new JythonIterator((PySequence) pyObject);
            } else if (pyObject instanceof PyDictionary) {
                return ((PyDictionary) pyObject).values().iterator();
            } else if (pyType instanceof PyJavaType) {
                Class cls = ((PyJavaType) pyType).getProxyType();
                return super.getIterator(pyObject.__tojava__(cls), i);
            } else {
                log.trace("Unsupported class:{} type:{}", pyObject.getClass(),
                        pyType);
            }
        }
        return super.getIterator(obj, i);
    }

    @Override
    public VelMethod getMethod(Object obj, String methodName, Object[] args,
            Info i) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("getMethod obj:" + obj + " methodName:" + methodName
                    + " i:" + i);
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    log.trace("          arg:" + arg);
                }
            }
        }

        if (obj instanceof PyObject) {
            return new JythonVelMethod(methodName);
        }

        return super.getMethod(obj, methodName, args, i);
    }

    @Override
    public VelPropertyGet getPropertyGet(Object obj, String identifier, Info i)
            throws Exception {
        log.trace("getPropertyGet obj:" + obj + " identifier:" + identifier
                + " i:" + i);

        if (obj instanceof PyObject) {
            return new JythonVelPropertyGet(identifier);
        }

        return super.getPropertyGet(obj, identifier, i);
    }

    @Override
    public VelPropertySet getPropertySet(Object obj, String identifier,
            Object arg, Info i) throws Exception {
        log.trace("getPropertySet obj:" + obj + " identifier:" + identifier
                + " arg:" + arg + " i:" + i);
        return super.getPropertySet(obj, identifier, arg, i);
    }

    @SuppressWarnings("unchecked")
    public static Object toJava(Object obj) {
        log.trace("toJava object:{}", obj);
        if (obj instanceof PyObject) {
            PyObject pyObject = (PyObject) obj;
            if (pyObject instanceof PyNone) {
                return null;
            } else if (pyObject instanceof PyString) {
                return ((PyString) pyObject).asString();
            } else if (pyObject instanceof PyBoolean) {
                return Boolean.parseBoolean(pyObject.toString());
            } else if (pyObject instanceof PyInteger) {
                return Integer.parseInt(pyObject.toString());
            } else if (pyObject instanceof PyUnicode) {
                return new String(((PyUnicode) pyObject).encode("UTF-8"));
            } else if (pyObject instanceof PyObjectDerived) {
                PyType pyType = pyObject.getType();
                Class cls = ((PyJavaType) pyType).getProxyType();
                return ((PyObjectDerived) pyObject).__tojava__(cls);
            } else if (pyObject instanceof PyList
                    || pyObject instanceof PyDictionary) {
                return (pyObject);
            } else {
                log.trace("toJava unhandled type:{}", pyObject.getClass()
                        .getName());
            }
        }
        return obj;
    }
}
