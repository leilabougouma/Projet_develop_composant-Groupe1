package com.ibam.produits.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Réponse d'une suppression : confirme l'opération et rappelle le produit supprimé. */
@Schema(description = "Confirmation de suppression, avec le produit tel qu'il était avant d'être supprimé")
public record SuppressionResponse(
        @Schema(example = "Produit 12 supprimé") String message,
        ProduitResponse produit
) {
    public static SuppressionResponse of(ProduitResponse produit) {
        return new SuppressionResponse("Produit " + produit.id() + " supprimé", produit);
    }
}
