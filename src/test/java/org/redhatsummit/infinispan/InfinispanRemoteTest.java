package org.redhatsummit.infinispan;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.TransactionManager;

import org.junit.jupiter.api.Test;
import org.redhatsummit.infinispan.domain.MachineComponent;

public class InfinispanRemoteTest {

    @Test
    public void addAndRetrieveComponentTest() throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("infinispan-test");
        MachineComponent component = new MachineComponent(0L, 0.0);
        persistTestData(emf, component);

        TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        tm.begin();
        EntityManager em = emf.createEntityManager();
        MachineComponent loadedComponent = em.find(MachineComponent.class, component.getId());

        assert component.equals(loadedComponent);

        em.remove(loadedComponent);
        tm.commit();
    }

    private void persistTestData(EntityManagerFactory entityManagerFactory, MachineComponent component) throws Exception {
        TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        tm.begin();
        EntityManager entityManager = entityManagerFactory.createEntityManager();

        entityManager.persist(component);
        entityManager.close();
        tm.commit();
    }
}
