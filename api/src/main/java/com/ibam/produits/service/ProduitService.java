package com.ibam.produits.service;

import com.ibam.produits.dto.PageResponse;
import com.ibam.produits.dto.ProduitRequest;
import com.ibam.produits.dto.ProduitResponse;
import com.ibam.produits.entity.Produit;
import com.ibam.produits.exception.ProduitNotFoundException;
import com.ibam.produits.repository.ProduitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProduitService {

    private final ProduitRepository repository;

    public PageResponse<ProduitResponse> lister(String categorie, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        Page<Produit> result = (categorie == null || categorie.isBlank())
                ? repository.findAll(pageable)
                : repository.findByCategorieIgnoreCase(categorie.trim(), pageable);
        return PageResponse.of(result, ProduitResponse::from);
    }

    public ProduitResponse consulter(Integer id) {
        return ProduitResponse.from(trouver(id));
    }

    @Transactional
    public ProduitResponse creer(ProduitRequest request) {
        Produit produit = new Produit();
        appliquer(produit, request);
        return ProduitResponse.from(repository.saveAndFlush(produit));
    }

    @Transactional
    public ProduitResponse modifier(Integer id, ProduitRequest request) {
        Produit produit = trouver(id);
        appliquer(produit, request);
        return ProduitResponse.from(repository.saveAndFlush(produit));
    }

    @Transactional
    public void supprimer(Integer id) {
        repository.delete(trouver(id));
    }

    private Produit trouver(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ProduitNotFoundException(id));
    }

    private void appliquer(Produit produit, ProduitRequest r) {
        produit.setNom(r.nom().trim());
        produit.setDescription(r.description());
        produit.setPrix(r.prix());
        produit.setQuantite(r.quantite());
        produit.setCategorie(r.categorie().trim());
    }
}
