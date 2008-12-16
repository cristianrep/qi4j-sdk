/*
 * Copyright (c) 2008, Rickard Öberg. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.qi4j.runtime.entity;

import java.lang.reflect.Method;
import org.qi4j.api.constraint.ConstraintViolationException;
import org.qi4j.api.entity.Queryable;
import org.qi4j.api.property.Property;
import org.qi4j.runtime.composite.ValueConstraintsInstance;
import org.qi4j.runtime.property.PropertyModel;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.PropertyType;
import org.qi4j.api.util.ClassUtil;
import org.qi4j.api.common.MetaInfo;

/**
 * TODO
 */
public final class EntityPropertyModel extends PropertyModel
{

    private final boolean queryable;

    public EntityPropertyModel( Method anAccessor, ValueConstraintsInstance constraints, MetaInfo metaInfo, Object defaultValue )
    {
        super( anAccessor, constraints, metaInfo, defaultValue );
        final Queryable queryable = anAccessor.getAnnotation( Queryable.class );
        this.queryable = queryable == null || queryable.value();
    }

    public Property newEntityInstance( EntityState state )
    {
        if( isComputed() )
        {
            return super.newDefaultInstance();
        }
        else
        {
            return new EntityPropertyInstance( this, state, this );
        }
    }

    public void setState( Property property, EntityState entityState )
        throws ConstraintViolationException
    {
        Object value = property.get();

        // Check constraints
        checkConstraints( value );

        entityState.setProperty( qualifiedName(), value );
    }

    public PropertyType propertyType()
    {
        PropertyType.PropertyTypeEnum type;
        if( isComputed() )
        {
            type = PropertyType.PropertyTypeEnum.COMPUTED;
        }
        else if( isImmutable() )
        {
            type = PropertyType.PropertyTypeEnum.IMMUTABLE;
        }
        else
        {
            type = PropertyType.PropertyTypeEnum.MUTABLE;
        }

        return new PropertyType( qualifiedName(), ClassUtil.getRawClass( type() ).getName(), toURI(), toRDF(), queryable, type );
    }
}
