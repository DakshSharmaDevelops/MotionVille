package org.example.motionville.entity.video;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name="tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    private Long id;

    @Column(nullable = false,unique = true,length = 50)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private List<Video> videos;

}
