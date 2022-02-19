package ru.ohapegor.widgets.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "WIDGETS")
public class WidgetEntity implements HasId, Rectangle {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private int height;

    private int width;

    private int x;

    private int y;

    private Integer z;

    @CreatedDate
    @Builder.Default
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Builder.Default
    private Instant lastModifiedAt = Instant.now();

    public void updateData(WidgetEntity fromEntity) {
        id = fromEntity.getId();
        height = fromEntity.getHeight();
        width = fromEntity.getWidth();
        z = fromEntity.getZ();
        x = fromEntity.getX();
        y = fromEntity.getY();
        createdAt = fromEntity.createdAt;
        lastModifiedAt = fromEntity.lastModifiedAt;
    }

    /**
     * Used to return clones from all repository methods to avoid index corruption
     * if some indexed fields are changed in service layer by object reference
     */
    @Override
    public WidgetEntity clone() {
        WidgetEntity clone = new WidgetEntity();
        clone.updateData(this);
        return clone;
    }

}
