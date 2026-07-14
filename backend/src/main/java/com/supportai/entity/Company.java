package com.supportai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "companies")
@Getter
@Setter
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column
    private String website;

    @Column
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String aiSystemPrompt;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "company")
    private List<User> members = new ArrayList<>();
}
