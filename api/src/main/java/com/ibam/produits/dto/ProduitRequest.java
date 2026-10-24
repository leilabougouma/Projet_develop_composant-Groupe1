package com.ibam.produits.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Données d'un produit à créer ou à remplacer")
public record ProduitRequest(
        @Schema(example = "Clavier mécanique")
        @NotBlank(message = "Le nom est obligatoire")
        @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
        String nom,

        @Schema(example = "Clavier AZERTY rétroéclairé")
        String description,

        @Schema(example = "59.90")
        @NotNull(message = "Le prix est obligatoire")
        @PositiveOrZero(message = "Le prix doit être positif ou nul")
        @Digits(integer = 8, fraction = 2, message = "Le prix doit avoir au plus 8 chiffres entiers et 2 décimales")
        BigDecimal prix,

        @Schema(example = "25")
        @NotNull(message = "La quantité est obligatoire")
        @PositiveOrZero(message = "La quantité doit être positive ou nulle")
        Integer quantite,

        @Schema(example = "Informatique")
        @NotBlank(message = "La catégorie est obligatoire")
        @Size(max = 100, message = "La catégorie ne doit pas dépasser 100 caractères")
        String categorie
) {
}
