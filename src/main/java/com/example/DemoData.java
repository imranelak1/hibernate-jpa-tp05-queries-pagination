package com.example;

import com.example.model.*;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;

public final class DemoData {
    public static final LocalDateTime DEBUT=LocalDateTime.of(2024,6,1,9,0);
    public static final LocalDateTime FIN=DEBUT.plusHours(2);
    private DemoData() { }
    public static void seed(EntityManager em) {
        Salle a=new Salle("Atlas",30);
        a.addReservation(new Reservation(DEBUT.plusMinutes(30),FIN.plusHours(1))); // chevauche
        Salle b=new Salle("Boreal",20);
        b.addReservation(new Reservation(DEBUT.minusHours(1),DEBUT)); // touche le debut
        Salle c=new Salle("Cedre",50);
        c.addReservation(new Reservation(FIN,FIN.plusHours(1))); // touche la fin
        Salle d=new Salle("Dune",12); // aucune reservation
        Salle e=new Salle("Etoile",40);
        e.addReservation(new Reservation(DEBUT.minusHours(1),FIN.plusHours(1))); // englobe
        Salle f=new Salle("Falaise",35);
        f.addReservation(new Reservation(DEBUT.plusDays(1),FIN.plusDays(1))); // autre jour
        em.persist(a); em.persist(b); em.persist(c); em.persist(d); em.persist(e); em.persist(f);
    }
}
