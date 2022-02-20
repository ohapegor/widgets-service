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
     * <p>1. [Find position for new record] Invoke {@link RectangleRTree#chooseLeaf} to select a leafNode</p>
     * <p>2. [Add record to leaf node] If leaf has room for another entry, install entry.
     * Otherwise, invoke {@link RectangleRTree#splitNode} to obtain newLeafNode and redistribute all the old entries
     * of leafNode and inserting entry between leafNode and newLeafNode.</p>
     * <p>3. Invoke {@link RectangleRTree#adjustTree} on leafNode also passing newLeafNode if a split was performed.</p>
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

        Optional<EntryNode<E>> entryNodeOpt = findEntry(root, id, dimensions); //1 [Find node containing record]
        if (entryNodeOpt.isEmpty()) {
            return false;
        }
        EntryNode<E> entryNode = entryNodeOpt.get();
        if (entryNode.getParent() == null) {
            throw new IllegalStateException("found entry with null parent by id = " + id);
        }
        TreeNode<E> parent = entryNode.getParent();
        if (!parent.removeChild(entryNode)) { //2 [Delete record]
            log.error("failed to delete entryNode={}  from parent={}", entryNode, parent);
            throw new IllegalStateException("failed to delete entryNode");
        }

        condenseTree(parent); //3 [Propagate changes]

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
        List<EntryNode<E>> nodesToReinsert = new LinkedList<>(); //1 [Initialize]
        while (node != root) {
            if (node.getChildNodes().size() < minEntries) { //2 [Eliminate under-full node]
                extractAllEntries(node, nodesToReinsert);
                TreeNode<E> parent = node.getParent();
                parent.removeChild(node);
                node = parent; //4 [Move up one level in tree]
            } else {
                tightenDimensions(node); //3 [Adjust covering rectangle]
                node = node.getParent(); //4 [Move up one level in tree]
            }
        }
        if (!root.isLeaf() && root.getChildNodes().size() == 0) { //5 [Check if root becomes leaf]
            root = new TreeNode<>(true);
        }
        nodesToReinsert.forEach(this::insert); //5 [Reinsert orphaned entries]
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

    /**
     * Given node with overflowed children limit {@param origNode} then create new node and distribute children
     * between them.
     * Algorithm:
     * 1. [Pick first entry for each group] Apply Algorithm {@link RectangleRTree#linearPeekSeeds} to choose
     * two entries to be the first elements of the groups. Assign each to a group.
     * 2. [Check If done] If all entries have been assigned, stop If one group has so few entries
     * that all the rest must be assigned to it m order for it to have
     * the minimum number {@link RectangleRTree#minEntries}, assign them and stop.
     * 3. [Select entry to assign] Invoke Algorithm {@link RectangleRTree#choosePreferredNode} to choose the next
     * entry to assign. Add it to the group whose covering rectangle will have to be enlarged least to accommodate it.
     * Resolve ties by adding the entry to the group with smaller area, then to the one with fewer entries,
     * then to either. Repeat from [2].
     *
     * @param origNode - node with overflowed children limit
     * @return newly created node
     */
    private TreeNode<E> splitNode(TreeNode<E> origNode) {
        //1 [Pick first entry for each group] choose two entries
        var seeds = linearPeekSeeds(origNode);
        if (!origNode.removeChild(seeds.lowestNode)) {
            throw new IllegalStateException("remove lowestNode failed");
        }
        if (!origNode.removeChild(seeds.highestNode)) {
            throw new IllegalStateException("remove highestNode failed");
        }

        //1 origNode becomes the first group, evicting all children to array
        LinkedList<Node<E>> childrenToRearrange = new LinkedList<>(origNode.getChildNodes());
        origNode.getChildNodes().clear();

        TreeNode<E> parent = origNode.parent;
        if (origNode == root) {
            root = new TreeNode<>(false);
            parent = root;
            parent.addChild(origNode);
        }

        //1 Create second group
        TreeNode<E> newNode = new TreeNode<>(origNode.isLeaf());
        parent.addChild(newNode);

        //1 Assign chosen entries to groups
        origNode.addChild(seeds.lowestNode);
        newNode.addChild(seeds.highestNode);

        tightenDimensions(origNode);
        tightenDimensions(newNode);

        //2. [Check If done]
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
            //3. [Select entry to assign]
            TreeNode<E> preferredNode = choosePreferredNode(origNode, newNode, child);
            preferredNode.addChild(child);
            tightenDimensions(preferredNode);
        }
        return newNode;
    }

    private boolean shouldAddAllRemainingToMatchMinSize(TreeNode<E> node, int remainingSize) {
        return remainingSize <= minEntries && node.getChildNodes().size() + remainingSize == minEntries;
    }

    /**
     * Select node to put an entry in.
     * Algorithm:
     * 1. [Determine cost of adding entry in each node] Calculate the area increase required in the
     * covering rectangle of node to include adding node.
     * 2. [Find node with the greatest preference] Choose a node with area increase is lower
     * or node with fewer elements if calculated  area increase is th same.
     *
     * @param first                - first node
     * @param second               - second node
     * @param addingNodeDimensions - dimension of adding node
     * @return chosen node
     */
    private TreeNode<E> choosePreferredNode(TreeNode<E> first, TreeNode<E> second, Rectangle addingNodeDimensions) {
        // [1]
        BigDecimal enlargement1 = calculateRequiredEnlargement(first, addingNodeDimensions);
        BigDecimal enlargement2 = calculateRequiredEnlargement(second, addingNodeDimensions);
        // [2]
        int comparison = enlargement1.compareTo(enlargement2);
        if (comparison < 0) {
            return first;
        } else if (comparison > 0) {
            return second;
        }
        return first.getChildNodes().size() < second.getChildNodes().size() ? first : second;
    }

    /**
     * Select two entries among nodes children to be the first elements of the splitting groups.
     * Algorithm:
     * 1. [Find extreme rectangles along all dimensions] Along each dimension, find the entry whose rectangle has
     * the highest low side, and the one with the lowest high side. Record the separation. If a collision happens
     * when it is not possible to find 2 extreme rectangles, then fallback to selecting just firs and last rectangles.
     * 2. [AdJust for shape of the rectangle cluster] Normalize the separations by dividing by the width of the entire
     * set along the corresponding dimension.
     * 3. [Select the most extreme pair] Choose the pair with the greatest normalized separation along any dimension.
     *
     * @param node - node in which children we are choosing seeds
     * @return object containing 2 chosen child nodes
     */
    private SeparationByCoordinateResult<E> linearPeekSeeds(TreeNode<E> node) {
        var separationByX = separationByX(node, new LinkedList<>());
        var separationByY = separationByY(node, new LinkedList<>());
        //3 [Select the most extreme pair]
        return separationByX.normalizedSeparation > separationByY.normalizedSeparation ? separationByX : separationByY;
    }

    private SeparationByCoordinateResult<E> separationByX(TreeNode<E> node, List<Node<E>> excludeNodes) {
        Node<E> lowestHighSideNode = null;
        Node<E> highestLowSideNode = null;
        int highestSide = Integer.MIN_VALUE, highestLowSide = Integer.MIN_VALUE;
        int lowestSide = Integer.MAX_VALUE, lowestHighSide = Integer.MAX_VALUE;
        if (node.getChildNodes().size() - excludeNodes.size() >= minEntries) {
            //1. [Find extreme rectangles along all dimensions]
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
            /*
             Fallback logic for handling collisions, when highestLowSideNode and lowestHighSideNode are pointing to the same node.
             May happen when all child nodes have the same dimensions.
             */
            log.warn("unable to resolve separationByX collision for node : {}, returning just first and last nodes", node);
            lowestHighSideNode = node.getChildNodes().get(0);
            lowestHighSide = lowestHighSideNode.getMaxX();
            highestLowSideNode = node.getChildNodes().get(node.getChildNodes().size() - 1);
            highestLowSide = highestLowSideNode.getMinX();
            highestSide = node.getChildNodes().stream().map(Node::getMaxX).max(Comparator.naturalOrder()).get();
            lowestSide = node.getChildNodes().stream().map(Node::getMinX).min(Comparator.naturalOrder()).get();
        }
        //2. [AdJust for shape of the rectangle cluster]
        double separation = (highestLowSide - lowestHighSide) * 1.0 / (highestSide - lowestSide);
        return new SeparationByCoordinateResult<>(separation, lowestHighSideNode, highestLowSideNode);
    }

    private SeparationByCoordinateResult<E> separationByY(TreeNode<E> node, List<Node<E>> excludeNodes) {
        Node<E> lowestHighSideNode = null;
        Node<E> highestLowSideNode = null;
        int highestSide = Integer.MIN_VALUE, highestLowSide = Integer.MIN_VALUE;
        int lowestSide = Integer.MAX_VALUE, lowestHighSide = Integer.MAX_VALUE;
        if (node.getChildNodes().size() - excludeNodes.size() >= minEntries) {
            //1. [Find extreme rectangles along all dimensions]
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
        //2. [AdJust for shape of the rectangle cluster]
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
        //2 [Adjust covering rectangles]
        tightenDimensions(first);
        if (second != null) {
            tightenDimensions(second);
        }
        //3 [Check if done]
        if (first == root) {
            if (second != null) {
                root = new TreeNode<>(false);
                root.addChild(first);
                root.addChild(second);
                tightenDimensions(root);
            }
            return;
        }
        if (second != null && first.getParent().getChildNodes().size() > maxEntries) { //4 [Propagate node split upward]
            TreeNode<E> newNode = splitNode(first.getParent());
            adjustTree(first.getParent(), newNode); //5 [Move up to next level]
        } else {
            adjustTree(first.getParent(), null); //5 [Move up to next level]
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
     * 4. [Descend until a leaf is reached] Set {@param node} to be the child node found in [3]. Repeat from [2].
     *
     * @param node            - node inside which search for leaf node is conducted
     * @param entryDimensions - dimensions of entry node
     * @return leaf node matching dimensions
     */
    private TreeNode<E> chooseLeaf(TreeNode<E> node, Rectangle entryDimensions) {
        if (node.isLeaf()) { //2 [Check leaf]
            return node;
        }
        BigDecimal minEnlargement = null;
        Node<E> minEnlargementNode = null;
        for (Node<E> child : node.getChildNodes()) { //3 [Choose subtree]
            BigDecimal enlargement = calculateRequiredEnlargement(child, entryDimensions);
            if (minEnlargement == null || enlargement.compareTo(minEnlargement) < 0) {
                minEnlargement = enlargement;
                minEnlargementNode = child;
            }
        }
        TreeNode<E> chosenNode = (TreeNode<E>) minEnlargementNode;
        return chooseLeaf(chosenNode, entryDimensions); //4 [Descend until a leaf is reached]
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
