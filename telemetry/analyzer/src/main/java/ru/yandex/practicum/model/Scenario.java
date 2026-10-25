package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scenarios")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Scenario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "hub_id", nullable = false)
    String hubId;

    @Column(name = "name", nullable = false)
    String name;

    @Builder.Default
    @OneToMany(mappedBy = "scenario", fetch = FetchType.EAGER)
    List<ScenarioCondition> conditions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "scenario", fetch = FetchType.EAGER)
    List<ScenarioAction> actions = new ArrayList<>();
}
