package com.ibam.produits.controller;

import com.ibam.produits.dto.PageResponse;
import com.ibam.produits.dto.ProduitRequest;
import com.ibam.produits.dto.ProduitResponse;
import com.ibam.produits.dto.SuppressionResponse;
import com.ibam.produits.exception.ErrorResponse;
import com.ibam.produits.service.ProduitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/produits")
@RequiredArgsConstructor
@Validated
@Tag(name = "Produits", description = "Opérations CRUD sur les produits")
public class ProduitController {

    private final ProduitService service;

    @GetMapping
    @Operation(summary = "Lister les produits (pagination, filtre par catégorie)")
    @ApiResponse(responseCode = "200", description = "Page de produits")
    @ApiResponse(responseCode = "400", description = "Paramètres de pagination invalides",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PageResponse<ProduitResponse> lister(
            @RequestParam(required = false) String categorie,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page doit être >= 0") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size doit être >= 1")
            @Max(value = 100, message = "size doit être <= 100") int size) {
        return service.lister(categorie, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un produit")
    @ApiResponse(responseCode = "200", description = "Produit trouvé")
    @ApiResponse(responseCode = "404", description = "Produit absent",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProduitResponse consulter(@PathVariable Integer id) {
        return service.consulter(id);
    }

    @PostMapping
    @Operation(summary = "Créer un produit")
    @ApiResponse(responseCode = "201", description = "Produit créé")
    @ApiResponse(responseCode = "400", description = "Données invalides",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ProduitResponse> creer(@Valid @RequestBody ProduitRequest request) {
        ProduitResponse created = service.creer(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un produit")
    @ApiResponse(responseCode = "200", description = "Produit modifié")
    @ApiResponse(responseCode = "400", description = "Données invalides",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Produit absent",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProduitResponse modifier(@PathVariable Integer id, @Valid @RequestBody ProduitRequest request) {
        return service.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un produit")
    @ApiResponse(responseCode = "200", description = "Produit supprimé (message de confirmation et produit supprimé)")
    @ApiResponse(responseCode = "404", description = "Produit absent",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public SuppressionResponse supprimer(@PathVariable Integer id) {
        return SuppressionResponse.of(service.supprimer(id));
    }
}
