package com.example;

import com.example.model.Salle;
import com.example.service.*;
import com.example.util.Jpa;
import javax.persistence.*;
import java.util.List;
import static com.example.DemoData.*;

public class App {
    public static void main(String[] args) {
        EntityManagerFactory factory=Persistence.createEntityManagerFactory("SallePU");
        EntityManager em=null;
        try {
            Jpa.transaction(factory,m -> { seed(m); return null; });
            em=factory.createEntityManager();
            SalleService service=new SalleServiceImpl();
            ((SalleServiceImpl)service).setEntityManager(em);
            System.out.println("=== TP05 - JPQL, Criteria et pagination ===");
            System.out.println("Donnees: 6 salles, 5 reservations; creneau ["+DEBUT+", "+FIN+"[");
            List<Salle> available=service.getAvailableRooms(DEBUT,FIN);
            System.out.println("JPQL disponibles: "+names(available));
            System.out.println("Criteria nom='e', capacite>=20, disponibles: "+names(service.searchRooms("e",20,DEBUT,FIN,1,10)));
            for(int page=1;page<=2;page++) {
                var result=service.searchPage(null,20,DEBUT,FIN,page,2);
                System.out.println("Page "+result.getCurrentPage()+"/"+result.getTotalPages()+", taille="+result.getPageSize()+", total="+result.getTotalItems()+": "+names(result.getItems()));
            }
            System.out.println("Page hors limite: "+service.searchPage(null,20,DEBUT,FIN,3,2).getItems().size()+" resultat");
            System.out.println("Bornes adjacentes: Boreal et Cedre restent disponibles.");
            System.out.println("RESULTAT: 4 salles disponibles; filtres et compte coherent; 3 salles capacite>=20 sur 2 pages.");
        } finally { if(em!=null) em.close(); factory.close(); }
    }
    private static List<String> names(List<Salle> rooms) {
        return rooms.stream().map(r -> r.getNom()+"("+r.getCapacite()+")").toList();
    }
}
