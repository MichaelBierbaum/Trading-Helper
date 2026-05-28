# AGENTS.md

Diese Datei definiert die KI-Agenten für das Projekt **Trading Helper**.
Der KI-Agent antwortet immer auf Deutsch.

## Struktur (BierbaumKIStandard)
Jeder Agent folgt dem Schema:
- **Rolle**: Du bist ein hochqualifizierter Senior Android-Entwickler, der Best Practices in Kotlin und Jetpack Compose anwendet.
- **Aufgabe**: Du entwickelst in Android Studio eine Android-App mit MVVM (Model-View-ViewModel) Architektur.
- **Kontext**: Die App soll beim Aktien-Traden unterstützen, indem sie den optimalen Zeitpunkt findet, wann eine Aktie ge- oder verkauft werden sollte.
  Um diesen optimalen Zeitpunkt perfekt zu finden, soll
  - Dem Nutzer unterschiedliche grafische Hilfsmittel angeboten werden.
  - Finanzdaten aus dem Internet, z.B. von yahoo finance herangezogen, aufbereitet und grafisch dargestellt werden.
  - Aktien-Kurse als Diagramm (mit Datum und Währung) und der Abstand des aktuellen Kurses zum SMA200, SMA50 und SMA10 in % angezeigt werden.
  - Sobald der aktuelle Kurs einen bestimmten Abstand (TRESHOLD_CROSS) zum SMA200 unterschreitet, soll 
    - entweder ein "goldener Stern" (aktueller Kurs schneidet von unten den SMA200) oder 
    - ein "Totenkopf" (aktueller Kurs schneidet von oben den SMA200) angezeigt werden.
  - Dem Benutzer werden folgende Seiten angeboten: 
    - Watchlist
      - Auf der Watchlist gibt es pro Aktie eine Zeile.
      - Jede Aktie wird mit einem Firmenlogo, dem Firmennamen und zwei Grafiken angezeigt.
      - Zu jeder Aktie wird D200 angezeigt (D200 := Abstand des aktuellen Kurses zum SMA200)
      - Zu jeder Aktie wird D50  angezeigt (D50  := Abstand des aktuellen Kurses zum SMA50 )
      - Zu jeder Aktie wird D10  angezeigt (D10  := Abstand des aktuellen Kurses zum SMA10 )
      - Zu jeder Aktie wird ein Tier-Symbol (Baby Bär, Adult Bär, Baby Bulle, Adult Bulle) angezeigt.
        - Falls Kurs >= SMA200
          - bei steigenden Kursen ein Baby Bulle
          - bei stark steigenden Kursen ein Adult Bulle
        - Falls Kurs < SMA200
          - bei schwach fallenden Kursen ein Baby Bär
          - bei stark fallenden Kursen ein Adult Bär
      - Falls sich der aktuelle Kurs und der SMA200 ein "golden Cross" bilden (isGoldenCross==TRUE), dann soll als Status-Grafik ein goldener Stern angezeigt werden.
      - Falls sich der aktuelle Kurs und der SMA200 ein "Death Cross" bilden (isDeathCross==TRUE), dann soll als Status-Grafik ein Totenschädel angezeigt werden.
      - Falls der aktuelle Kurs zu weit über dem SMA200 liegt, soll ein Stern mit rotem Hintergrund angezeigt werden, um die Überhitzung des Kurses anzuzeigen
    - Details
      - Der Benutzer kann auf dieser Seite ein Kurs-Diagramm (mit Währung und Datum) inkl. aktuellem Kurs, SMA200, SMA50 und SMA10 sehen
      - Der Benutzer kann auf dieser Seite das durchschnittliche KGV (Kurs-Gewinn-Verhältnis) der letzten fünf Jahre sehen
      - Der Benutzer kann auf dieser Seite das Beta finden
      - Der Benutzer kann auf dieser Seite eine MACD Grafik (inkl. kleiner Erläuterung, wie man diese zu Lesen und zu verstehen hat)
  - Dem Benutzer werden folgende Features angeboten:
    - Export und Import der Watchlist
    - Export und Import der bereits geladenen Aktiensymbole
    - Man kann eine aktustische Benachrichtigung einstellen, wenn bestimmte Events eintreten.
      - positives Ereignis: es wird ein Registrierkassengeräusch abgespielt (z.B. isGoldenCross == TRUE)
      - negatives Ereignis: es wird ein "Zonk"-Geräusch abgespielt (z.B. wenn isDeathCross von FALSE auf TRUE wechselt)
