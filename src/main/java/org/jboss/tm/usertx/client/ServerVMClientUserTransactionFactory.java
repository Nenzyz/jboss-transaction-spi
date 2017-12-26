/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.tm.usertx.client;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

import org.jboss.tm.TransactionPropagationContextUtil;
import javax.transaction.UserTransaction;
import javax.naming.NamingException;
import javax.naming.Reference;

import org.jboss.logging.Logger;

public class ServerVMClientUserTransactionFactory implements ObjectFactory {

    /** The log */
    private static final Logger log = Logger.getLogger(ServerVMClientUserTransactionFactory.class);

    /**
     *  The <code>UserTransaction</code> this factory will return.
     *  This is evaluated lazily in {@link #getUserTransaction()}.
     */
    static private UserTransaction userTransaction = null;

    /**
     *  Get the <code>UserTransaction</code> this factory will return.
     *  This may return a cached value from a previous call.
     */
    static private UserTransaction getUserTransaction()
    {
        if (userTransaction == null) {
            // See if we have a local TM
            try {
                 new InitialContext().lookup("java:/TransactionManager");

                // We execute in the server.
                userTransaction = ServerVMClientUserTransaction.getSingleton();
//            } catch (NamingException ex) {
            } catch (Exception ex) {
                // We execute in a stand-alone client.
                ClientUserTransaction cut = ClientUserTransaction.getSingleton();

                // Tell the proxy that this is the factory for
                // transaction propagation contexts.
                TransactionPropagationContextUtil.setTPCFactory(cut);
                userTransaction = cut;
            }
        }
        return userTransaction;
    }

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws Exception {
        // return ServerVMClientUserTransaction.getSingleton();
        Reference ref = (Reference)obj;
 
        if (!ref.getClassName().equals(ClientUserTransaction.class.getName()))
            return null;

        log.info(this + " environment " + environment.toString());

        if (environment.containsKey("java.naming.provider.url")) {
            ClientUserTransaction localClientUserTransaction = ClientUserTransaction.getSingleton();

            TransactionPropagationContextUtil.setTPCFactory(localClientUserTransaction);
            return localClientUserTransaction;
        }

        return getUserTransaction();
    }
}
