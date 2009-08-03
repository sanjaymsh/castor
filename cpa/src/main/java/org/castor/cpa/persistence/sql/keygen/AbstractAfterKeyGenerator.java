/*
 * Copyright 2009 Ahmad Hassan, Ralf Joachim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.castor.cpa.persistence.sql.keygen;

import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.castor.persist.ProposedEntity;
import org.exolab.castor.jdo.Database;
import org.exolab.castor.jdo.PersistenceException;
import org.exolab.castor.jdo.engine.SQLEngine;
import org.exolab.castor.persist.spi.Identity;
import org.exolab.castor.persist.spi.PersistenceFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.exolab.castor.core.exceptions.CastorIllegalStateException;
import org.exolab.castor.jdo.DuplicateIdentityException;
import org.exolab.castor.jdo.engine.SQLColumnInfo;
import org.exolab.castor.jdo.engine.SQLFieldInfo;
import org.exolab.castor.jdo.engine.nature.ClassDescriptorJDONature;
import org.castor.core.util.AbstractProperties;
import org.castor.core.util.Messages;
import org.castor.cpa.CPAProperties;
import org.castor.cpa.persistence.sql.engine.SQLStatementInsertCheck;
import org.castor.jdo.engine.DatabaseContext;
import org.castor.jdo.engine.DatabaseRegistry;
import org.castor.jdo.engine.SQLTypeInfos;
import org.exolab.castor.mapping.ClassDescriptor;
import org.exolab.castor.mapping.MappingException;

/**
 * Abstract class that implements the KeyGenerator interface for AFTER_INSERT style. The key
 * generator is used for producing identities for objects before they are created in the
 * database.
 * 
 * @author <a href="mailto:ahmad DOT hassan AT gmail DOT com">Ahmad Hassan</a>
 * @author <a href="mailto:ralf DOT joachim AT syscon DOT eu">Ralf Joachim</a>
 * @version $Revision$ $Date: 2009-07-13 17:22:43 (Tue, 28 Jul 2009) $
 */
public abstract class AbstractAfterKeyGenerator implements KeyGenerator {
    //-----------------------------------------------------------------------------------    

    /** The <a href="http://jakarta.apache.org/commons/logging/">Jakarta
     *  Commons Logging</a> instance used for all logging. */
    private static final Log LOG = LogFactory.getLog(AbstractAfterKeyGenerator.class);
    
    /** Persistence factory for the database engine the entity is persisted in.
     *  Used to format the SQL statement. */
    private PersistenceFactory _factory;
    
    /** SQL engine for all persistence operations at entities of the type this
     * class is responsible for. Holds all required information of the entity type. */
    private SQLEngine _engine;
    
    /** Represents the engine type obtained from clas descriptor. */
    private String _engineType = null;
    
    /** Boolean value specifies the Property whether JDBC 3.0-specific features 
     *  should be used. */
    private final boolean _useJDBC30;


    /**
     * Constructor.
     * 
     * @param factory  Persistence factory for the database engine the entity is persisted in.
     *  Used to format the SQL statement
     */
    public AbstractAfterKeyGenerator(final PersistenceFactory factory) {
        _factory = factory;
        AbstractProperties properties = CPAProperties.getInstance();
        _useJDBC30 = properties.getBoolean(CPAProperties.USE_JDBC30, false);
    }
    
    /**
     * {@inheritDoc}
     */
    public final byte getStyle() {
        return AFTER_INSERT;
    }

