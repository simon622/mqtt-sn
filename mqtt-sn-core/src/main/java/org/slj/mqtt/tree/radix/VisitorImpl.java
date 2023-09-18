package org.slj.mqtt.tree.radix;


/**
 * A simple standard implementation for a {@link visitor}.
 *
 * @author Dennis Heidsiek
 * @param <T,R>
 */
public abstract class VisitorImpl<T, R> implements Visitor<T, R> {

    protected R result;

    public VisitorImpl() {
        this.result = null;
    }

    public VisitorImpl(R initialValue) {
        this.result = initialValue;
    }

    public R getResult() {
        return result;
    }

    abstract public void visit(String key, RadixTreeNode<T> parent, RadixTreeNode<T> node);
}