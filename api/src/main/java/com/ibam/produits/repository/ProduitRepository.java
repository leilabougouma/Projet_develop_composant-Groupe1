package com.ibam.produits.repository;

import com.ibam.produits.entity.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProduitRepository extends JpaRepository<Produit, Integer> {

    Page<Produit> findByCategorieIgnoreCase(String categorie, Pageable pageable);
}
