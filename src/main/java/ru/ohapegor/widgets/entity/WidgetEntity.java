package ru.ohapegor.widgets.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
//import org.locationtech.jts.geom.Polygon;
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
public class WidgetEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private String id;

    private Integer height;

    private Integer width;

    private Integer x;

    private Integer y;

    private Integer z;

    @CreatedDate
    @Builder.Default
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Builder.Default
    private Instant lastModifiedAt = Instant.now();
}
