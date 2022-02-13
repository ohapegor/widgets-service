package ru.ohapegor.widgets.repository.memory.rtree;

import lombok.Getter;
import lombok.Setter;
import ru.ohapegor.widgets.model.HasId;
import ru.ohapegor.widgets.model.Rectangle;


@Getter
@Setter
abstract class Node<E extends HasId> implements Rectangle {
    protected int x;
    protected int y;
    protected int height;
    protected int width;
    protected TreeNode<E> parent;

    public void setDimensions(Rectangle rectangle) {
        this.x = rectangle.getX();
        this.y = rectangle.getY();
        this.width = rectangle.getWidth();
        this.height = rectangle.getHeight();
    }


}
