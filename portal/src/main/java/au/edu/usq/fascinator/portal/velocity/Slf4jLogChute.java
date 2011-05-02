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

import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple SLF4J wrapper for Velocity.
 * 
 * @author Oliver Lucido
 */
public class Slf4jLogChute implements LogChute {

    private Logger logger = LoggerFactory.getLogger(Velocity.class);

    @Override
    public void init(RuntimeServices runtimeServices) throws Exception {
    }

    @Override
    public void log(int level, String message) {
        switch (level) {
        case DEBUG_ID:
            logger.debug(message);
            break;
        case INFO_ID:
            logger.info(message);
            break;
        case WARN_ID:
            logger.warn(message);
            break;
        case ERROR_ID:
            logger.error(message);
            break;
        }
    }

    @Override
    public void log(int level, String message, Throwable throwable) {
        switch (level) {
        case DEBUG_ID:
            logger.debug(message, throwable);
            break;
        case INFO_ID:
            logger.info(message, throwable);
            break;
        case WARN_ID:
            logger.warn(message, throwable);
            break;
        case ERROR_ID:
            logger.error(message, throwable);
            break;
        }
    }

    @Override
    public boolean isLevelEnabled(int level) {
        boolean result = false;
        switch (level) {
        case DEBUG_ID:
            result = logger.isDebugEnabled();
            break;
        case INFO_ID:
            result = logger.isInfoEnabled();
            break;
        case WARN_ID:
            result = logger.isWarnEnabled();
            break;
        case ERROR_ID:
            result = logger.isErrorEnabled();
            break;
        }
        return result;
    }
}
