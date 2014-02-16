package com.googlecode.fascinator.plugins.authentication.internal;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.googlecode.fascinator.authentication.internal.InternalUser;
import com.googlecode.fascinator.common.authentication.hibernate.HibernateUser;
import com.googlecode.fascinator.common.authentication.hibernate.HibernateUserService;

//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration({ "file:src/test/resources/test-applicationContext.xml" })
public class InternalUserHibernateTest {

//	@Autowired
//    private HibernateUserService hibernateAuthUserService;
//	
	  @Test
    public void testInternalUser() throws Exception {
//        InternalUser intUser = new InternalUser();
//        String username = "user-" + System.currentTimeMillis();
//        String source = "internal-" + System.currentTimeMillis();
//        String password = "password-" + System.currentTimeMillis();
//        intUser.setUsername(username);
//        intUser.setSource(source);
//        intUser.password = password;
//        HibernateUser user = new HibernateUser(intUser);
//        hibernateAuthUserService.addUser(user);
//
//        HibernateUser fromDb = hibernateAuthUserService.getUser(username);
//        assertEquals(username, fromDb.getUsername());
//        assertEquals(password, fromDb.getAttributes().get("password")
//                .getValStr());
//
//        password = "password2";
//        intUser.password = password;
//        fromDb.mergeAttributes(intUser);
//        hibernateAuthUserService.saveUser(fromDb);
//
//        fromDb = hibernateAuthUserService.getUser(username);
//
//        assertEquals(password, fromDb.getAttributes().get("password")
//                .getValStr());
    }


}
