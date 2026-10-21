# HTML fra konsumenter av /api/html-til-pdf

Brukes av `PdfGeneratorTest`. Hver `.html` har en fasit-`.pdf` ved siden av seg.

| Mappe | Kilde |
|---|---|
| `familie-baks-mottak/` | Testressurser i familie-baks-mottak (dokgen-søknad) |
| `tilleggsstonader-sak/` | Testressurser i tilleggsstonader-sak (internt vedtak og kjøreliste) |
| `tilleggsstonader-klage/` | Testressurser i tilleggsstonader-klage (blankett og fritekstbrev) |
| `tilleggsstonader-soknad-api/` | Testressurser i tilleggsstonader-soknad-api (søknader) |
| `hm-pdf-generator/` | Testressurser i hm-pdf-generator (notat) |
| `familie-brev/` | Generert med familie-brev lokalt (se under). Brukes av familie-brev selv og familie-klage |
| `familie-ef-sak/` | Vedtaksbrev generert med familie-brev lokalt, slik ef-sak henter og sender dem |

## Generere familie-brev/ og familie-ef-sak/ på nytt

Start familie-brev lokalt (`pnpm build && NODE_ENV=development node dist/src/server/index.js`, port 8001).
Sanity-datasettene leses uten token.

- `blankett_ef.html`: `GET /blankett/dummy-html`
- `fritekst_brev_ef.html` / `fritekst_brev_baks.html`: `POST /api/fritekst-brev/html` / `POST /api/fritekst-brev/baks/html`
  med en `IFritekstbrevMedSignatur`-body
- `familie-ef-sak/*.html`: `POST /api/ef-brev/avansert-dokument/bokmaal/<brevmal>/html` med en body der alle
  flettefelt, delmaler og første valgmulighet fra `GET .../<brevmal>/felter/v3` er fylt ut med syntetiske verdier,
  og htmlfelt (f.eks. `inntektsperioderHtml`) er en beløpstabell på samme format som ef-sak-frontend lager

| Fil | Brevmal |
|---|---|
| `vedtaksbrev_innvilget_overgangsstonad.html` | `nyInnvilgetOvergangsstonadHoved2` |
| `vedtaksbrev_innvilget_barnetilsyn.html` | `innvilgetStonadTilBarnetilsynMedTabell` |
| `vedtaksbrev_innvilget_skolepenger.html` | `innvilgetStonadTilSkolepenger` |
| `vedtaksbrev_avslag_endring_inntekt.html` | `avslagIkkeTiProsentEndringInntekt` |
| `varsel_om_ny_vurdering.html` | `varselOmNyVurdering` |

Bruk kun syntetiske personidenter. Generer fasit med
`mvn test -Dtest=PdfGeneratorTest -DoppdaterPdfFasit=true` før avhengigheter oppgraderes.
