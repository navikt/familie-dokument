# familie-dokument
Felles applikasjon for opplasting av dokumenter via S3 i SBS. 
Brukes ifm vedlegg i søknadsdialoger og generering av pdf fra html for brev 

## Kjøre lokalt for søknad med mock-oauth-server kjørende i annen app
For at tokensupport skal fungere må det kjøres opp en annen app som har 
mock-oauth-server kjørende på port 11588

Kjør opp `ApplicationLocalSoknad`

## Kjøre lokalt for søknad med fakedings
Søknadsapper som bruker fakedings for å hente og validere token lokalt. Dette gjelder både `familie-baks-soknad-api` og `familie-ef-soknad-api`.

Kjør opp `ApplicationLocalSoknadFakedings`

## Kjøre lokalt for sak/brev
Kjør opp `ApplicationLocal` som starter opp mock-oauth-server selv