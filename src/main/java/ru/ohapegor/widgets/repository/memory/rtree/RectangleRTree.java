package ru.ohapegor.widgets.repository.memory.rtree;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import ru.ohapegor.widgets.model.HasId;
import ru.ohapegor.widgets.model.Rectangle;
import ru.ohapegor.widgets.model.SearchArea;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of r-tree described by Antonin Guttman
 * original article can be found by link <a href="http://www-db.deis.unibo.it/courses/SI-LS/papers/Gut84.pdf">r-tree.pdf</a>
 *
 * @param <E> - type of object (entry) which is actually will be stored in this tree.
 */
@Slf4j
public class RectangleRTree<E extends HasId> {

    /**
     * minimum count of nodes one {@link TreeNode} can hold in its childNodes list,
     * except it is only one node (parent) in r-tree (tree size < minNodes)
     */
    private final int minEntries;

    /**
     * maximum count of nodes one {@link TreeNode} can hold in its childNodes list
     */
    private final int maxEntries;


    private TreeNode<E> root;

    /**
     * Creates a new R-tree
     *
     * @param minEntries - minimum count of children in one node
     * @param maxEntries - maximum count of children in one node
     */
    public RectangleRTree(int minEntries, int maxEntries) {
        if (minEntries < 1) {
            throw new IllegalArgumentException("minNodes must be grater than 0");
        }
        if (maxEntries <= minEntries) {
            throw new IllegalArgumentException("maxNodes must be grater than minNodes");
        }
        this.minEntries = minEntries;
        this.maxEntries = maxEntries;
    }

    /**
     * Get list of entries that includes inside of dimensions of search area ({@param searchArea}).
     * Search area can be open ( any of {@param searchArea} borders can be null).
     *
     * @param searchArea - object which represents search area.
     * @return list of entries or empty list
     */
    public List<E> search(SearchArea searchArea) {
        Objects.requireNonNull(searchArea, "searchArea can't be null");
        if (root == null) {
            return Collections.emptyList();
        }
        List<E> results = new LinkedList<>();
        search(searchArea, root, results);
        return results;
    }

    /**
     * Inserts entry node inside tree.
     * Algorithm:
     * 1. Find position for new record (Invoke {@link RectangleRTree#chooseLeaf} to select a leafNode)
     * 2. If leaf has room for another entry, install entry. Otherwise, invoke {@link RectangleRTree#splitNode}
     * to obtain newLeafNode and redistribute all the old entries of leafNode and inserting entry between leafNode and newLeafNode.
     * 3. Invoke {@link RectangleRTree#adjustTree} on leafNode also passing newLeafNode if a split was performed.
     *
     * @param entryNode - entry node with reference to storing object and dimensions
     */
    public void insert(EntryNode<E> entryNode) {
        if (root == null) {
            root = new TreeNode<>(true);
            root.addChild(entryNode);
            return;
        }
        TreeNode<E> leafNode = chooseLeaf(root, entryNode);
        leafNode.addChild(entryNode);
        if (leafNode.getChildNodes().size() > maxEntries) {
            //if node children count exceeded max of children, then we should
            TreeNode<E> newLeafNode = splitNode(leafNode);
            adjustTree(leafNode, newLeafNode);
        } else {
            adjustTree(leafNode, null);
        }
    }

    /**
     * Remove index record from an R-tree.
     * Algorithm:
     * 1. [Find node containing record] Invoke {@link RectangleRTree#findEntry} to find entryNode, containing entry.
     * Stop if not found.
     * 2. [Delete record] Remove entry node from its parent.
     * 3. [Propagate changes] Invoke {@link RectangleRTree#condenseTree} passing leaf node, in which deleted entry has been contained.
     *
     * @param id         - id of deleting entry
     * @param dimensions - rectangle where this entry expected to be.
     * @return if entry is actually was deleted
     */
    public boolean deleteEntry(String id, Rectangle dimensions) {
        Objects.requireNonNull(id, "null id");
        Objects.requireNonNull(id, "null dimensions");

        Optional<EntryNode<E>> entryNodeOpt = findEntry(root, id, dimensions); //1
        if (entryNodeOpt.isEmpty()) {
            return false;
        }
        EntryNode<E> entryNode = entryNodeOpt.get();
        if (entryNode.getParent() == null) {
            throw new IllegalStateException("found entry with null parent by id = " + id);
        }
        TreeNode<E> parent = entryNode.getParent();
        if (!parent.removeChild(entryNode)) { //2
            log.error("failed to delete entryNode={}  from parent={}", entryNode, parent);
            throw new IllegalStateException("failed to delete entryNode");
        }

        condenseTree(parent); //3

        return true;
    }

