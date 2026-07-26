# Kontrolní scénáře

## Správný výpočet
- MES entry: 6412.50 / 6408.00
- MES Close: 6410.50
- US500 Close: 6357.20
- Čas obou svíček: 15:10
- Offset: -53.30
- US500 horní: 6359.20
- US500 dolní: 6354.70
- Midpoint: 6356.95

## Blokace
- MES čas 15:10, US500 čas 15:15 → výpočet se nesmí provést.
- Chybí Close → výpočet se nesmí provést.
- Horní zóna je nižší než dolní → aplikace hodnoty ze screenshotu automaticky seřadí; při ruční opravě výpočet chybné pořadí odmítne.

## Test verze 1.1 – čas
1. Vyber v aplikaci čas 15:10.
2. Nahraj MES a US500 screenshot stejné svíčky.
3. OCR nesmí porovnávat ani blokovat čas ze screenshotů.
4. Výsledek musí zobrazit zvolený čas 15:10.
