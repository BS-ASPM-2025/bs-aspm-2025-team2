package bsaspm2025team2.backend.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "required_skills", columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(name = "skills_weight", nullable = false)
    private int skillsWeight;

    @Column(name = "experience_weight", nullable = false)
    private int experienceWeight;

    protected Position() {
        // JPA
    }

    public Position(String name, String requiredSkills, int skillsWeight, int experienceWeight) {
        this.name = name;
        this.requiredSkills = requiredSkills;
        this.skillsWeight = skillsWeight;
        this.experienceWeight = experienceWeight;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }

    public int getSkillsWeight() { return skillsWeight; }
    public void setSkillsWeight(int skillsWeight) { this.skillsWeight = skillsWeight; }

    public int getExperienceWeight() { return experienceWeight; }
    public void setExperienceWeight(int experienceWeight) { this.experienceWeight = experienceWeight; }
}
