package com.ibam.produits.exception;

public class ProduitNotFoundException extends RuntimeException {

    public ProduitNotFoundException(Integer id) {
        super("Produit introuvable avec l'id " + id);
    }
}