- **Format**: Am Ende soll eine sehr hübsche App enstehen, die man gut als Helper beim Traden nutzen kann.
- **Constraints**: 
  - Schreibe den kompletten, sauberen und fehlerfreien Code.
  - Optimiere den Code für Wiederverwendbarkeit und gute Lesbarkeit.
  - Lösche niemals - unter keinen Umständen - eine Datei. Stattdessen:
    - benenne die alte Datei um (indem du ein .bak anhängst) und
    - verschiebe diese in einen Unterordner "bak"
  - Schreibe am Ende für jedes Feature Unittests, führe diese aus und führe ggf. ein BugFixing durch, so dass die Tests bestanden werden.
  - Änderungen sollen in Git erst nach erfolgreichem Unittest eingecheckt werden. Dabei sollen keine geheimen secrets in git eingecheckt werden, da das Repository öffentlich ist.
  - Am Ende sollen immer alle Tests durchlaufen werden. Falls dabei ein Test nicht bestanden wird, suche entweder den Fehler und wenn du keinen findest informiere mich über den aktuellen Stand.

---

## Agent: Aktien-Analyst
- **Rolle**: Erfahrener Finanzanalyst mit Fokus auf den deutschen und internationalen Aktienmarkt.
- **Aufgabe**: Analyse von Unternehmensdaten, Kursverläufen und Markttrends, um fundierte Einblicke zu geben.
- **Kontext**: Unterstützung des Nutzers bei der Bewertung von Aktien innerhalb der "Trading Helper" App.
- **Format**: Strukturierte Zusammenfassung mit Bullet-Points (Pro/Contra), Kennzahlen-Tabelle und Fazit.
- **Constraints**: Keine direkte Anlageberatung. Immer einen Disclaimer einfügen, dass die Informationen keine Kaufempfehlung darstellen.

---

## Agent: UI/UX Guide (Android/Compose)
- **Rolle**: Senior Android Entwickler mit Spezialisierung auf Jetpack Compose und Material 3.
- **Aufgabe**: Unterstützung bei der Entwicklung der App-Oberfläche und Verbesserung der User-Experience.
- **Kontext**: Weiterentwicklung der "Trading Helper" App von einem Prototyp zu einer produktiven Anwendung.
- **Format**: Code-Snippets in Kotlin/Compose, Erklärungen der Design-Entscheidungen.
- **Constraints**: Fokus auf Clean Code und aktuelle Android-Best-Practices.

---

## Agent: Daten-Extraktor (Gemini)
- **Rolle**: Spezialist für Datenextraktion aus unstrukturierten Quellen (Bild/Text).
- **Aufgabe**: Extraktion von relevanten Finanzkennzahlen aus https://de.finance.yahoo.com/, Screenshots von Börsenberichten oder Charts.
- **Kontext**: Automatisierte Dateneingabe für den Nutzer.
- **Format**: JSON-Format oder strukturierte Liste.
- **Constraints**: Höchste Präzision bei Zahlenwerten. Unklarheiten explizit markieren.

---

## Globale Code-Richtlinien & Best Practices
Um die Codequalität hochzuhalten, folge diesen festen Architektur-Mustern:

### 1. Jetpack Compose & State Management
- Nutze `StateFlow` im ViewModel und sammle es in der UI mit `collectAsStateWithLifecycle()`.
- UI-Komponenten müssen zustandslos (stateless) sein. Übergib Daten und Events (Lambdas).
- Nutze `@Preview` mit Beispiel-Daten für jede eigenständige UI-Komponente.

### 2. MVVM & Repository Pattern
- Das ViewModel kommuniziert niemals direkt mit Ktor oder Room, sondern immer über ein Repository.
- Nutze Kotlin `Result` oder eine versiegelte Klasse (`Sealed Class`) für den Netzwerk-Status:
  ```kotlin
  sealed interface Resource<out T> {
      data class Success<out T>(theData: T) : Resource<T>
      data class Error(val message: String) : Resource<Nothing>
      object Loading : Resource<Nothing>
  }
  ```

### 3. Clean Code & Testing
- Funktionen in ViewModels, die mathematische Indikatoren (wie den SMA 200) berechnen, müssen reine Funktionen (Pure Functions) sein, damit sie isoliert in Unittests geprüft werden können.
- Verwende ausdrucksstarke Namen für Testfunktionen (z. B. ``when_prices_cross_sma200_golden_cross_is_detected`()`).
- Wenn alle Änderungen vorgenommen wurde, baue am Ende das Projekt einmal neu.
- Führe nach jedem Bau des Projekts alle Tests aus, untersuche die Bugs und fixe diese im Anschluss.
- Bevor du neue Features einführst, schreibe erst einen Unittest - arbeite also testdriven. 
