/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
*/
package org.eigenbase.sql.validate;

import java.util.*;

import org.eigenbase.reltype.*;
import org.eigenbase.sql.*;


/**
 * Represents the name-resolution context for expressions in an ORDER BY clause.
 *
 * <p>In some dialects of SQL, the ORDER BY clause can reference column aliases
 * in the SELECT clause. For example, the query
 *
 * <blockquote><code>SELECT empno AS x<br/>
 * FROM emp<br/>
 * ORDER BY x</code></blockquote>
 *
 * is valid.
 *
 * @author jhyde
 * @version $Id$
 * @since Mar 25, 2003
 */
public class OrderByScope
    extends DelegatingScope
{
    //~ Instance fields --------------------------------------------------------

    private final SqlNodeList orderList;
    private final SqlSelect select;

    //~ Constructors -----------------------------------------------------------

    OrderByScope(
        SqlValidatorScope parent,
        SqlNodeList orderList,
        SqlSelect select)
    {
        super(parent);
        this.orderList = orderList;
        this.select = select;
    }

    //~ Methods ----------------------------------------------------------------

    public SqlNode getNode()
    {
        return orderList;
    }

    public void findAllColumnNames(List<SqlMoniker> result)
    {
        final SqlValidatorNamespace ns = validator.getNamespace(select);
        addColumnNames(ns, result);
    }

    public SqlIdentifier fullyQualify(SqlIdentifier identifier)
    {
        // If it's a simple identifier, look for an alias.
        if (identifier.isSimple()
            && validator.getConformance().isSortByAlias())
        {
            String name = identifier.names[0];
            final SqlValidatorNamespace selectNs =
                validator.getNamespace(select);
            final RelDataType rowType = selectNs.getRowType();
            if (SqlValidatorUtil.lookupField(rowType, name) != null) {
                return identifier;
            }
        }
        return super.fullyQualify(identifier);
    }

    public RelDataType resolveColumn(String name, SqlNode ctx)
    {
        final SqlValidatorNamespace selectNs = validator.getNamespace(select);
        final RelDataType rowType = selectNs.getRowType();
        final RelDataType dataType =
            SqlValidatorUtil.lookupFieldType(rowType, name);
        if (dataType != null) {
            return dataType;
        }
        final SqlValidatorScope selectScope = validator.getSelectScope(select);
        return selectScope.resolveColumn(name, ctx);
    }

    public void validateExpr(SqlNode expr)
    {
        SqlNode expanded = validator.expandOrderExpr(select, expr);

        // expression needs to be valid in parent scope too
        parent.validateExpr(expanded);
    }
}

// End OrderByScope.java
