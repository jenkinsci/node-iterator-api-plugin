/*
 * The MIT License
 *
 * Copyright (c) 2011-2013, CloudBees, Inc..
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.slaves.iterators.api;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Node;
import jenkins.model.Jenkins;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A class that iterates through all the {@link Node}s in the system, even nodes which are not attached to the main
 * {@link Jenkins} object. If you are holding onto nodes that are not attached to the main {@link Jenkins} object
 * then you need to have an {@link Extension} which extends this class and can iterate through your {@link Node}s.
 *
 * @author Stephen Connolly
 */
public abstract class NodeIterator<N extends Node> implements Iterator<N>, ExtensionPoint {

    /**
     * Returns a new iterator of all the {@link Node}s in the system.
     *
     * @return a new iterator of all the {@link Node}s in the system.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @NonNull
    public static NodeIterator<Node> iterator() {
        final Jenkins instance = Jenkins.getInstanceOrNull();
        return instance == null
                ? new MetaNodeIterator(Collections.emptyIterator(), Node.class)
                : new MetaNodeIterator(instance.getExtensionList(NodeIterator.class).iterator(), Node.class);
    }

    /**
     * Adapter to allow easy use from Java 5+ for loops.
     *
     * @return an {@link Iterable}.
     */
    @NonNull
    public static Iterable<Node> nodes() {
        return NodeIterable.ResourceHolder.INSTANCE;
    }

    /**
     * Returns a new iterator of all the {@link Node}s in the system.
     *
     * @param nodeClass the type of {@link Node}
     * @param <N> the class type of node
     *
     * @return a new iterator of all the {@link Node}s in the system.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @NonNull
    public static <N extends Node> NodeIterator<N> iterator(@NonNull Class<N> nodeClass) {
        nodeClass.getClass(); // throw NPE if null
        final Jenkins instance = Jenkins.getInstanceOrNull();
        return instance == null
                ? new MetaNodeIterator(Collections.emptyIterator(), nodeClass)
                : new MetaNodeIterator(instance.getExtensionList(NodeIterator.class).iterator(), nodeClass);
    }

    /**
     * Adapter to allow easy use from Java 5+ for loops.
     * If attempting to get all nodes use {@link NodeIterator#nodes()}
     *
     * @param nodeClass the type of {@link Node}
     * @param <N> the class type of node
     *
     * @return an {@link Iterable}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @NonNull
    public static <N extends Node> Iterable<N> nodes(@NonNull Class<N> nodeClass) {
        nodeClass.getClass(); // throw NPE if null
        return new NodeIterable(nodeClass);
    }

    /**
     * Returns {@code true} if and only if {@link jenkins.slaves.iterators.api.NodeIterator#iterator()} will iterate
     * all live instances of {@code Node}. This is useful if you want to resolve any backing resources that do not
     * have a corresponding {@link Node} instance. If this method returns {@code false} then it will not be possible
     * to definitively determine the complete live set and hence it will not be possible to definitively identify
     * unused backing resources.
     *
     * @return {@code true} if and only if {@link jenkins.slaves.iterators.api.NodeIterator#iterator()} will iterate
     *         all live instances of {@code Node}.
     * @since 1.2
     */
    public static boolean isComplete() {
        return isComplete(Node.class);
    }

