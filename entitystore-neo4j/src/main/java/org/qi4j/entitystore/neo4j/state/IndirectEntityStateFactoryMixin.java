/* Copyright 2008 Neo Technology, http://neotechnology.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.qi4j.entitystore.neo4j.state;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.neo4j.api.core.Direction;
import org.neo4j.api.core.Node;
import org.neo4j.api.core.Transaction;
import org.qi4j.api.entity.EntityReference;
import org.qi4j.api.injection.scope.Service;
import org.qi4j.entitystore.neo4j.NeoCoreService;
import org.qi4j.entitystore.neo4j.NeoIdentityIndex;
import org.qi4j.entitystore.neo4j.NeoTransactionService;
import org.qi4j.spi.entity.EntityStatus;
import org.qi4j.spi.entity.EntityStoreException;
import org.qi4j.spi.entity.StateCommitter;

public class IndirectEntityStateFactoryMixin implements NeoEntityStateFactory
{
    private
    @Service
    DirectEntityStateFactory directFactory;
    private
    @Service
    NeoTransactionService txFactory;
    private
    @Service
    NeoCoreService neo;

    public CommittableEntityState createEntityState(NeoIdentityIndex idIndex, LoadedDescriptor descriptor, EntityReference reference, EntityStatus status)
    {
        Transaction tx = txFactory.beginTx();
        try
        {
            Node node = idIndex.getNode(reference.identity());
            if (node == null)
            {
                node = UncreatedNode.getNode(reference.identity(), neo);
            }
            CommittableEntityState state = null; // new IndirectEntityState((DirectEntityState) directFactory.createEntityState(idIndex, node, descriptor, reference, status));
            state.preloadState();
            return state;
        }
        finally
        {
            tx.finish();
        }
    }

    public StateCommitter prepareCommit(NeoIdentityIndex idIndex, Iterable<CommittableEntityState> updated, Iterable<EntityReference> removed) throws EntityStoreException
    {
        final Transaction tx = txFactory.beginTx();
        try
        {
            for (CommittableEntityState state : updated)
            {
                state.prepareState();
            }
            for (CommittableEntityState state : updated)
            {
                state.prepareCommit();
            }
            List<Node> removedNodes = new LinkedList<Node>();
            for (EntityReference removedId : removed)
            {
                Node removedNode = idIndex.getNode(removedId.identity());
                CommittableEntityState entity = directFactory.loadEntityStateFromNode(idIndex, removedNode);
                entity.prepareRemove();
                removedNodes.add(removedNode);
            }
            for (Node removedNode : removedNodes)
            {
                if (removedNode.getRelationships(Direction.INCOMING).iterator().hasNext())
                {
                    throw new EntityStoreException(
                            "Cannot remove " + DirectEntityState.getIdentityFromNode(removedNode).identity() +
                                    ", it has incoming references.");
                }
                removedNode.delete();
            }
            return new StateCommitter()
            {
                public void commit()
                {
                    tx.success();
                    tx.finish();
                }

                public void cancel()
                {
                    tx.failure();
                    tx.finish();
                }
            };
        }
        catch (EntityStoreException ex)
        {
            tx.failure();
            tx.finish();
            throw ex;
        }
        catch (Exception ex)
        {
            tx.failure();
            tx.finish();
            throw new EntityStoreException(ex);
        }
    }

    public Iterator<CommittableEntityState> iterator(NeoIdentityIndex idIndex)
    {
        final Iterator<CommittableEntityState> direct = directFactory.iterator(idIndex);
        return new Iterator<CommittableEntityState>()
        {
            public boolean hasNext()
            {
                return direct.hasNext();
            }

            public CommittableEntityState next()
            {
                return null; // new IndirectEntityState((DirectEntityState) direct.next());
            }

            public void remove()
            {
                direct.remove();
            }
        };
    }
}
