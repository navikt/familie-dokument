# Copilot Instructions – familie-dokument

## Bygg, test og lint

```bash
# Bygg og kjør alle tester
mvn verify

# Kjør én enkelt testklasse
mvn test -pl . -Dtest=StorageControllerTest

# Kun ktlint-formatering (kjøres automatisk i validate-fasen)
mvn antrun:run@ktlint-format

# Kun ktlint-sjekk
mvn antrun:run@ktlint
```

`ktlint-format` kjøres automatisk ved `mvn validate`. Lint-sjekk (`ktlint`) kjøres i `verify`-fasen.

## Kjøre lokalt

- **Sak/brev**: Kjør `ApplicationLocal` – starter mock-oauth-server selv.
- **Søknad**: Kjør `ApplicationLocalSoknad` – forutsetter at fakedings kjører eksternt for token-validering.

Maven-konfig: legg til server `at.datenwort.openhtmltopdf` med GitHub-token i `~/.m2/settings.xml` (se README).

## Arkitektur

Appen har tre ansvarsområder, eksponert på én instans:

| Ansvar | Controller | Endepunkt-prefix | Auth |
|---|---|---|---|
| Vedlegg (opplasting/henting) | `StorageController` | `/api/mapper` | TokenX Level4 |
| Søknad mellomlagring | `StonadController` | `/api/soknad` | TokenX Level4 |
| HTML→PDF-generering | `PdfController` | `/api/html-til-pdf` | Ingen |

### Lagringsstack (vedlegg)

```
MultipartFile
  → VirusScanService (ClamAV)
  → AttachmentToStorableFormatConverter
      – Tika MIME-deteksjon
      – PDF → FlattenPdfService (flater ut lag og skjemafelt)
      – JPEG/PNG → ImageConversionService (skalerer og roterer etter EXIF)
  → AttachmentStorage
      → EncryptedStorage (AES-kryptering per FNR)
          → GcpStorageWrapper (GCP Cloud Storage)
```

### Lagringsstack (søknad/mellomlagring)

```
JSON-streng
  → MellomLagerService (retry ved GcpRateLimitException)
      → EncryptedStorage (samme som over)
          → GcpStorageWrapper (separat bøtte)
```

### Nøkkelkonvensjoner

- **Lagringsmappe**: FNR hashes med SHA-256 + salt (`FAMILIE_DOKUMENT_FNR_SECRET_SALT`) via `Hasher.lagFnrHash()`. Hashen brukes som katalognavn i GCP-bøtten.
- **Kryptering**: Data krypteres med brukerens FNR som nøkkelkontekst. Krypteringsnøkkel styres av `FAMILIE_DOKUMENT_STORAGE_ENCRYPTION_PASSWORD`.
- **To GCP-bøtter**: én for vedlegg (`ATTACHMENT_GCP_STORAGE`) og én for søknadsdata (`STONAD_GCP_STORAGE`). Bøttenavn settes via `GCP_STORAGE_BUCKETNAME`.
- **`{bucket}`-path-parameteret** i `StorageController` er ikke i bruk – `familievedlegg`-bøtten benyttes alltid uavhengig av verdien.
- **Stønadsnøkler**: `StonadController.StønadParameter`-enumen definerer gyldige stønadskategorier for mellomlagring.

### Integrasjonstester

Integrasjonstester arver fra `OppslagSpringRunnerTest`, som setter opp `MockOAuth2Server` og `WireMock`. TokenX-tokens genereres med `søkerBearerToken()`. Legg nye integrasjonstester under `storage/integrationTest/`.

## Miljøvariabler (påkrevd)

| Variabel | Formål |
|---|---|
| `FAMILIE_DOKUMENT_FNR_SECRET_SALT` | Salt for SHA-256-hashing av FNR |
| `FAMILIE_DOKUMENT_STORAGE_ENCRYPTION_PASSWORD` | Krypteringsnøkkel for lagret data |
| `GCP_STORAGE_BUCKETNAME` | Navn på GCP-bøtten |
