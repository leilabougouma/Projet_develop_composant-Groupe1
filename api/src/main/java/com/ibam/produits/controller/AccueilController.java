package com.ibam.produits.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Hidden
public class AccueilController {

    @GetMapping("/")
    public String accueil() {
        return "redirect:/swagger-ui/index.html";
    }
}
