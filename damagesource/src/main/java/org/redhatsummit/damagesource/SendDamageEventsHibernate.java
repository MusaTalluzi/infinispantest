package org.redhatsummit.damagesource;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import io.netty.util.internal.ThreadLocalRandom;

public class SendDamageEventsHibernate {

    private static int EVENTS_PER_SECOND = 1;

    public static void main(String[] args) throws Exception {
        int EVENTS_PER_SECOND = 1;
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("infinispan-test");

        while (true) {
            for (int i = 0; i < EVENTS_PER_SECOND; i++) {
                sendDamageEvent(emf);
            }
            Thread.sleep(1000);
        }
//        queryCacheStore(emf);
    }

    private static void sendDamageEvent(EntityManagerFactory emf) throws Exception {
        DamageEvent event = new DamageEvent();
        event.setDamagePercent(ThreadLocalRandom.current().nextDouble());

        TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        tm.begin();
        EntityManager em = emf.createEntityManager();
        em.persist(event);
        em.close();
        tm.commit();
    }

    private static void queryCacheStore(EntityManagerFactory emf) throws Exception {
        TransactionManager tm = com.arjuna.ats.jta.TransactionManager.transactionManager();
        tm.begin();
        EntityManager em = emf.createEntityManager();
        Query query = em.createQuery("SELECT de FROM " + DamageEvent.class.getSimpleName() + " de");
        List<DamageEvent> damageEvents = query.getResultList();
        em.close();
        tm.commit();

        if (damageEvents != null) {
            for (DamageEvent damageEvent : damageEvents) {
                System.out.println("SendDamageEventsHibernate.queryCacheStore: " + damageEvent);
            }
        } else {
            System.out.println("SendDamageEventsHibernate.queryCacheStore: DamageEvents is NULL");
        }
    }
}
