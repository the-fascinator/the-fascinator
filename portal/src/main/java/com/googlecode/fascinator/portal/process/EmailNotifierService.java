/*******************************************************************************
 * Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
 ******************************************************************************/
package com.googlecode.fascinator.portal.process;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Payload;

/**
 * @author Shilo Banihit
 * 
 */
public interface EmailNotifierService {
    public void email(@Header("from") String from,
            @Header("replyTo") String replyTo, @Header("to") String to,
            @Header("subject") String subject, @Payload String body);

    public void emailAttachment(@Header("from") String from,
            @Header("replyTo") String replyTo, @Header("to") String to,
            @Header("subject") String subject, @Payload String body,
            @Header("attachData") byte[] attachData,
            @Header("attachDataType") String attachDataType,
            @Header("attachFileName") String attachFileName,
            @Header("attachDesc") String attachDesc);
}
