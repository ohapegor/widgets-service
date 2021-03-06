package ru.ohapegor.widgets.repository.memory.rtree;

import lombok.Getter;
import lombok.Setter;
import ru.ohapegor.widgets.model.HasId;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * R-Tree node which holds array of another tree nodes {@link Node}.
 *
 */
@Getter
@Setter
class TreeNode<E extends HasId> extends Node<E> {

    /**
     * if leaf property equals true - only {@link EntryNode} can be a child of this node
     * if leaf property equals false - only {@link TreeNode} can be a child of this node
     */
    private boolean leaf;
    private final List<Node<E>> childNodes;

    TreeNode(boolean isLeaf) {
        this.leaf = isLeaf;
        this.childNodes = new LinkedList<>();
    }

    void addChild(Node<E> node) {
        childNodes.add(node);
        node.setParent(this);
    }

    boolean removeChild(Node<E> node) {
        node.setParent(null);
        return childNodes.remove(node);
    }

    @Override
    public String toString() {
        return "TreeNode{" +
                "minX=" + getMinX() +
                ", maxX=" + getMaxX() +
                ", minY=" + getMinY() +
                ", maxY=" + getMaxY() +
                ", leaf=" + leaf +
                ", childNodes=\n" + childNodes.stream().map(Node::toString).collect(Collectors.joining("\n")) +
                '}';
    }
}
