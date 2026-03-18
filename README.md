# BSI Tečajnica in Analiza

Preprosta namizna aplikacija v Javi za pregled tečajev Banke Slovenije, izris grafov in izračun oportunitetnih zaslužkov/izgub med dvema valutama.

## Tehnologije
* **Java** (JDK 11 ali novejši)
* **Maven** (za gradnjo in upravljanje odvisnosti)
* **JFreeChart** (edina zunanja knjižnica, uporabljena za čist izris grafov)
* **Java DOM Parser & Swing** (vgrajena orodja za obdelavo XML in uporabniški vmesnik)

## Namestitev in zagon

1. **Gradnja projekta:**
   V korenski mapi projekta (kjer se nahaja datoteka `pom.xml`) v terminalu zaženite naslednji ukaz, ki bo prenesel odvisnosti in zgradil projekt:
   ```bash
   mvn package

2. **Gradnja projekta:**
   Po uspešni gradnji se bo ustvarila mapa target. Program zaženite z ukazom:
   ```bash
   java -jar target/tecajnica-app-1.0-SNAPSHOT.jar

3. **Funkcionalnost**

    **Samodejen prenos podatkov:** Program ob zagonu samodejno prebere in razčleni XML datoteko neposredno s strežnika BSI.

    **Analiza izbranega obdobja:** Uporabnik vnese začetni in končni datum (v formatu LLLL-MM-DD). Aplikacija ustvari tabelarični ter grafični prikaz gibanja tečajev za poljubni dve izbrani valuti.

    **Oportunitetni izračun:** Za izbrano obdobje se na podlagi prvega in zadnjega veljavnega tečaja izračuna hipotetičen donos za vložek 1000 EUR, ter izpiše, katera valuta bi bila donosnejša.
