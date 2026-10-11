package com.example.repository;

import com.example.model.*;
import javax.persistence.*;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.*;

/** Injection explicite pour l'executable Java SE; meme logique de requetes qu'en JEE. */
public class SalleRepositoryImpl implements SalleRepository {
    private final EntityManager entityManager;
    public SalleRepositoryImpl(EntityManager entityManager) { this.entityManager=Objects.requireNonNull(entityManager); }

    @Override public List<Salle> findAvailableRooms(LocalDateTime debut, LocalDateTime fin) {
        validateInterval(debut,fin,true);
        return entityManager.createQuery("SELECT s FROM Salle s WHERE s.id NOT IN ("+
            "SELECT r.salle.id FROM Reservation r WHERE :debut < r.fin AND :fin > r.debut) ORDER BY s.nom, s.id",Salle.class)
            .setParameter("debut",debut).setParameter("fin",fin).getResultList();
    }
    @Override public List<Salle> searchRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin, int page, int size) {
        validate(nom,capaciteMin,debut,fin);
        int offset = offset(page,size);
        CriteriaBuilder cb=entityManager.getCriteriaBuilder();
        CriteriaQuery<Salle> query=cb.createQuery(Salle.class);
        Root<Salle> room=query.from(Salle.class);
        query.select(room).where(predicates(cb,query,room,nom,capaciteMin,debut,fin))
            .orderBy(cb.asc(room.get("nom")),cb.asc(room.get("id")));
        return entityManager.createQuery(query).setFirstResult(offset).setMaxResults(size).getResultList();
    }
    @Override public long countRooms(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        validate(nom,capaciteMin,debut,fin);
        CriteriaBuilder cb=entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query=cb.createQuery(Long.class);
        Root<Salle> room=query.from(Salle.class);
        // Les predicats et la sous-requete appartiennent a CETTE requete de comptage.
        query.select(cb.count(room)).where(predicates(cb,query,room,nom,capaciteMin,debut,fin));
        return entityManager.createQuery(query).getSingleResult();
    }
    private Predicate[] predicates(CriteriaBuilder cb, CriteriaQuery<?> query, Root<Salle> room,
            String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        List<Predicate> filters=new ArrayList<>();
        if (nom!=null && !nom.isBlank()) {
            String escaped=nom.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");
            filters.add(cb.like(cb.lower(room.get("nom")),"%"+escaped+"%",'\\'));
        }
        if (capaciteMin!=null) filters.add(cb.ge(room.get("capacite"),capaciteMin));
        if (debut!=null) {
            Subquery<Long> busy=query.subquery(Long.class);
            Root<Reservation> reservation=busy.from(Reservation.class);
            busy.select(reservation.get("salle").get("id")).where(
                cb.lessThan(reservation.<LocalDateTime>get("debut"),fin),
                cb.greaterThan(reservation.<LocalDateTime>get("fin"),debut));
            filters.add(cb.not(room.get("id").in(busy)));
        }
        return filters.toArray(Predicate[]::new);
    }
    private static void validate(String nom, Integer capaciteMin, LocalDateTime debut, LocalDateTime fin) {
        if (capaciteMin!=null && capaciteMin<0) throw new IllegalArgumentException("Capacite minimale negative");
        validateInterval(debut,fin,false);
    }
    public static void validateInterval(LocalDateTime debut, LocalDateTime fin, boolean required) {
        if (debut==null && fin==null && !required) return;
        if (debut==null || fin==null || !debut.isBefore(fin))
            throw new IllegalArgumentException("Le creneau doit avoir un debut strictement anterieur a sa fin");
    }
    private static int offset(int page,int size) {
        if (page<1 || size<1) throw new IllegalArgumentException("Page et taille doivent etre positives (page 1-based)");
        long value=(long)(page-1)*size;
        if (value>Integer.MAX_VALUE) throw new IllegalArgumentException("Offset trop grand");
        return (int)value;
    }
}
