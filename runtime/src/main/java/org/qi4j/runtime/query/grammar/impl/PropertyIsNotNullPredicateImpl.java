/*
 * Copyright 2008 Alin Dreghiciu.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.qi4j.runtime.query.grammar.impl;

import org.qi4j.api.property.Property;
import org.qi4j.api.query.grammar.PropertyIsNotNullPredicate;
import org.qi4j.api.query.grammar.PropertyReference;

/**
 * Default {@link org.qi4j.api.query.grammar.PropertyIsNotNullPredicate} implementation.
 *
 * @author Alin Dreghiciu
 * @since March 25, 2008
 */
public final class PropertyIsNotNullPredicateImpl<T>
    extends PropertyNullPredicateImpl<T>
    implements PropertyIsNotNullPredicate<T>
{

    /**
     * Constructor.
     *
     * @param propertyReference property reference; cannot be null
     * @throws IllegalArgumentException - If property reference is null
     */
    public PropertyIsNotNullPredicateImpl( final PropertyReference<T> propertyReference )
    {
        super( propertyReference );
    }

    /**
     * @see org.qi4j.api.query.grammar.BooleanExpression#eval(Object)
     */
    public boolean eval( final Object target )
    {
        final Property<T> prop = propertyReference().eval( target );
        return prop != null && prop.get() != null;
    }

    @Override public String toString()
    {
        return new StringBuilder()
            .append( "( " )
            .append( propertyReference() )
            .append( " IS NOT NULL )" )
            .toString();
    }

}