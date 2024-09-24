package org.example;

public class Produkt_na_paragonie extends Produkt_z_menu{
    private int ilosc;
    private String notatka;
    private int numer_na_paragonie;

    public Produkt_na_paragonie(Produkt_z_menu produkt, int numer)
    {
        super(produkt.getNazwa(), produkt.getId(), produkt.getKod_z_kasy());
        this.numer_na_paragonie = numer;
    }

    public Produkt_na_paragonie(String nazwa, int id, int kod_kasy, int numer)
    {
        super(nazwa, id, kod_kasy);
        this.numer_na_paragonie = numer;
    }

    public int getIlosc() {
        return ilosc;
    }

    public String getNotatka() {
        return notatka;
    }

    public int getNumer_na_paragonie()
    {
        return numer_na_paragonie;
    }

    public void setNumer_na_paragonie(int numer_na_paragonie) {
        this.numer_na_paragonie = numer_na_paragonie;
    }

    public void setIlosc(int ilosc) {
        this.ilosc = ilosc;
    }

    public void setNotatka(String notatka) {
        this.notatka = notatka;
    }
}