    /**
     * Returns {@code true} if and only if {@link jenkins.slaves.iterators.api.NodeIterator#iterator()} will iterate
     * all live instances of the specified subtype of {@link Node}. This is useful if you want to resolve any backing
     * resources that do not have a corresponding {@link Node} instance. If this method returns {@code false} then it
     * will not be possible to definitively determine the complete live set and hence it will not be possible to
     * definitively identify unused backing resources.
     *
     * @param nodeClass the type of {@link Node}
     * @param <N> the class type of node
     *
     * @return {@code true} if and only if {@link jenkins.slaves.iterators.api.NodeIterator#iterator()} will iterate
     *         all live instances of {@code Node}.
     * @since 1.2
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <N extends Node> boolean isComplete(@NonNull Class<N> nodeClass) {
        nodeClass.getClass(); // throw NPE if null
        final Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance == null) {
            return false;
        }
        for (NodeIterator iterator : instance.getExtensionList(NodeIterator.class)) {
            if (!iterator.hasCompleteLiveSet(nodeClass)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Implementers of {@link NodeIterator} should override this method if they "lend" {@link Node} instances to
     * other JVMs and they require an on-line connection to those JVMs in order to iterate the {@link Node} instances
     * that have been "lent". Where an implementer only "lends" a specific sub-type of {@link Node} it should return
     * {@code true} if the specified node type is not a sub-type of the type of {@link Node} that it leases.
     * Implementers should only return {@code false} when the type of {@link Node} they "lend" is a sub-type of
     * the specified type and at least one of their connections to remote JVMs is off-line in order to allow
     * clients of the {@link NodeIterator} API to identify unused backing resources.
     *
     * @param nodeClass the sub-type of {@link Node} that is being queried.
     * @param <N>       the sub-type of {@link Node} that is being queried.
     * @return {@code false} if and only if this {@link NodeIterator} cannot currently return the full set of
     *         {@link Node} instances of the specified type as a result of a transient loss of remote connectivity
     * @since 1.2
     */
    protected <N extends Node> boolean hasCompleteLiveSet(@NonNull Class<N> nodeClass) {
        return true;
    }

    /**
     * The internal iterator that loops through all the known NodeIterator extensions.
     */
    private static class MetaNodeIterator<N extends Node> extends NodeIterator<N> {

        /**
         * The type of {@link Node} that we are iterating.
         */
        @NonNull
        private final Class<N> nodeClass;
        /**
         * The next delegate.
         */
        @NonNull
        private final Iterator<NodeIterator<? extends Node>> metaIterator;
        /**
         * The current delegate.
         */
        @CheckForNull
        private Iterator<? extends Node> delegate;
        /**
         * The next node.
         */
        @CheckForNull
        private N next;

        /**
         * Constructs the iterator.
         *
         * @param metaIterator the iterator of iterators.
         * @param nodeClass    the type of {@link Node} that we are iterating.
         */
        MetaNodeIterator(@NonNull Iterator<NodeIterator<? extends Node>> metaIterator,
                         @NonNull Class<N> nodeClass) {
            metaIterator.getClass(); // throw NPE if null
            nodeClass.getClass(); // throw NPE if null
            final Jenkins instance = Jenkins.getInstanceOrNull();
            if (instance == null) { // during startup
                delegate = Collections.emptyIterator();
            } else {
                List<Node> nodes = instance.getNodes();
                delegate = nodes == null ? Collections.<Node>emptyIterator() : nodes.iterator();
            }
            this.metaIterator = metaIterator;
            this.nodeClass = nodeClass;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            while ((delegate != null && delegate.hasNext()) || metaIterator.hasNext()) {
                while (delegate != null && delegate.hasNext()) {
                    Node _next = delegate.next();
                    if (nodeClass.isInstance(_next)) {
                        this.next = nodeClass.cast(_next);
                        return true;
                    }
                }
                while (metaIterator.hasNext() && (delegate == null || !delegate.hasNext())) {
                    delegate = metaIterator.next();
                }
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public N next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            try {
                return next;
            } finally {
                next = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Adapter class to help with working with Java 5+ for loops.
     */
    private static class NodeIterable<N extends Node> implements Iterable<N> {

        /**
         * The type of {@link Node} we are iterating.
         */
        @NonNull
        private final Class<N> nodeClass;

        /**
         * Constructor.
         *
         * @param nodeClass the type of {@link Node} to iterate.
         */
        private NodeIterable(@NonNull Class<N> nodeClass) {
            this.nodeClass = nodeClass;
        }

        /**
         * {@inheritDoc}
         */
        @NonNull
        public Iterator<N> iterator() {
            return NodeIterator.iterator(nodeClass);
        }

        /**
         * Lazy singleton pattern.
         */
        private static final class ResourceHolder {
            /**
             * Because this always routes through {@link Jenkins#getInstance()} maintaining a class-level singleton is
             * OK.
             */
            private static final NodeIterable<Node> INSTANCE = new NodeIterator.NodeIterable<Node>(Node.class);

            private ResourceHolder() {
            }
        }
    }
}
