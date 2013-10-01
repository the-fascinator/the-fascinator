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

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Payload;
import org.springframework.integration.annotation.Transformer;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * @author Shilo Banihit
 * 
 */
public class EmailNotifierTransformer {

    private static Logger log = LoggerFactory
            .getLogger(EmailNotifierTransformer.class);

    @Autowired
    JavaMailSender notify_mailSender;

    @Transformer
    public SimpleMailMessage transform(@Header("from") String from,
            @Header("replyTo") String replyTo, @Header("to") String to,
            @Header("subject") String subject, @Payload String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setReplyTo(replyTo);
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        return message;
    }

    @Transformer
    public MimeMailMessage transformWithAttachment(@Header("from") String from,
            @Header("replyTo") String replyTo, @Header("to") String to,
            @Header("subject") String subject, @Payload String body,
            @Header("attachData") byte[] attachData,
            @Header("attachDataType") String attachDataType,
            @Header("attachFileName") String attachFileName,
            @Header("attachDesc") String attachDesc) {
        MimeMessage message = notify_mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setReplyTo(replyTo);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(attachFileName, new ByteArrayResource(
                    attachData, attachDesc), attachDataType);
        } catch (Exception ex) {
            log.error("Error thrown during transformation:", ex);
        }
        log.debug("Message body after transformation: " + body);
        return new MimeMailMessage(message);
    }
}
