package com.example;

import com.example.model.Salle;
import com.example.repository.SalleRepositoryImpl;
import com.example.service.*;
import com.example.util.*;
import javax.persistence.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.DemoData.*;

class RoomQueryTest {
    private EntityManagerFactory factory;
    private EntityManager em;
    private SalleService service;
    @BeforeEach void open() {
        factory=Persistence.createEntityManagerFactory("SallePU",Map.of(
            "javax.persistence.jdbc.url","jdbc:h2:mem:rooms_"+UUID.randomUUID(),"hibernate.show_sql","false"));
        Jpa.transaction(factory,m -> { seed(m); return null; });
        em=factory.createEntityManager(); service=new SalleServiceImpl(new SalleRepositoryImpl(em));
    }
    @AfterEach void close() { em.close(); factory.close(); }
    private List<String> names(List<Salle> rooms) { return rooms.stream().map(Salle::getNom).toList(); }
    @Test void jpqlExcludesPartialAndContainingOverlaps() {
        assertEquals(List.of("Boreal","Cedre","Dune","Falaise"),names(service.getAvailableRooms(DEBUT,FIN)));
    }
    @Test void adjacentIntervalsAndRoomsWithoutReservationsAreAvailable() {
        assertTrue(names(service.getAvailableRooms(DEBUT,FIN)).containsAll(List.of("Boreal","Cedre","Dune")));
        assertEquals(6,service.getAvailableRooms(DEBUT.plusDays(2),FIN.plusDays(2)).size());
    }
    @Test void criteriaAndCountUseIdenticalCombinedFilters() {
        assertEquals(List.of("Boreal","Cedre","Falaise"),names(service.searchRooms("e",20,DEBUT,FIN,1,10)));
        assertEquals(3,service.countRooms("e",20,DEBUT,FIN));
        assertEquals(0,service.countRooms("Atlas",20,DEBUT,FIN));
        assertEquals(1,service.countRooms("Atlas",20,null,null));
    }
    @Test void nameMatchingIsCaseInsensitiveAndEscapesWildcards() {
        assertEquals(List.of("Cedre"),names(service.searchRooms(" CED ",null,null,null,1,10)));
        assertEquals(0,service.countRooms("%",null,null,null));
        assertEquals(0,service.countRooms("_",null,null,null));
        assertEquals(6,service.countRooms("  ",null,null,null));
    }
    @Test void paginationIsStableAndHasAccurateFilteredMetadata() {
        var first=service.searchPage(null,20,DEBUT,FIN,1,2);
        var second=service.searchPage(null,20,DEBUT,FIN,2,2);
        assertEquals(List.of("Boreal","Cedre"),names(first.getItems()));
        assertEquals(List.of("Falaise"),names(second.getItems()));
        assertEquals(3,first.getTotalItems()); assertEquals(2,first.getTotalPages());
        assertEquals(2,second.getCurrentPage()); assertEquals(2,second.getPageSize());
        Set<Long> ids=new HashSet<>(); first.getItems().forEach(r -> ids.add(r.getId()));
        second.getItems().forEach(r -> assertTrue(ids.add(r.getId())));
    }
    @Test void emptyAndOutsidePagesKeepCorrectTotals() {
        var outside=service.searchPage(null,20,DEBUT,FIN,3,2);
        assertTrue(outside.getItems().isEmpty()); assertEquals(3,outside.getTotalItems());
        var empty=service.searchPage("absent",null,null,null,1,2);
        assertEquals(0,empty.getTotalPages()); assertEquals(0,empty.getTotalItems());
        assertThrows(UnsupportedOperationException.class, () -> outside.getItems().add(new Salle()));
    }
    @Test void rejectsInvalidTimeWindowsAndHalfProvidedIntervals() {
        assertThrows(IllegalArgumentException.class, () -> service.getAvailableRooms(null,null));
        assertThrows(IllegalArgumentException.class, () -> service.getAvailableRooms(FIN,DEBUT));
        assertThrows(IllegalArgumentException.class, () -> service.getAvailableRooms(DEBUT,DEBUT));
        assertThrows(IllegalArgumentException.class, () -> service.countRooms(null,null,DEBUT,null));
    }
    @Test void rejectsInvalidPaginationAndCapacity() {
        assertThrows(IllegalArgumentException.class, () -> service.searchRooms(null,null,null,null,0,2));
        assertThrows(IllegalArgumentException.class, () -> service.searchRooms(null,null,null,null,1,0));
        assertThrows(IllegalArgumentException.class, () -> service.searchRooms(null,null,null,null,Integer.MAX_VALUE,2));
        assertThrows(IllegalArgumentException.class, () -> service.countRooms(null,-1,null,null));
        assertThrows(IllegalArgumentException.class, () -> new PaginationResult<>(List.of(),1,0,0));
    }
    @Test void bidirectionalAssociationCascadesAndPersistsRequiredForeignKey() {
        assertEquals(5,((Number)em.createQuery("select count(r) from Reservation r").getSingleResult()).longValue());
        Salle atlas=em.createQuery("select s from Salle s where s.nom='Atlas'",Salle.class).getSingleResult();
        assertEquals(1,atlas.getReservations().size());
        assertEquals(atlas.getId(),atlas.getReservations().iterator().next().getSalle().getId());
    }
}
