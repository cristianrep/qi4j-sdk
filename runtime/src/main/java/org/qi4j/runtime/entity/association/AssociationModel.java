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

package org.qi4j.runtime.entity.association;

import org.qi4j.api.common.MetaInfo;
import org.qi4j.api.common.QualifiedName;
import org.qi4j.api.composite.Composite;
import org.qi4j.api.constraint.ConstraintViolation;
import org.qi4j.api.constraint.ConstraintViolationException;
import org.qi4j.api.entity.Aggregated;
import org.qi4j.api.entity.Queryable;
import org.qi4j.api.entity.association.Association;
import org.qi4j.api.entity.association.AssociationInfo;
import org.qi4j.api.entity.association.GenericAssociationInfo;
import org.qi4j.api.property.Immutable;
import org.qi4j.api.util.Classes;
import org.qi4j.functional.Visitable;
import org.qi4j.functional.Visitor;
import org.qi4j.bootstrap.BindingException;
import org.qi4j.runtime.composite.ConstraintsCheck;
import org.qi4j.runtime.composite.ValueConstraintsInstance;
import org.qi4j.runtime.model.Binder;
import org.qi4j.runtime.model.Resolution;
import org.qi4j.runtime.structure.ModuleUnitOfWork;
import org.qi4j.runtime.unitofwork.BuilderEntityState;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.association.AssociationDescriptor;

import java.lang.reflect.*;
import java.util.List;

/**
 * JAVADOC
 */
public final class AssociationModel
    implements AssociationDescriptor, ConstraintsCheck, Binder, Visitable<AssociationModel>
{
    private MetaInfo metaInfo;
    private Type type;
    private AccessibleObject accessor;
    private QualifiedName qualifiedName;
    private ValueConstraintsInstance constraints;
    private ValueConstraintsInstance associationConstraints;
    private boolean queryable;
    private boolean immutable;
    private boolean aggregated;
    private AssociationInfo builderInfo;

    public AssociationModel( AccessibleObject accessor,
                             ValueConstraintsInstance valueConstraintsInstance,
                             ValueConstraintsInstance associationConstraintsInstance,
                             MetaInfo metaInfo
    )
    {
        this.metaInfo = metaInfo;
        this.constraints = valueConstraintsInstance;
        this.associationConstraints = associationConstraintsInstance;
        this.accessor = accessor;
        initialize();
    }

    private void initialize()
    {
        this.type = GenericAssociationInfo.getAssociationType( accessor );
        this.qualifiedName = QualifiedName.fromAccessor( accessor );
        this.immutable = metaInfo.get( Immutable.class ) != null;
        this.aggregated = metaInfo.get( Aggregated.class ) != null;

        final Queryable queryable = accessor.getAnnotation( Queryable.class );
        this.queryable = queryable == null || queryable.value();
    }

    public <T> T metaInfo( Class<T> infoType )
    {
        return metaInfo.get( infoType );
    }

    public QualifiedName qualifiedName()
    {
        return qualifiedName;
    }

    public Type type()
    {
        return type;
    }

    public boolean isImmutable()
    {
        return immutable;
    }

    public boolean isAggregated()
    {
        return aggregated;
    }

    public AccessibleObject accessor()
    {
        return accessor;
    }

    @Override
    public boolean queryable()
    {
        return queryable;
    }

    @Override
    public <ThrowableType extends Throwable> boolean accept( Visitor<? super AssociationModel, ThrowableType> visitor ) throws ThrowableType
    {
        return visitor.visit( this );
    }

    public <T> Association<T> newInstance( ModuleUnitOfWork uow, EntityState state )
    {
        Association<T> associationInstance = new AssociationInstance<T>( state instanceof BuilderEntityState ? builderInfo : this, this, uow, state );

/* TODO What was this supposed to do?
        if( Composite.class.isAssignableFrom( accessor.getReturnType() ) )
        {
            associationInstance = (Association<T>) uow.module()
                .transientBuilderFactory()
                .newTransientBuilder( accessor.getReturnType() )
                .use( associationInstance )
                .newInstance();
        }
*/

        return associationInstance;
    }

    public void checkConstraints( Object value )
        throws ConstraintViolationException
    {
        if( constraints != null )
        {
            List<ConstraintViolation> violations = constraints.checkConstraints( value );
            if( !violations.isEmpty() )
            {
                throw new ConstraintViolationException( "", "<unknown>", (Member) accessor, violations );
            }
        }
    }

    public void checkConstraints( AssociationsInstance associations )
        throws ConstraintViolationException
    {
        if( constraints != null )
        {
            Object value = associations.associationFor( accessor ).get();
            checkConstraints( value );
        }
    }

    public void checkAssociationConstraints( AssociationsInstance associationsInstance )
        throws ConstraintViolationException
    {
        if( associationConstraints != null )
        {
            Association association = associationsInstance.associationFor( accessor );

            List<ConstraintViolation> violations = associationConstraints.checkConstraints( association );
            if( !violations.isEmpty() )
            {
                throw new ConstraintViolationException( (Composite) association.get(), (Member) accessor, violations );
            }
        }
    }

    @Override
    public void bind( Resolution resolution ) throws BindingException
    {
        builderInfo = new AssociationDescriptor()
        {
            @Override
            public boolean isImmutable()
            {
                return false;
            }

            @Override
            public <T> T metaInfo( Class<T> infoType )
            {
                return metaInfo.get( infoType );
            }

            @Override
            public QualifiedName qualifiedName()
            {
                return qualifiedName;
            }

            @Override
            public Type type()
            {
                return type;
            }

            @Override
            public AccessibleObject accessor()
            {
                return accessor;
            }

            @Override
            public boolean isAggregated()
            {
                return aggregated;
            }

            @Override
            public boolean queryable()
            {
                return queryable;
            }
        };

        if (type instanceof TypeVariable)
        {
            type = Classes.resolveTypeVariable( (TypeVariable) type, ((Member)accessor).getDeclaringClass(), resolution.object().type());
        }
    }

    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        if( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        AssociationModel that = (AssociationModel) o;

        if( !accessor.equals( that.accessor ) )
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return accessor.hashCode();
    }

    @Override
    public String toString()
    {
        if (accessor instanceof Field )
          return ((Field)accessor).toGenericString();
        else
            return ((Method)accessor).toGenericString();
    }
}
