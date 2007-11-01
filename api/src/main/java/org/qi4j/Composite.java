/*
 * Copyright (c) 2007, Rickard Öberg. All Rights Reserved.
 * Copyright (c) 2007, Niclas Hedhman. All Rights Reserved.
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
package org.qi4j;

import org.qi4j.model.CompositeModel;

/**
 * All Composite objects must implement this interface. Let the
 * Composite interface extend this one. An implementation will be provided
 * by the framework.
 */
public interface Composite
{
    /**
     * Cast the current object to the given interface.
     * <p/>
     * The returned object uses the current object which provides mixins
     * that should be reused for this new object.
     *
     * @param anObjectType an interface that describes the object to be created
     * @return a new composite object implementing the interface
     */
    <T extends Composite> T cast( Class<T> anObjectType );

    /**
     * Checks if the object can be cast() to the provided object type.
     *
     * @param anObjectType The object type we want to check the assignability to for this object.
     * @return true if a cast() is possible of this object to the provided object type.
     */
    boolean isInstance( Class anObjectType );

    /**
     * Get the model for this object
     *
     * @return the CompositeModel that describes this thisAs
     */
    CompositeModel getCompositeModel();

    Composite dereference();
}