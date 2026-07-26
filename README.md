# US500 Screen Bot

Aktualizace v1.2.0: přidána vlastní ikona aplikace pro zobrazení na ploše Androidu.

zplatná Android aplikace pro přepočet MES entry zóny na US500 zónu pomocí tří screenshotů.

## Vstupy
1. Screenshot entry zóny z Discordu.
2. Screenshot uzavřené MES svíčky.
3. Screenshot uzavřené US500 svíčky stejného času z Rebels Funding nebo Fintokei.

## Výpočet
- `offset = US500 close - MES close`
- `US500 horní = MES horní + offset`
- `US500 dolní = MES dolní + offset`
- `midpoint = (horní + dolní) / 2`

## Soukromí a cena
- Aplikace nemá oprávnění k internetu.
- OCR probíhá přímo v Android zařízení.
- Žádný server, Make ani placené API.
- Google ML Kit Text Recognition používá přibalený offline model.

## Důležitý požadavek na screenshoty
Na grafech musí být označená uzavřená svíčka a jasně viditelné:
- čas svíčky,
- hodnota `C` nebo `Close`.

MES a US500 musí mít stejný tržní čas svíčky. Ne stejný okamžik pořízení screenshotu.

## Bezpečnost
Aplikace ukáže všechny rozpoznané hodnoty před výpočtem. Když OCR selže, lze hodnotu ručně opravit. Výpočet se zablokuje při rozdílném čase MES a US500.

## Sestavení zdarma přes GitHub Actions
1. Vytvoř zdarma nový soukromý GitHub repozitář.
2. Nahraj celý obsah projektu.
3. Otevři záložku **Actions**.
4. Spusť workflow **Build Android APK**.
5. Po dokončení stáhni artifact **US500-Screen-Bot-APK**.
6. Rozbal ZIP a nainstaluj `app-debug.apk` do telefonu/tabletu.

## Lokální sestavení v Android Studio
Otevři složku projektu a zvol **Build > Build APK(s)**.

## Verze 1.1.0 – oprava času
- OCR už nečte čas svíčky.
- Uživatel vybere čas jednou v aplikaci.
- OCR čte pouze entry zónu a Close MES/US500.
- Odstraněno chybné blokování „Rozdílný čas svíček“.
- Výchozí čas je automaticky nastaven přibližně na aktuální čas mínus 10 minut, zaokrouhlený na 5 minut.
