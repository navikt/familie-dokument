# familie-dokument
Felles applikasjon for opplasting av dokumenter via S3 i SBS. 
Brukes ifm vedlegg i søknadsdialoger og generering av pdf fra html for brev 

## Kjøre lokalt for søknad for BA
For at tokensupport skal fungere må det kjøres opp en annen app som har 
mock-oauth-server kjørende på port 11588

Kjør opp `ApplicationLocalSoknad`

## Kjøre lokalt for søknad for EF
Appene i enslig forsørger bruker fakedings for å hente og validere token lokalt

Kjør opp `ApplicationLocalEfSoknad`

## Kjøre lokalt for sak/brev
Kjør opp `ApplicationLocal` som starter opp mock-oauth-server selv