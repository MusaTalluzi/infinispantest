package org.redhatsummit.damagesource;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;

@Entity
public class DamageEvent implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;
    private double damagePercent;

    public DamageEvent() {
    }

    public DamageEvent(Long id, double damagePercent) {
        this.id = id;
        this.damagePercent = damagePercent;
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DamageEvent that = (DamageEvent) o;
        return Double.compare(that.damagePercent, damagePercent) == 0 &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, damagePercent);
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
