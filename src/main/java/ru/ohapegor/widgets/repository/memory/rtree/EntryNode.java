package ru.ohapegor.widgets.repository.memory.rtree;

import lombok.Getter;
import lombok.Setter;
import ru.ohapegor.widgets.model.HasId;

@Getter
@Setter
public class EntryNode<E extends HasId> extends Node<E> {
    private E entry;
    private TreeNode<E> parent;

    public EntryNode(E entry) {
        this.entry = entry;
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
