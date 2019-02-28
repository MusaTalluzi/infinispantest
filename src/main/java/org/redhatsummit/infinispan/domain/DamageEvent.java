package org.redhatsummit.infinispan.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;

@Entity
public class DamageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;
    private double damagePercent;

    public DamageEvent() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getDamagePercent() {
        return damagePercent;
    }

    public void setDamagePercent(double damagePercent) {
        this.damagePercent = damagePercent;
    }

    @Override
    public String toString() {
        return "DamageEvent{" +
                "id=" + id +
                ", damagePercent=" + damagePercent +
                '}';
    }

    @PrePersist
    public void onPrePersist() {
        System.out.println("DamageEvent.onPrePersist: " + this);
    }
}
