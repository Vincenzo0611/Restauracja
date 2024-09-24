package org.example;

import java.util.ArrayList;
import java.util.List;

public class Dzial_menu {
    private String nazwa;

    List<Produkt_z_menu> produkty = new ArrayList<>();

    public Dzial_menu(String nazwa)
    {
        this.nazwa = nazwa;
    }

    public String getNazwa() {
        return nazwa;
    }
}
