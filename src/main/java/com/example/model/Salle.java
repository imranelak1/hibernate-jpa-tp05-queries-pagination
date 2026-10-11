package com.example.model;

import javax.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity @Table(name="salles")
public class Salle {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false,unique=true)
    private String nom;
    @Column(nullable=false)
    private int capacite;
    @OneToMany(mappedBy="salle",cascade=CascadeType.ALL,fetch=FetchType.LAZY)
    private Set<Reservation> reservations = new LinkedHashSet<>();
    public Salle() { }
    public Salle(String nom, int capacite) { this.nom=nom; this.capacite=capacite; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id=id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom=nom; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite=capacite; }
    public Set<Reservation> getReservations() { return reservations; }
    public void setReservations(Set<Reservation> reservations) {
        this.reservations=reservations;
        reservations.forEach(r -> r.setSalle(this));
    }
    public void addReservation(Reservation reservation) {
        reservations.add(reservation); reservation.setSalle(this);
    }
}
