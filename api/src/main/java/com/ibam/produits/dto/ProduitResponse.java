package com.ibam.produits.dto;

import com.ibam.produits.entity.Produit;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProduitResponse(
        Integer id,
        String nom,
        String description,
        BigDecimal prix,
        Integer quantite,
        String categorie,
        LocalDateTime dateCreation
) {
    public static ProduitResponse from(Produit p) {
        return new ProduitResponse(p.getId(), p.getNom(), p.getDescription(), p.getPrix(),
                p.getQuantite(), p.getCategorie(), p.getDateCreation());
    }
}
