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

import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.Node;
import jenkins.model.Jenkins;

import java.util.Iterator;
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
    @SuppressWarnings("unchecked")
    public static NodeIterator<Node> iterator() {
        return new MetaNodeIterator(Jenkins.getInstance().getExtensionList(NodeIterator.class).iterator(), Node.class);
    }

    /**
     * Adapter to allow easy use from Java 5+ for loops.
     *
     * @return an {@link Iterable}.
     */
    public static Iterable<Node> nodes() {
        return NodeIterable.ResourceHolder.INSTANCE;
    }

    /**
     * Returns a new iterator of all the {@link Node}s in the system.
     *
     * @return a new iterator of all the {@link Node}s in the system.
     */
    @SuppressWarnings("unchecked")
    public static <N extends Node> NodeIterator<N> iterator(Class<N> nodeClass) {
        return new MetaNodeIterator(Jenkins.getInstance().getExtensionList(NodeIterator.class).iterator(), nodeClass);
    }

    /**
     * Adapter to allow easy use from Java 5+ for loops.
     *
     * @return an {@link Iterable}.
     */
    @SuppressWarnings("unchecked")
    public static <N extends Node> Iterable<N> nodes(Class<N> nodeClass) {
        return nodeClass.equals(Node.class) ? nodes() : new NodeIterable(nodeClass);
    }

    /**
     * The internal iterator that loops through all the known NodeIterator extensions.
     */
    private static class MetaNodeIterator<N extends Node> extends NodeIterator<N> {

        private final Class<N> nodeClass;

        /**
         * The current delegate.
         */
        private Iterator<? extends Node> delegate;

        /**
         * The next delegate.
         */
        private final Iterator<NodeIterator<? extends Node>> metaIterator;

        private N next;

        /**
         * Constructs the iterator.
         *
         * @param metaIterator the iterator of iterators.
         */
        public MetaNodeIterator(Iterator<NodeIterator<? extends Node>> metaIterator, Class<N> nodeClass) {
            this.delegate = Jenkins.getInstance().getNodes().iterator();
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
                    Node next = delegate.next();
                    if (nodeClass.isInstance(next)) {
                        this.next = nodeClass.cast(next);
                        return true;
                    }
                }
                while (metaIterator.hasNext()) {
                    delegate = metaIterator.next();
                    if (delegate.hasNext()) {
                        return true;
                    }
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

        private Class<N> nodeClass;

        /**
         * Lazy singleton pattern.
         */
        private static final class ResourceHolder {
            /**
             * Because this always routes through {@link Jenkins#getInstance()} maintaining a class-level singleton is
             * OK.
             */
            private static final NodeIterable<Node> INSTANCE = new NodeIterator.NodeIterable<Node>(Node.class);
        }

        private NodeIterable(Class<N> nodeClass) {
            this.nodeClass = nodeClass;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<N> iterator() {
            return NodeIterator.iterator(nodeClass);
        }
    }
}
