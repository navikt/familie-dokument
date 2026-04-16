# familie-dokument
Felles applikasjon for opplasting av dokumenter via S3 i SBS. 
Brukes ifm vedlegg i søknadsdialoger og generering av pdf fra html for brev

## Funksjonalitet

### `api/mapper` - Vedlegg

- **POST** `/{bucket}` — Last opp en fil (multipart)
- **POST** `/merge/{bucket}` — Slå sammen flere lagrede PDFer til én
- **GET** `/{bucket}/{dokumentId}` — Hent et vedlegg som `Ressurs<ByteArray>`
- **GET** `/{bucket}/{dokumentId}/pdf` — Hent et vedlegg som rå byte-strøm

Håndterer opplasting av vedlegg fra søknadsdialoger.

#### 🦠 Virusskanning

Hvert opplastet vedlegg skannes via **Nais/ClamAV** (`http://clamav.nais-system`) før det behandles videre. Oppdages virus, returneres `400 Bad Request`.
Se [antivirus på nais.io](https://doc.nais.io/services/antivirus/).

#### 🧹 Normalisering

Etter virusskanning normaliseres filen av `AttachmentToStorableFormatConverter`.

##### Filtypedeteksjon med Apache Tika

Før filen behandles videre må tjenesten vite hva slags fil det faktisk er. Det løses med **Apache Tika**, som leser de første bytene i filen og gjenkjenner formatet ut fra den såkalte _magic bytes_-signaturen – et fast mønster som alle filer av en gitt type starter med. En PDF begynner for eksempel alltid med `%PDF-`, en JPEG med `FF D8 FF`. Dette er viktig fordi **filnavnet og filendelsen ikke er til å stole på**.

Tjenesten godtar kun tre typer (definert i `Format`-enumen):

| MIME-type | Handling |
|---|---|
| `application/pdf` | Flates ut via `FlattenPdfService` |
| `image/jpeg` | Konverteres til PDF via `ImageConversionService` |
| `image/png` | Konverteres til PDF via `ImageConversionService` |

Alt annet (Word-dokumenter, tekstfiler, ZIP-arkiver osv.) avvises med `400 Bad Request`.

- **Bilder (JPEG/PNG)** → skaleres og roteres etter EXIF-metadata via `ImageConversionService`
- **PDFer** → flates ut via `FlattenPdfService`, som fjerner interaktive skjemafelt med PDFBox `acroForm.flatten()`. Dette sikrer at vedlegg ikke inneholder redigerbart innhold når de arkiveres.

#### 🔒 Kryptering og lagring

Etter normalisering krypteres dataene med **AES/GCM** via `EncryptedStorage`. Krypteringsnøkkelen er utledet fra brukerens fødselsnummer, og lagringsmappen navngis etter en **SHA-256-hash av fødselsnummeret + et hemmelig salt** (`Hasher.lagFnrHash()`). Data er dermed isolert per innbygger i GCP-bøtten, og ingen kan lese andres filer uten riktig nøkkelkontekst.

---

### `api/soknad` — Mellomlagring av søknad

- **POST** `/{stonad}` — Mellomlagre søknadsdata (JSON)
- **GET** `/{stonad}` — Hent mellomlagret søknad
- **DELETE** `/{stonad}` — Slett mellomlagret søknad

Brukes til å lagre søknadsdata midlertidig mens brukeren fyller ut en søknad, slik at data ikke går tapt ved navigering eller avbrudd.

#### 🔒 Kryptering og lagring

Bruker samme krypterings- og hash-mekanisme som vedleggsmodulen, men data lagres i en **separat GCP-bøtte**. `MellomLagerService` har innebygd retry-logikk (maks 3 forsøk med 1 sekunds intervall) ved `GcpRateLimitException`.

---

### `api/html-til-pdf` — PDF-generering

- **POST** `/html-til-pdf` — Konverter HTML til PDF (returnerer bytes)

Tar imot rå HTML og returnerer en ferdig PDF. Brukes av bl.a. `familie-brev` og `familie-klage` for å generere brev og vedtak.

#### Teknologi

| Bibliotek | Rolle |
|---|---|
| **Jsoup** | Parser HTML-strengen og bygger en DOM-struktur |
| **openhtmltopdf** | Rendrer DOM-en til PDF (HTML/CSS-motor tilpasset PDF-utdata) |
| **Apache Batik** | Gjengir SVG-grafikk inne i HTML-en |
| **Apache PDFBox** | Lavnivå PDF-manipulasjon: fonter, metadata, fargeprofil, sammenslåing |
| **Apache XmpBox** | Skriver XMP-metadata inn i PDF-filen |

#### Prosessteg

```
HTML-streng
  → [Jsoup] parser til DOM
  → [openhtmltopdf] rendrer DOM til PDF i minnet
      – Source Sans Pro (regular/bold/kursiv) lastes inn og bygges inn i PDF-en
      – SVG-elementer gjengis via Batik
  → [conform()] gjør PDF-en arkivklar (se PDF/A nedenfor)
  → PDF-bytes returneres til kaller
```

Fonten **Source Sans Pro** er valgt fordi den er åpen lisens (SIL Open Font License) og god lesbarhet på trykk. Alle tre snitt (regular, bold, kursiv) bygges inn i PDF-filen slik at dokumentet ser likt ut uansett hvilken PDF-leser som brukes.

#### 📄 Hva er PDF/A og hvorfor brukes det?

Vanlige PDFer kan inneholde referanser til fonter eller ressurser som ligger eksternt – de kan derfor bli uleselige om noen år om disse ressursene forsvinner. **PDF/A** er en ISO-standard for langtidsarkivering som sier at alt som trengs for å vise dokumentet (fonter, farger, metadata) _må_ være innebygd i selve filen. Norske offentlige dokumenter er pålagt å følge denne standarden.

Tjenesten bruker **PDF/A-2A**, der:
- **2** betyr versjon 2 av standarden (basert på PDF 1.7, støtter bl.a. lag og transparens)
- **A** betyr «Accessible» – dokumentet har logisk strukturinformasjon (tagging) som gjør det lesbart for skjermlesere og annen hjelpeteknologi

#### Hva gjør `conform()`

Etter at selve PDF-en er generert, kjøres en etterbehandling som gjør den PDF/A-2A-kompatibel:

1. **XMP-metadata** — maskinlesbar informasjon om dokumentet skrives inn: hvem som lagde det (`navikt/familie-dokument`), dato, og en markør som forteller PDF-lesere at dette er PDF/A-2A
2. **sRGB-fargeprofil** — en standard fargebeskrivelse (`sRGB.icc`) bygges inn så farger gjengis konsistent på alle skjermer og skrivere
3. **Tilgjengelighetstagging** — dokumentet merkes som «tagged» og får et strukturtre, noe som gjør det søkbart og tilgjengelig for hjelpeteknologi
4. **Språk** — settes til `nb-NO` så bl.a. stavekontroll og skjermlesere vet at innholdet er norsk bokmål
5. **Dokumenttittel** — PDF-lesere vises tittelfeltet i tittellinjen i stedet for filnavnet


## Maven-oppsett
Så lenge vi bruker en spesialversjon av openhtmltopdf må du legge inn ny server i `~/.m2/settings.xml`-fila lokalt:
```
    <server>
      <id>at.datenwort.openhtmltopdf</id>
      <username>navikt</username>
      <password>[TOKEN-DU-BRUKER-TIL-DETTE]</password>
    </server>
```

## Kjøre lokalt for søknad
Søknadsapper som bruker fakedings for å hente og validere token lokalt. Dette gjelder både `familie-baks-soknad-api` og `familie-ef-soknad-api`.

Kjør opp `ApplicationLocalSoknad`

## Kjøre lokalt for sak/brev
Kjør opp `ApplicationLocal` som starter opp mock-oauth-server selv
