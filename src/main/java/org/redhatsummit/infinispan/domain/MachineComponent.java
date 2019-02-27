package org.redhatsummit.infinispan.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class MachineComponent implements Serializable {

    @Id
    private Long id;
    private double attrition;

    public MachineComponent() {
    }

    public MachineComponent(Long id, double attrition) {
        this.id = id;
        this.attrition = attrition;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getAttrition() {
        return attrition;
    }

    public void setAttrition(double attrition) {
        this.attrition = attrition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MachineComponent component = (MachineComponent) o;
        return Double.compare(component.attrition, attrition) == 0 &&
                Objects.equals(id, component.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, attrition);
    }

    @Override
    public String toString() {
        return "MachineComponent{" +
                "id=" + id +
                ", attrition=" + attrition +
                '}';
    }
}
