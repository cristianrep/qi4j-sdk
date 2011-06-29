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
import org.qi4j.api.common.Optional;
import org.qi4j.api.common.QualifiedName;
import org.qi4j.api.entity.association.Association;
import org.qi4j.api.entity.association.GenericAssociationInfo;
import org.qi4j.api.util.Annotations;
import org.qi4j.api.util.Classes;
import org.qi4j.functional.HierarchicalVisitor;
import org.qi4j.functional.VisitableHierarchy;
import org.qi4j.bootstrap.AssociationDeclarations;
import org.qi4j.runtime.composite.ConstraintsModel;
import org.qi4j.runtime.composite.ValueConstraintsInstance;
import org.qi4j.runtime.composite.ValueConstraintsModel;
import org.qi4j.runtime.structure.ModuleUnitOfWork;
import org.qi4j.spi.entity.EntityState;
import org.qi4j.spi.entity.association.AssociationDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.util.*;

import static org.qi4j.api.util.Annotations.isType;
import static org.qi4j.functional.Iterables.filter;
import static org.qi4j.functional.Iterables.first;

/**
 * JAVADOC
 */
public final class AssociationsModel
    implements VisitableHierarchy<AssociationsModel, AssociationModel>
{
    private final Set<AccessibleObject> accessors = new HashSet<AccessibleObject>();
    private final Set<AssociationModel> associationModels = new LinkedHashSet<AssociationModel>();
    private final Map<AccessibleObject, AssociationModel> mapAccessorAssociationModel = new HashMap<AccessibleObject, AssociationModel>();
    private final Map<QualifiedName, AccessibleObject> mapQualifiedNameAccessors = new HashMap<QualifiedName, AccessibleObject>();
    private final ConstraintsModel constraints;
    private AssociationDeclarations associationDeclarations;

    public AssociationsModel( ConstraintsModel constraints, AssociationDeclarations associationDeclarations )
    {
        this.constraints = constraints;
        this.associationDeclarations = associationDeclarations;
    }

    public void addAssociationFor( AccessibleObject accessor )
    {
        if( !accessors.contains( accessor ) )
        {
            if( Association.class.isAssignableFrom( Classes.RAW_CLASS.map( Classes.TYPE_OF.map( accessor ) ) ))
            {
                Iterable<Annotation> annotations = Annotations.getAccessorAndTypeAnnotations( accessor );
                boolean optional = first( filter( isType( Optional.class ), annotations ) ) != null;

                // Constraints for Association references
                ValueConstraintsModel valueConstraintsModel = constraints.constraintsFor( annotations, GenericAssociationInfo
                    .getAssociationType( accessor ), ((Member)accessor).getName(), optional );
                ValueConstraintsInstance valueConstraintsInstance = null;
                if( valueConstraintsModel.isConstrained() )
                {
                    valueConstraintsInstance = valueConstraintsModel.newInstance();
                }

                // Constraints for the Association itself
                valueConstraintsModel = constraints.constraintsFor( annotations, Association.class, ((Member)accessor).getName(), optional );
                ValueConstraintsInstance associationValueConstraintsInstance = null;
                if( valueConstraintsModel.isConstrained() )
                {
                    associationValueConstraintsInstance = valueConstraintsModel.newInstance();
                }

                MetaInfo metaInfo = associationDeclarations.getMetaInfo( accessor );
                AssociationModel associationModel = new AssociationModel( accessor, valueConstraintsInstance, associationValueConstraintsInstance, metaInfo );
                if( !mapQualifiedNameAccessors.containsKey( associationModel.qualifiedName() ) )
                {
                    associationModels.add( associationModel );
                    mapAccessorAssociationModel.put( accessor, associationModel );
                    mapQualifiedNameAccessors.put( associationModel.qualifiedName(), associationModel.accessor() );
                }
            }
            accessors.add( accessor );
        }
    }

    public <T extends AssociationDescriptor> Set<T> associations()
    {
        return (Set<T>) associationModels;
    }

    @Override
    public <ThrowableType extends Throwable> boolean accept( HierarchicalVisitor<? super AssociationsModel, ? super AssociationModel, ThrowableType> visitor ) throws ThrowableType
    {
        if (visitor.visitEnter( this ))
        {
            for( AssociationModel associationModel : associationModels )
            {
                if (!associationModel.accept(visitor))
                    break;
            }
        }
        return visitor.visitLeave( this );
    }

    public <T> Association<T> newInstance( AccessibleObject accessor, EntityState entityState, ModuleUnitOfWork uow )
    {
        return mapAccessorAssociationModel.get( accessor ).newInstance( uow, entityState );
    }

    public AssociationDescriptor getAssociationByName( String name )
    {
        for( AssociationModel associationModel : associationModels )
        {
            if( associationModel.qualifiedName().name().equals( name ) )
            {
                return associationModel;
            }
        }

        return null;
    }

    public void checkConstraints( AssociationsInstance associations )
    {
        for( AssociationModel associationModel : associationModels )
        {
            associationModel.checkAssociationConstraints( associations );
            associationModel.checkConstraints( associations );
        }
    }

    public AssociationsInstance newInstance( EntityState entityState, ModuleUnitOfWork uow )
    {
        return new AssociationsInstance( this, entityState, uow );
    }
}