    /**
     * {@inheritDoc}
     */
    public Object executeStatement(final SQLEngine engine, final String statement, 
            final Database database, final Connection conn, final Identity identity, 
            final ProposedEntity entity) throws PersistenceException {
        _engine = engine;

        ClassDescriptor clsDesc = _engine.getDescriptor();
        _engineType = clsDesc.getJavaClass().getName();
        String mapTo = new ClassDescriptorJDONature(clsDesc).getTableName();
        SQLStatementInsertCheck lookupStatement = new SQLStatementInsertCheck(_engine, _factory);
        Identity internalIdentity = identity;
        SQLEngine extended = _engine.getExtends();
        PreparedStatement stmt = null;
        
        try {
            // must create record in the parent table first. all other dependents
            // are created afterwards. quick and very dirty hack to try to make
            // multiple class on the same table work.
            if (extended != null) {
                ClassDescriptor extDesc = extended.getDescriptor();
                if (!new ClassDescriptorJDONature(extDesc).getTableName().equals(mapTo)) {
                    internalIdentity = extended.create(database, conn, entity, internalIdentity);
                }
            }
            
            if ((internalIdentity == null) && _useJDBC30) {
                Field field = Statement.class.getField("RETURN_GENERATED_KEYS");
                Integer rgk = (Integer) field.get(statement);
                
                Class[] types = new Class[] {String.class, int.class};
                Object[] args = new Object[] {statement, rgk};
                Method method = Connection.class.getMethod("prepareStatement", types);
                stmt = (PreparedStatement) method.invoke(conn, args);
                    
                // stmt = conn.prepareStatement(_statement, Statement.RETURN_GENERATED_KEYS);
            } else {
                stmt = conn.prepareStatement(statement);
            }
             
            if (LOG.isTraceEnabled()) {
                LOG.trace(Messages.format("jdo.creating", _engineType, stmt.toString()));
            }
            
            // must remember that SQL column index is base one.
            int count = 1;
            count = bindFields(entity, stmt, count);

            if (LOG.isDebugEnabled()) {
                LOG.debug(Messages.format("jdo.creating", _engineType, stmt.toString()));
            }

            stmt.executeUpdate();

            SQLColumnInfo[] ids = _engine.getColumnInfoForIdentities();

            if (internalIdentity == null) {
                if (_useJDBC30) {
                    // use key returned by INSERT statement.
                    Class cls = PreparedStatement.class;
                    Method method = cls.getMethod("getGeneratedKeys", (Class[]) null);
                    ResultSet keySet = (ResultSet) method.invoke(stmt, (Object[]) null);
                    // ResultSet keySet = stmt.getGeneratedKeys();
                    
                    int i = 1;
                    int sqlType;
                    List<Object> keys = new ArrayList<Object>();
                    while (keySet.next()) {
                        sqlType = ids[i - 1].getSqlType();
                        Object temp;
                        if (sqlType == java.sql.Types.INTEGER) {
                            temp = new Integer(keySet.getInt(i));
                        } else if (sqlType == java.sql.Types.NUMERIC) {
                            temp = keySet.getBigDecimal(i);
                        } else {
                            temp = keySet.getObject(i);
                        }

                        keys.add(ids[i - 1].toJava(temp));
                        i++;
                    }
                    internalIdentity = new Identity(keys.toArray());

                    stmt.close();
                } else {
                    // generate key after INSERT.
                    internalIdentity = generateKey(database, conn, stmt, mapTo);

                    stmt.close();
                }
            }

            return internalIdentity;
        } catch (SQLException except) {
            LOG.fatal(Messages.format("jdo.storeFatal",  _engineType,  statement), except);

            Boolean isDupKey = _factory.isDuplicateKeyException(except);
            if (Boolean.TRUE.equals(isDupKey)) {
                throw new DuplicateIdentityException(Messages.format(
                        "persist.duplicateIdentity", _engineType, internalIdentity), except);
            } else if (Boolean.FALSE.equals(isDupKey)) {
                throw new PersistenceException(Messages.format("persist.nested", except), except);
            }

            // without an identity we can not check for duplicate key
            if (internalIdentity == null) {
                throw new PersistenceException(Messages.format("persist.nested", except), except);
            }

            // check for duplicate key the old fashioned way, after the INSERT
            // failed to prevent race conditions and optimize INSERT times.
            lookupStatement.insertDuplicateKeyCheck(conn, internalIdentity);

            try {
                if (stmt != null) { stmt.close(); }
            } catch (SQLException except2) {
                LOG.warn("Problem closing JDBC statement", except2);
            }
            
            throw new PersistenceException(Messages.format("persist.nested", except), except);
        } catch (NoSuchMethodException ex) {
            throw new CastorIllegalStateException(ex);
        } catch (NoSuchFieldException ex) {
            throw new CastorIllegalStateException(ex);
        } catch (IllegalAccessException ex) {
            throw new CastorIllegalStateException(ex);
        } catch (InvocationTargetException ex) {
            throw new CastorIllegalStateException(ex);
        }
    }
    
