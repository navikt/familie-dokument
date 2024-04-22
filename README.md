# familie-dokument
Felles applikasjon for opplasting av dokumenter via S3 i SBS. 
Brukes ifm vedlegg i søknadsdialoger og generering av pdf fra html for brev

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
