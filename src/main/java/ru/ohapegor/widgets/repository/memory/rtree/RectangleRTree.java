package ru.ohapegor.widgets.repository.memory.rtree;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.ohapegor.widgets.model.HasId;
import ru.ohapegor.widgets.model.Rectangle;
import ru.ohapegor.widgets.model.SearchArea;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class RectangleRTree<E extends HasId> {

    private final int minEntries;
    private final int maxEntries;

    private TreeNode<E> root;


    public RectangleRTree(int minNodes, int maxNodes) {
        this.minEntries = minNodes;
        this.maxEntries = maxNodes;
    }

    public List<E> search(SearchArea searchArea) {
        if (root == null) {
            return Collections.emptyList();
        }
        List<E> results = new LinkedList<>();
        search(searchArea, root, results);
        return results;
    }

    public void insert(EntryNode<E> entryNode) {
        if (root == null) {
            root = new TreeNode<>(true);
            root.addChild(entryNode);
            return;
        }
        TreeNode<E> leaf = chooseLeaf(root, entryNode);
        leaf.addChild(entryNode);
        if (leaf.getChildNodes().size() > maxEntries) {
            TreeNode<E> newNode = splitNode(leaf);
            adjustTree(leaf, newNode);
        } else {
            adjustTree(leaf, null);
        }
    }

    public boolean deleteEntry(String id, Rectangle dimensions) {
        Objects.requireNonNull(id, "null id");
        Objects.requireNonNull(id, "null dimensions");

        Optional<EntryNode<E>> entryNodeOpt = findEntry(root, id, dimensions);
        if (entryNodeOpt.isEmpty()) {
            return false;
        }
        EntryNode<E> entryNode = entryNodeOpt.get();
        if (entryNode.getParent() == null) {
            throw new IllegalStateException("found entry with null parent by id = " + id);
        }
        TreeNode<E> parent = entryNode.getParent();
        if (!parent.removeChild(entryNode)) {
            throw new IllegalStateException(">> failed to delete entryNode=" + entryNode + " from parent " + parent);
        }

        condenseTree(parent);

        return true;
    }

    private void condenseTree(TreeNode<E> node) {
        List<EntryNode<E>> nodesToReinsert = new LinkedList<>();
            while (node != root) {
                if (node.getChildNodes().size() < minEntries) {
                    extractAllEntries(node, nodesToReinsert);
                    TreeNode<E> parent = node.getParent();
                    parent.removeChild(node);
                    node = parent;
                } else {
                    tightenDimensions(node);
                    node = node.getParent();
                }
            }
            nodesToReinsert.forEach(this::insert);
    }

    private void extractAllEntries(TreeNode<E> node, List<EntryNode<E>> collector) {
        if (node.isLeaf()) {
            for (Node<E> entry : node.getChildNodes()) {
                EntryNode<E> entryNode = (EntryNode<E>) entry;
                collector.add(entryNode);
            }
        } else {
            for (Node<E> child : node.getChildNodes()) {
                TreeNode<E> treeNode = (TreeNode<E>) child;
                extractAllEntries(treeNode, collector);
            }
        }
    }

    private Optional<EntryNode<E>> findEntry(TreeNode<E> node, String id, Rectangle rectangle) {
        for (Node<E> child : node.getChildNodes()) {
            if (child instanceof TreeNode) {
                if (isOverlap(child, rectangle)) {
                    TreeNode<E> treeNode = (TreeNode<E>) child;
                    Optional<EntryNode<E>> entryNodeOpt = findEntry(treeNode, id, rectangle);
                    if (entryNodeOpt.isPresent()) {
                        return entryNodeOpt;
                    }
                }
            } else if (child instanceof EntryNode) {
                EntryNode<E> entryNode = (EntryNode<E>) child;
                String entryId = entryNode.getEntry().getId();
                if (id.equals(entryId)) {
                    return Optional.of(entryNode);
                }
            }
        }
        return Optional.empty();
    }

    private boolean isOverlap(Rectangle first, Rectangle second) {
        return first.getMinX() < second.getMaxX() && first.getMaxX() > second.getMinX() //overlap by X
                && first.getMinY() < second.getMaxY() && first.getMaxY() > second.getMinY(); //overlap by Y
    }

    private TreeNode<E> splitNode(TreeNode<E> origNode) {
        var seeds = linearPeekSeeds(origNode);
        if (!origNode.removeChild(seeds.lowestNode)) {
            throw new IllegalStateException("remove lowestNode failed");
        }
        if (!origNode.removeChild(seeds.highestNode)) {
            throw new IllegalStateException("remove lowestNode failed");
        }

        LinkedList<Node<E>> childrenToRearrange = new LinkedList<>(origNode.getChildNodes());
        origNode.getChildNodes().clear();

        TreeNode<E> parent = origNode.parent;
        if (origNode == root) {
            root = new TreeNode<>(false);
            parent = root;
            parent.addChild(origNode);
        }

        TreeNode<E> newNode = new TreeNode<>(origNode.isLeaf());
        parent.addChild(newNode);

        origNode.addChild(seeds.lowestNode);
        newNode.addChild(seeds.highestNode);

        tightenDimensions(origNode);
        tightenDimensions(newNode);

        while (!childrenToRearrange.isEmpty()) {
            int remainingSize = childrenToRearrange.size();
            if (shouldAddAllRemainingToMatchMinSize(origNode, remainingSize)) {
                childrenToRearrange.forEach(origNode::addChild);
                tightenDimensions(origNode);
                break;
            }
            if (shouldAddAllRemainingToMatchMinSize(newNode, remainingSize)) {
                childrenToRearrange.forEach(newNode::addChild);
                tightenDimensions(newNode);
                break;
            }
            Node<E> child = childrenToRearrange.remove();
            TreeNode<E> preferredNode = choosePreferredNode(origNode, newNode, child);
            preferredNode.addChild(child);
            tightenDimensions(preferredNode);
        }
        return newNode;
    }

    private TreeNode<E> choosePreferredNode(TreeNode<E> first, TreeNode<E> second, Rectangle insertingNode) {
        BigDecimal enlargement1 = calculateRequiredEnlargement(first, insertingNode);
        BigDecimal enlargement2 = calculateRequiredEnlargement(second, insertingNode);
        int comparison = enlargement1.compareTo(enlargement2);
        if (comparison < 0) {
            return first;
        } else if (comparison > 0) {
            return second;
        }
        return first.getChildNodes().size() < second.getChildNodes().size() ? first : second;
    }

    private boolean shouldAddAllRemainingToMatchMinSize(TreeNode<E> node, int remainingSize) {
        return remainingSize <= minEntries && node.getChildNodes().size() + remainingSize == minEntries;
    }

    private SeparationByCoordinateResult<E> linearPeekSeeds(TreeNode<E> node) {
        var separationByX = separationByX(node, new LinkedList<>());
        var separationByY = separationByY(node, new LinkedList<>());
        return separationByX.normalizedSeparation > separationByY.normalizedSeparation ? separationByX : separationByY;
    }

    private SeparationByCoordinateResult<E> separationByX(TreeNode<E> node, List<Node<E>> excludeNodes) {
        if (node.getChildNodes().size() - excludeNodes.size() < 2) {
            log.error("unable to resolve separationByX collision for node : {}", node);
            throw new IllegalStateException("unable to resolve separationByX collision");
        }
        Node<E> lowestHighSideNode = null;
        Node<E> highestLowSideNode = null;
        int highestSide = Integer.MIN_VALUE, highestLowSide = Integer.MIN_VALUE;
        int lowestSide = Integer.MAX_VALUE, lowestHighSide = Integer.MAX_VALUE;
        for (Node<E> child : node.getChildNodes()) {
            if (excludeNodes.contains(child)) {
                continue;
            }
            if (child.getMinX() > highestLowSide) {
                highestLowSide = child.getMinX();
                highestLowSideNode = child;
            }
            if (child.getMaxX() < lowestHighSide) {
                lowestHighSide = child.getMaxX();
                lowestHighSideNode = child;
            }
            if (child.getMaxX() > highestSide) {
                highestSide = child.getMaxX();
            }
            if (child.getMinX() < lowestSide) {
                lowestSide = child.getMinX();
            }
        }
        if (lowestHighSideNode == highestLowSideNode) {
            log.warn("separationByX collision \nnode={} \nhighestSide={} \nhighestLowSide={} \nlowestSide={} \nlowestHighSide={} \namong: {}",
                    highestLowSideNode, highestSide, highestLowSide, lowestSide, lowestHighSide,
                    node.getChildNodes().stream().map(Node::toString).collect(Collectors.joining("\n")));
            excludeNodes.add(lowestHighSideNode);
            return separationByX(node, excludeNodes);
        }
        double separation = (highestLowSide - lowestHighSide) * 1.0 / (highestSide - lowestSide);
        return new SeparationByCoordinateResult<>(separation, lowestHighSideNode, highestLowSideNode);
    }

    private SeparationByCoordinateResult<E> separationByY(TreeNode<E> node, List<Node<E>> excludeNodes) {
        if (node.getChildNodes().size() - excludeNodes.size() < 2) {
            log.error("unable to resolve separationByX collision for node : {}", node);
            throw new IllegalStateException("unable to resolve separationByX collision");
        }
        Node<E> lowestHighSideNode = null;
        Node<E> highestLowSideNode = null;
        int highestSide = Integer.MIN_VALUE, highestLowSide = Integer.MIN_VALUE;
        int lowestSide = Integer.MAX_VALUE, lowestHighSide = Integer.MAX_VALUE;
        for (Node<E> child : node.getChildNodes()) {
            if (excludeNodes.contains(child)) {
                continue;
            }
            if (child.getMinY() > highestLowSide) {
                highestLowSide = child.getMinY();
                highestLowSideNode = child;
            }
            if (child.getMaxY() < lowestHighSide) {
                lowestHighSide = child.getMaxY();
                lowestHighSideNode = child;
            }
            if (child.getMaxY() > highestSide) {
                highestSide = child.getMaxY();
            }
            if (child.getMinY() < lowestSide) {
                lowestSide = child.getMinY();
            }
        }
        if (lowestHighSideNode == highestLowSideNode) {
            log.error("separationByY collision \nnode={} \nhighestSide={} \nhighestLowSide={} \nlowestSide={} \nlowestHighSide={} \namong: {}",
                    lowestHighSideNode, highestSide, highestLowSide, lowestSide, lowestHighSide,
                    node.getChildNodes().stream().map(Node::toString).collect(Collectors.joining("\n")));
            excludeNodes.add(lowestHighSideNode);
            return separationByX(node, excludeNodes);
        }
        double separation = (highestLowSide - lowestHighSide) * 1.0 / (highestSide - lowestSide);
        return new SeparationByCoordinateResult<>(separation, lowestHighSideNode, highestLowSideNode);
    }

    private void adjustTree(TreeNode<E> first, TreeNode<E> second) {
        tightenDimensions(first);
        if (second != null) {
            tightenDimensions(second);
        }
        if (first == root) {
            if (second != null) {
                root = new TreeNode<E>(false);
                root.addChild(first);
                root.addChild(second);
                tightenDimensions(root);
            }
            return;
        }
        if (second != null) {
            if (first.getParent().getChildNodes().size() > maxEntries) {
                TreeNode<E> newNode = splitNode(first.getParent());
                adjustTree(first.getParent(), newNode);
            }
        } else {
            adjustTree(first.getParent(), null);
        }
    }

    private void tightenDimensions(TreeNode<E> node) {
        Iterator<Node<E>> childIterator = node.getChildNodes().iterator();
        if (!childIterator.hasNext()) {
            return;
        }
        Rectangle child = childIterator.next();
        int minX = child.getMinX();
        int maxX = child.getMaxX();
        int minY = child.getMinY();
        int maxY = child.getMaxY();
        while (childIterator.hasNext()) {
            child = childIterator.next();
            minX = Math.min(minX, child.getMinX());
            maxX = Math.max(maxX, child.getMaxX());
            minY = Math.min(minY, child.getMinY());
            maxY = Math.max(maxY, child.getMaxY());
        }
        node.setX(minX);
        node.setY(minY);
        node.setWidth(maxX - minX);
        node.setHeight(maxY - minY);
    }

    private TreeNode<E> chooseLeaf(TreeNode<E> node, Rectangle entryDimensions) {
        if (node.isLeaf()) {
            return node;
        }
        BigDecimal minEnlargement = null;
        Node<E> minEnlargementNode = null;
        for (Node<E> child : node.getChildNodes()) {
            BigDecimal enlargement = calculateRequiredEnlargement(child, entryDimensions);
            if (minEnlargement == null || enlargement.compareTo(minEnlargement) < 0) {
                minEnlargement = enlargement;
                minEnlargementNode = child;
            }
        }
        TreeNode<E> chosenNode = (TreeNode<E>) minEnlargementNode;
        return chooseLeaf(chosenNode, entryDimensions);
    }

    private BigDecimal calculateRequiredEnlargement(Rectangle nodeDimensions, Rectangle entryDimensions) {
        BigDecimal initialArea = nodeDimensions.getArea();

        int minX = Math.min(nodeDimensions.getMinX(), entryDimensions.getMinX());
        int minY = Math.min(nodeDimensions.getMinY(), entryDimensions.getMinY());
        int maxX = Math.max(nodeDimensions.getMaxX(), entryDimensions.getMaxX());
        int maxY = Math.max(nodeDimensions.getMaxY(), entryDimensions.getMaxY());

        BigDecimal resultHeight = BigDecimal.valueOf(maxY - minY);
        BigDecimal resultWidth = BigDecimal.valueOf(maxX - minX);

        BigDecimal resultArea = resultHeight.multiply(resultWidth);

        return resultArea.min(initialArea);
    }

    private void search(SearchArea searchArea, TreeNode<E> node, List<E> results) {
        if (node.isLeaf()) {
            for (Node<E> child : node.getChildNodes()) {
                if (!(child instanceof EntryNode)) {
                    throw new IllegalStateException("leaf node can contain only entries");
                }
                if (searchArea.includes(child)) {
                    EntryNode<E> entryNode = (EntryNode<E>) child;
                    results.add(entryNode.getEntry());
                }
            }
            return;
        }
        if (node.getChildNodes() != null) {
            node.getChildNodes().stream()
                    .filter(searchArea::isOverlap)
                    .forEach(childNode -> search(searchArea, (TreeNode<E>) childNode, results));
        }
    }

    public void clear() {
        root = null;
    }

    @Data
    @AllArgsConstructor
    private static class SeparationByCoordinateResult<E extends HasId> {
        private double normalizedSeparation;
        private Node<E> lowestNode;
        private Node<E> highestNode;
    }

}
