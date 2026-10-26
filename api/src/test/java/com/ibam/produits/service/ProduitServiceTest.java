package com.ibam.produits.service;

import com.ibam.produits.dto.PageResponse;
import com.ibam.produits.dto.ProduitRequest;
import com.ibam.produits.dto.ProduitResponse;
import com.ibam.produits.entity.Produit;
import com.ibam.produits.exception.ProduitNotFoundException;
import com.ibam.produits.repository.ProduitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    @Mock
    ProduitRepository repository;

    @InjectMocks
    ProduitService service;

    private Produit produit(int id) {
        return Produit.builder().id(id).nom("Clavier").prix(new BigDecimal("59.90"))
                .quantite(5).categorie("Informatique").build();
    }

    private ProduitRequest requete() {
        return new ProduitRequest("  Souris ", "sans fil", new BigDecimal("19.90"), 10, " Informatique ");
    }

    @Test
    void listerSansFiltreUtiliseFindAll() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(produit(1))));

        PageResponse<ProduitResponse> page = service.lister(null, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @Test
    void listerAvecCategorieUtiliseLeFiltre() {
        when(repository.findByCategorieIgnoreCase(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(produit(1))));

        service.lister(" Informatique ", 0, 20);

        verify(repository).findByCategorieIgnoreCase(org.mockito.ArgumentMatchers.eq("Informatique"), any(Pageable.class));
    }

    @Test
    void consulterRetourneLeProduit() {
        when(repository.findById(1)).thenReturn(Optional.of(produit(1)));

        assertThat(service.consulter(1).nom()).isEqualTo("Clavier");
    }

    @Test
    void consulterLeveUneExceptionSiAbsent() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consulter(99)).isInstanceOf(ProduitNotFoundException.class);
    }

    @Test
    void creerNormaliseEtEnregistre() {
        when(repository.saveAndFlush(any(Produit.class))).thenAnswer(i -> {
            Produit p = i.getArgument(0);
            p.setId(7);
            return p;
        });

        ProduitResponse r = service.creer(requete());

        assertThat(r.id()).isEqualTo(7);
        assertThat(r.nom()).isEqualTo("Souris");
        assertThat(r.categorie()).isEqualTo("Informatique");
    }

    @Test
    void modifierMetAJourLeProduitExistant() {
        when(repository.findById(1)).thenReturn(Optional.of(produit(1)));
        when(repository.saveAndFlush(any(Produit.class))).thenAnswer(i -> i.getArgument(0));

        ProduitResponse r = service.modifier(1, requete());

        assertThat(r.id()).isEqualTo(1);
        assertThat(r.nom()).isEqualTo("Souris");
        assertThat(r.quantite()).isEqualTo(10);
    }

    @Test
    void modifierLeveUneExceptionSiAbsent() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.modifier(99, requete())).isInstanceOf(ProduitNotFoundException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void supprimerSupprimeLeProduit() {
        Produit p = produit(1);
        when(repository.findById(1)).thenReturn(Optional.of(p));

        service.supprimer(1);

        verify(repository).delete(p);
    }

    @Test
    void supprimerLeveUneExceptionSiAbsent() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.supprimer(99)).isInstanceOf(ProduitNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
