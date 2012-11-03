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
package org.eigenbase.relopt;

import org.eigenbase.oj.rel.*;
import org.eigenbase.rel.*;
import org.eigenbase.util.*;

import net.hydromatic.optiq.rules.java.EnumerableRel;


/**
 * <code>CallingConvention</code> enumerates the calling conventions built in to
 * the Saffron project. This set can be extended by applications by defining new
 * instances of CallingConvention and registering them with a planner, along
 * with the desired conversion rules. Extended ordinals must be greater than
 * CallingConvention.enumeration.getMax().
 */
public class CallingConvention
    implements RelTrait
{
    //~ Static fields/initializers ---------------------------------------------

    private static int maxOrdinal;

    /**
     * The <code>NONE</code> calling convention means that expression does not
     * support any calling convention -- in other words, it is not
     * implementable, and has to be transformed to something else in order to be
     * implemented.
     */
    public static final int NONE_ORDINAL = -1;
    public static final CallingConvention NONE =
        new CallingConvention("NONE", NONE_ORDINAL, RelNode.class);

    /**
     * The <code>JAVA</code> calling convention means that the expression is
     * converted into an Openjava parse tree, which can then be un-parsed,
     * compiled, and executed as java code.
     *
     * <p>The {@link org.eigenbase.oj.rel.JavaRel#implement} method generates a
     * piece of code which will call the piece of code corresponding to the
     * parent once for each row:
     *
     * <ul>
     * <li>The <dfn>parent code</dfn> is generated by calling {@link
     * JavaRelImplementor#generateParentBody}, which in turn calls {@link
     * org.eigenbase.oj.rel.JavaLoopRel#implementJavaParent} on the parent.</li>
     * <li>The code is generated into the <dfn>current block</dfn> (gleaned from
     * {@link org.eigenbase.oj.rel.JavaRelImplementor#getStatementList}).</li>
     * </ul>
     * </p>
     */
    public static final int JAVA_ORDINAL = 0;
    public static final CallingConvention JAVA =
        new CallingConvention("JAVA", JAVA_ORDINAL, JavaLoopRel.class);

    /**
     * The <code>ITERATOR</code> calling convention means that the expression is
     * converted to an openjava expression ({@link openjava.ptree.Expression})
     * which evalutes to an {@link org.eigenbase.runtime.TupleIter}. See {@link
     * org.eigenbase.rel.convert.ConverterRel}.
     */
    public static final int ITERATOR_ORDINAL = 1;
    public static final CallingConvention ITERATOR =
        new CallingConvention("ITERATOR", ITERATOR_ORDINAL, JavaRel.class);

    /**
     * The <code>ARRAY</code> calling convention results in a Java expression
     * which evaluates to an array containing the rows returned. Compare with
     * {@link #JAVA_ORDINAL}, where a loop <em>does something</em> for each row
     * returned).
     */
    public static final int ARRAY_ORDINAL = 2;
    public static final CallingConvention ARRAY =
        new CallingConvention("ARRAY", ARRAY_ORDINAL, JavaRel.class);

    /**
     * The <code>COLLECTION</code> calling convention results in a Java
     * expression which evaluates to a {@link java.util.Collection}, typically a
     * {@link java.util.ArrayList}.
     */
    public static final int COLLECTION_ORDINAL = 3;
    public static final CallingConvention COLLECTION =
        new CallingConvention("COLLECTION", COLLECTION_ORDINAL, JavaRel.class);
    public static final int VECTOR_ORDINAL = 4;
    public static final CallingConvention VECTOR =
        new CallingConvention("VECTOR", VECTOR_ORDINAL, JavaRel.class);
    public static final int ENUMERATION_ORDINAL = 5;
    public static final CallingConvention ENUMERATION =
        new CallingConvention(
            "ENUMERATION",
            ENUMERATION_ORDINAL,
            JavaRel.class);
    public static final int MAP_ORDINAL = 6;
    public static final CallingConvention MAP =
        new CallingConvention("MAP", MAP_ORDINAL, JavaRel.class);
    public static final int HASHTABLE_ORDINAL = 7;
    public static final CallingConvention HASHTABLE =
        new CallingConvention("HASHTABLE", HASHTABLE_ORDINAL, JavaRel.class);

    /**
     * The <code>ITERABLE</code> calling convention means that the expression is
     * converted to an openjava expression ({@link openjava.ptree.Expression})
     * which evaluates to an object which implements {@link Iterable}.
     */
    public static final int ITERABLE_ORDINAL = 8;
    public static final CallingConvention ITERABLE =
        new CallingConvention("ITERABLE", ITERABLE_ORDINAL, JavaRel.class);

    /**
     * The <code>EXISTS</code> calling convention is only allowed for a
     * terminator.
     */
    public static final int EXISTS_ORDINAL = 9;
    public static final CallingConvention EXISTS =
        new CallingConvention("EXISTS", EXISTS_ORDINAL, JavaRel.class);

    /**
     * The <code>RESULT_SET</code> calling convention means that the expression
     * is a {@link java.sql.ResultSet JDBC result set} or {@link
     * org.eigenbase.runtime.ResultSetProvider}. When a result set is
     * converted to another convention such as array or iterator, the default
     * object type is {@link org.eigenbase.runtime.Row}.
     */
    public static final int RESULT_SET_ORDINAL = 10;
    public static final CallingConvention RESULT_SET =
        new CallingConvention(
            "RESULT_SET",
            RESULT_SET_ORDINAL,
            ResultSetRel.class);

    /**
     * The <code>ENUMERABLE</code> calling convention means that the expression
     * is a {@link net.hydromatic.linq4j.Enumerable}.
     */
    public static final int ENUMERABLE_ORDINAL = 11;
    public static final CallingConvention ENUMERABLE =
        new CallingConvention(
            "ENUMERABLE",
            ENUMERABLE_ORDINAL,
            EnumerableRel.class);

    public static final CallingConvention [] values =
        new CallingConvention[] {
            NONE, JAVA, ITERATOR, ARRAY, COLLECTION, VECTOR, ENUMERATION, MAP,
            HASHTABLE, ITERABLE, EXISTS, RESULT_SET, ENUMERABLE,
        };

    //~ Instance fields --------------------------------------------------------

    /**
     * Enumerated value's name.
     */
    private final String name;

    /**
     * Enumerated value's ordinal.
     */
    private final int ordinal;

    /**
     * Interface that a relational expression of this calling convention must
     * implement. Must be a sub-interface of {@link RelNode}.
     */
    private final Class interfaze;

    //~ Constructors -----------------------------------------------------------

    public CallingConvention(
        String name,
        int ordinal,
        Class interfaze)
    {
        Util.pre(name != null, "name != null");

        this.name = name;
        this.ordinal = ordinal;
        this.interfaze = interfaze;
        Util.pre(
            RelNode.class.isAssignableFrom(interfaze),
            "RelNode.class.isAssignableFrom(interfaze)");
        maxOrdinal = Math.max(ordinal, maxOrdinal);
    }

    //~ Methods ----------------------------------------------------------------

    public Class getInterface()
    {
        return interfaze;
    }

    public static int generateOrdinal()
    {
        return maxOrdinal + 1;
    }

    public String getName()
    {
        return name;
    }

    public int getOrdinal()
    {
        return ordinal;
    }

    // Implement RelTrait
    public RelTraitDef getTraitDef()
    {
        return CallingConventionTraitDef.instance;
    }

    /**
     * Returns the ordinal as the CallingConvention's hash code.
     *
     * @return ordinal
     */
    public int hashCode()
    {
        return ordinal;
    }

    /**
     * Compares this CallingConvention to another for equality by ordinal.
     *
     * @param o the other CallingConvention
     *
     * @return true if they are equal, false otherwise
     */
    public boolean equals(Object o)
    {
        return this == o
            || o instanceof CallingConvention
            && ordinal == ((CallingConvention) o).ordinal;
    }

    /**
     * Returns the value's name.
     */
    public String toString()
    {
        return name;
    }
}

// End CallingConvention.java
