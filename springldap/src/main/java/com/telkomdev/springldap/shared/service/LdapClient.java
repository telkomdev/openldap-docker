package com.telkomdev.springldap.shared.service;

import com.telkomdev.springldap.modules.users.domain.User;
import com.telkomdev.springldap.shared.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.*;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.ldap.support.LdapUtils;

import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.directory.DirContext;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

public class LdapClient {

    private final static Logger logger = LoggerFactory.getLogger(LdapClient.class);

    @Autowired
    private Environment env;

    @Autowired
    private ContextSource contextSource;

    @Autowired
    private LdapTemplate ldapTemplate;

    // https://docs.spring.io/spring-ldap/docs/2.3.2.RELEASE/reference/#basic-authentication
    public Boolean authenticate(final String username, final String password) {
        DirContext ctx = null;
        try {
            ctx = contextSource.getContext("cn=" + username + "," + env.getRequiredProperty("ldap.baseDn"), password);
            return true;
        } catch (Exception e) {
            // Context creation failed - authentication did not succeed
            logger.error("Login failed", e.getMessage());
            return false;
        } finally {
            // It is imperative that the created DirContext instance is always closed
            LdapUtils.closeContext(ctx);
        }
    }

    public List<String> search(final String username) {
        return ldapTemplate.search(
                env.getRequiredProperty("ldap.baseDn"),
                "cn=" + username,
                (AttributesMapper<String>) attrs -> (String) attrs
                        .get("cn")
                        .get());
    }

    public Result<User, String> create(User user) {
        DirContextAdapter context = null;
        try {
            //javax.naming.NameAlreadyBoundException
            Name dn = LdapNameBuilder
                    .newInstance()
                    .add("cn", user.getUsername())
                    .build();
            context = new DirContextAdapter(dn);

            //context.setAttributeValues("objectclass", new String[]{"top", "person", "organizationalPerson", "inetOrgPerson"});
            context.setAttributeValues("objectclass", new String[]{"inetOrgPerson"});
            context.setAttributeValue("cn", user.getUsername());
            context.setAttributeValue("sn", user.getUsername());
            context.setAttributeValue("givenname", user.getUsername());
            context.setAttributeValue("mail", user.getEmail());
            context.setAttributeValue("displayname", user.getFullName());
            context.setAttributeValue("userPassword", digestSHA(user.getPassword()));

            ldapTemplate.bind(context);
            return Result.from(user, null);
        } catch (Exception e) {
            logger.error("create new user failed", e.getMessage());
            if (e instanceof NameAlreadyBoundException) {
                return Result.from(null, new String("user " + user.getUsername() + " already exist"));
            }

            return Result.from(null, e.getMessage());
        } finally {
            LdapUtils.closeContext(context);
        }

    }

    public Result<User, String> modify(User user) {
        DirContextOperations context = null;
        try {
            Name dn = LdapNameBuilder
                    .newInstance()
                    .add("cn", user.getUsername())
                    .build();
            context = ldapTemplate.lookupContext(dn);

            context.setAttributeValues("objectclass", new String[]{"inetOrgPerson"});
            context.setAttributeValue("cn", user.getUsername());
            context.setAttributeValue("sn", user.getUsername());
            context.setAttributeValue("givenname", user.getUsername());
            context.setAttributeValue("mail", user.getEmail());
            context.setAttributeValue("displayname", user.getFullName());
            context.setAttributeValue("userPassword", digestSHA(user.getPassword()));

            ldapTemplate.modifyAttributes(context);
            return Result.from(user, null);
        } catch (Exception e) {
            logger.error("update user failed", e.getMessage());
            return Result.from(null, e.getMessage());
        } finally {
            LdapUtils.closeContext(context);
        }
    }

    private String digestSHA(final String password) {
        String base64;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            digest.update(password.getBytes());
            base64 = Base64
                    .getEncoder()
                    .encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return "{SHA}" + base64;
    }
}
