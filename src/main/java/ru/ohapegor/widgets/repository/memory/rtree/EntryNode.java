package ru.ohapegor.widgets.repository.memory.rtree;

import lombok.Getter;
import lombok.Setter;
import ru.ohapegor.widgets.model.HasId;

import java.util.Objects;

/**
 * R-Tree node which holds link to entry object, link to parent node and has coordinates.
 * Can be a child of leaf nodes only ({@link TreeNode} with leaf == true).
 */
@Getter
@Setter
public class EntryNode<E extends HasId> extends Node<E> {
    private E entry;

    public EntryNode(E entry) {
        this.entry = Objects.requireNonNull(entry, "EntryNode should contain non-null entry");
    }

    @Override
    public int hashCode() {
        return entry.getId().hashCode();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object another) {
        if (another == null) {
            return false;
        }
        if (!(another instanceof EntryNode)) {
            return false;
        }
        EntryNode<E> anotherEntry = (EntryNode<E>) another;

        return Objects.equals(entry.getId(), anotherEntry.getEntry().getId());
    }

    @Override
    public String toString() {
        return "EntryNode{" +
                "minX=" + getMinX() +
                ", maxX=" + getMaxX() +
                ", minY=" + getMinY() +
                ", maxY=" + getMaxY() +
                ", entry=" + entry +
                '}';
    }
}
