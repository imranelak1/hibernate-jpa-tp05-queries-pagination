package com.example.service;

import com.example.model.Salle;
import com.example.repository.*;
import com.example.util.PaginationResult;
import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;

public class SalleServiceImpl implements SalleService {
    private SalleRepository repository;
    public SalleServiceImpl() { }
    public SalleServiceImpl(SalleRepository repository) { this.repository=repository; }
    public void setEntityManager(EntityManager em) { this.repository=new SalleRepositoryImpl(em); }
    @Override public List<Salle> getAvailableRooms(LocalDateTime debut,LocalDateTime fin) { return repository.findAvailableRooms(debut,fin); }
    @Override public List<Salle> searchRooms(String nom,Integer capaciteMin,LocalDateTime debut,LocalDateTime fin,int page,int size) {
        return repository.searchRooms(nom,capaciteMin,debut,fin,page,size);
    }
    @Override public long countRooms(String nom,Integer capaciteMin,LocalDateTime debut,LocalDateTime fin) {
        return repository.countRooms(nom,capaciteMin,debut,fin);
    }
    @Override public PaginationResult<Salle> searchPage(String nom,Integer capaciteMin,LocalDateTime debut,LocalDateTime fin,int page,int size) {
        List<Salle> items=searchRooms(nom,capaciteMin,debut,fin,page,size);
        return new PaginationResult<>(items,page,size,countRooms(nom,capaciteMin,debut,fin));
    }
}