    /**
     * Given a leafNode from which entry has been deleted, eliminate node if it has too few entries and relocate
     * its entries. Propagate node elimination upward as necessary. Adjust all covering rectangles on the path
     * to the root, making them smaller if possible.
     * 1. [Initialize] Set list of eliminated nodes to be empty.
     * 2. [Eliminate under-full node] If node has fewer than minEntries delete node from its parent and add its entries to list.
     * 3. [Adjust covering rectangle] If node has not been eliminated adjust its dimensions to tightly contain
     * all child rectangles.
     * 4. [Move up one level in tree] Set node as its parent and repeat from 2.
     * 5. [Check if root becomes leaf] Set root as leaf if all nodes has been removed from non leaf root.
     * 6. [Reinsert orphaned entries]
     */
    private void condenseTree(TreeNode<E> node) {
        List<EntryNode<E>> nodesToReinsert = new LinkedList<>(); //1
        while (node != root) {
            if (node.getChildNodes().size() < minEntries) { //2
                extractAllEntries(node, nodesToReinsert);
                TreeNode<E> parent = node.getParent();
                parent.removeChild(node);
                node = parent; //4
            } else {
                tightenDimensions(node); //3
                node = node.getParent(); //4
            }
        }
        if (!root.isLeaf() && root.getChildNodes().size() == 0) {
            root = new TreeNode<>(true);
        }
        nodesToReinsert.forEach(this::insert); //5
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
        Node<E> lowestHighSideNode = null;
        Node<E> highestLowSideNode = null;
        int highestSide = Integer.MIN_VALUE, highestLowSide = Integer.MIN_VALUE;
        int lowestSide = Integer.MAX_VALUE, lowestHighSide = Integer.MAX_VALUE;
        if (node.getChildNodes().size() - excludeNodes.size() >= minEntries) {
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
        } else {
            log.warn("unable to resolve separationByX collision for node : {}, returning just first and last nodes", node);
            lowestHighSideNode = node.getChildNodes().get(0);
            lowestHighSide = lowestHighSideNode.getMaxX();
            highestLowSideNode = node.getChildNodes().get(node.getChildNodes().size() - 1);
            highestLowSide = highestLowSideNode.getMinX();
            highestSide = node.getChildNodes().stream().map(Node::getMaxX).max(Comparator.naturalOrder()).get();
            lowestSide = node.getChildNodes().stream().map(Node::getMinX).min(Comparator.naturalOrder()).get();
        }
        double separation = (highestLowSide - lowestHighSide) * 1.0 / (highestSide - lowestSide);
        return new SeparationByCoordinateResult<>(separation, lowestHighSideNode, highestLowSideNode);
    }

    private SeparationByCoordinateResult<E> separationByY(TreeNode<E> node, List<Node<E>> excludeNodes) {
        Node<E> lowestHighSideNode = null;
        Node<E> highestLowSideNode = null;
        int highestSide = Integer.MIN_VALUE, highestLowSide = Integer.MIN_VALUE;
        int lowestSide = Integer.MAX_VALUE, lowestHighSide = Integer.MAX_VALUE;
        if (node.getChildNodes().size() - excludeNodes.size() >= minEntries) {
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
        } else {
            log.warn("unable to resolve separationByY collision for node : {}, returning just first and last nodes", node);
            lowestHighSideNode = node.getChildNodes().get(0);
            lowestHighSide = lowestHighSideNode.getMaxY();
            highestLowSideNode = node.getChildNodes().get(node.getChildNodes().size() - 1);
            highestLowSide = highestLowSideNode.getMinY();
            highestSide = node.getChildNodes().stream().map(Node::getMaxY).max(Comparator.naturalOrder()).get();
            lowestSide = node.getChildNodes().stream().map(Node::getMinY).min(Comparator.naturalOrder()).get();
        }
        double separation = (highestLowSide - lowestHighSide) * 1.0 / (highestSide - lowestSide);
        return new SeparationByCoordinateResult<>(separation, lowestHighSideNode, highestLowSideNode);
    }

    /**
     * Ascend from a leaf node to the root, adjusting covering rectangles and propagating node splits as necessary.
     * Algorithm:
     * 1. [Initialize] set {@param first} node as leafNode (see {@link RectangleRTree#insert} step 1).
     * Set {@param second} node as newLeafNode (see {@link RectangleRTree#insert} step 2).
     * 2. [Adjust covering rectangles] Adjust nodes dimensions so that it tightly encloses all child rectangles.
     * 3. [Check if done] If {@param first} is root, stop.
     * 4. [Propagate node split upward] If {@param second} node is not null - partner of {@param first} node
     * from earlier split. Then we should check if parent capacity is not exceeded limit.
     * If so {@link RectangleRTree#splitNode} for parent is required to obtain newNode and redistribute parents children.
     * 5. [Move up to next level] Set {@param first} as parent of {@param fists} node
     * and {@param second} as newNode from 4 if a split occurred.
     *
     * @param first  - not null tree node
     * @param second - nullable tree node
     */
    private void adjustTree(TreeNode<E> first, TreeNode<E> second) {
        //2
        tightenDimensions(first);
        if (second != null) {
            tightenDimensions(second);
        }
        if (first == root) { //3
            if (second != null) {
                root = new TreeNode<>(false);
                root.addChild(first);
                root.addChild(second);
                tightenDimensions(root);
            }
            return;
        }
        if (second != null && first.getParent().getChildNodes().size() > maxEntries) { //4
            TreeNode<E> newNode = splitNode(first.getParent());
            adjustTree(first.getParent(), newNode); //5
        } else {
            adjustTree(first.getParent(), null); //5
        }
    }

    /**
     * Recalculate node dimensions so that they tightly enclose all child rectangles.
     */
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

    /**
     * Select a leaf node in which to place a new index entry.
     * Algorithm:
     * 1. [Initialize] Fist invocation shod be done passing {@link RectangleRTree#root} as {@param node}.
     * 2. [Check leaf] If {@param node} is a leaf - return {@param node} as found leaf.
     * 3. [Choose subtree] If {@param node} is not a leaf - find node in {@param node} children
     * which is required minimum enlargement to include {@param entryDimensions}.
     * 4. [Descend until a leaf is reached] Set {@param node} to be the child node found in 3. Repeat from 2.
     *
     * @param node            - node inside which search for leaf node is conducted
     * @param entryDimensions - dimensions of entry node
     * @return leaf node matching dimensions
     */
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

    /**
     * Calculate required enlargement of node with dimensions {@param nodeDimensions}
     * to include node with dimension {@param entryDimensions}
     *
     * @return difference in rectangle area between initial dimensions and dimensions after including entry.
     */
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
