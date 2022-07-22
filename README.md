# familie-dokument
Felles applikasjon for opplasting av dokumenter via S3 i SBS. 
Brukes ifm vedlegg i søknadsdialoger og generering av pdf fra html for brev 

## Kjøre lokalt for søknad
For at tokensupport skal fungere må det kjøres opp en annen app som har 
mock-oauth-server kjørende på port 11588 - f.eks. `familie-ef-soknad-api` for EF sin del.

Kjør opp `DevLauncherForSøknad`

## Kjøre lokalt for sak/brev
Kjør opp `DevLauncher` som starter opp mock-oauth-server selv