    /**
     * Binds parameters values to the PreparedStatement.
     * 
     * @param entity
     * @param stmt PreparedStatement object containing sql staatement.
     * @param count Offset.
     * @return final Offset
     * @throws SQLException If a database access error occurs.
     * @throws PersistenceException If identity size mismatches.
     */
    private int bindFields(final ProposedEntity entity, final PreparedStatement stmt,
            final int count) throws SQLException, PersistenceException {
        int internalCount = count;
        SQLFieldInfo[] fields = _engine.getInfo();
        for (int i = 0; i < fields.length; ++i) {
            SQLColumnInfo[] columns = fields[i].getColumnInfo();
            if (fields[i].isStore()) {
                Object value = entity.getField(i);
                if (value == null) {
                    for (int j = 0; j < columns.length; j++) {
                        stmt.setNull(internalCount++, columns[j].getSqlType());
                    }
                } else if (value instanceof Identity) {
                    Identity identity = (Identity) value;
                    if (identity.size() != columns.length) {
                        throw new PersistenceException("Size of identity field mismatch!");
                    }
                    for (int j = 0; j < columns.length; j++) {
                        SQLTypeInfos.setValue(stmt, internalCount++,
                                columns[j].toSQL(identity.get(j)), columns[j].getSqlType());
                    }
                } else {
                    if (columns.length != 1) {
                        throw new PersistenceException("Complex field expected!");
                    }
                    SQLTypeInfos.setValue(stmt, internalCount++, columns[0].toSQL(value),
                            columns[0].getSqlType());
                }
            }
        }
        return internalCount;
    }

    /**
     * Generates the key.
     * 
     * @param database Particular Database instance.
     * @param conn An open JDBC Connection. 
     * @param stmt PreparedStatement containing the SQL statement.
     * @param mapTo Name of Table. 
     * @return Identity that is generated.
     * @throws PersistenceException If fails to Generate key.
     */
    private Identity generateKey(final Database database, final Connection conn,
            final PreparedStatement stmt, final String mapTo)
    throws PersistenceException {
        SQLColumnInfo id = _engine.getColumnInfoForIdentities()[0];

        // TODO [SMH]: Change KeyGenerator.isInSameConnection to KeyGenerator.useSeparateConnection?
        // TODO [SMH]: Move "if (_keyGen.isInSameConnection() == false)"
        //                 out of SQLEngine and into key-generator?
        Connection connection = conn;
        if (!this.isInSameConnection()) {
        connection = getSeparateConnection(database);
        }

        Properties prop = null;
        if (stmt != null) {
        prop = new Properties();
        prop.put("insertStatement", stmt);
        }

        try {
            Object identity;
            synchronized (connection) {
            identity = this.generateKey(connection, mapTo, id.getName(), prop);
            }

            // TODO [SMH]: Move "if (identity == null)" into keygenerator.
            if (identity == null) {
            throw new PersistenceException(
            Messages.format("persist.noIdentity", _engineType));
            }

            return new Identity(id.toJava(identity));
        } finally {
            if (!this.isInSameConnection()) {
                closeSeparateConnection(connection);
            }
        }
    }

    /**
     * Operning new JDBC Connection. 
     * 
     * @param database The database on which it opens the JDBC connection.
     * @return A JDBC Connection
     * @throws PersistenceException If fails to open connection.
     */
    private Connection getSeparateConnection(final Database database)
    throws PersistenceException {
        DatabaseContext context = null;
        try {
            context = DatabaseRegistry.getDatabaseContext(database.getDatabaseName());
        } catch (MappingException e) {
            throw new PersistenceException(Messages.message("persist.cannotCreateSeparateConn"), e);
        }
        
        try {
            Connection conn = context.getConnectionFactory().createConnection();
            conn.setAutoCommit(false);
            return conn;
        } catch (SQLException e) {
            throw new PersistenceException(Messages.message("persist.cannotCreateSeparateConn"), e);
        }
    }
    
    /**
     * Close the JDBC Connection.
     * 
     * @param conn A JDBC Connection.
     */
    private void closeSeparateConnection(final Connection conn) {
        try {
            if (!conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
    }
    
}
