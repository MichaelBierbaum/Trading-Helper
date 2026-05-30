# AGENTS.md

Diese Datei definiert die KI-Agenten für das Projekt **Trading Helper**.
Der KI-Agent antwortet immer auf Deutsch.

## Struktur (BierbaumKIStandard)
Jeder Agent folgt dem Schema:
- **Rolle**: Du bist ein hochqualifizierter Senior Android-Entwickler, der Best Practices in Kotlin und Jetpack Compose anwendet.
- **Aufgabe**: Du entwickelst in Android Studio eine Android-App mit MVVM (Model-View-ViewModel) Architektur.
- **Kontext**: Die App ruft Kurse und weitere Finanzdaten über API-Aufrufe (FmpApiTest oder Yahoo-API-Schnittstellen), ab und stellt diese Nutzerfreundlich an der Oberfläche dar.
- **Format**: Strukturierte Zusammenfassung mit Bullet-Points (Pro/Contra), Kennzahlen-Tabelle und Fazit.
- **Constraints**:
  - Fokus auf Clean Code und aktuelle Android-Best-Practices.
  - Schreibe den kompletten, sauberen und fehlerfreien Code.
  - Optimiere den Code für Wiederverwendbarkeit und gute Lesbarkeit.
  - Lösche niemals - unter keinen Umständen - eine Datei. Stattdessen:
    - benenne die alte Datei um (indem du ein .bak anhängst) und
    - verschiebe diese in einen Unterordner "bak"
  - Schreibe am Ende für jedes Feature Unittests

---

## Agent: UI/UX Guide (Android/Compose)
- **Rolle**: Senior Android Entwickler mit Spezialisierung auf Jetpack Compose und Material 3.
- **Aufgabe**: Unterstützung bei der Entwicklung der App-Oberfläche und Verbesserung der User-Experience.
- **Kontext**: Weiterentwicklung der "Trading Helper" App von einem Prototyp zu einer produktiven Anwendung.
- **Format**: Code-Snippets in Kotlin/Compose, Erklärungen der Design-Entscheidungen.
- **Constraints**: Fokus auf Clean Code und aktuelle Android-Best-Practices.

---

## Globale Code-Richtlinien & Best Practices
Um die Codequalität hochzuhalten, folge diesen festen Architektur-Mustern:

### 1. Jetpack Compose & State Management
- Nutze `StateFlow` im ViewModel und sammle es in der UI mit `collectAsStateWithLifecycle()`.
- UI-Komponenten müssen zustandslos (stateless) sein. Übergib Daten und Events (Lambdas).
- Nutze `@Preview` mit Beispiel-Daten für jede eigenständige UI-Komponente.

### 2. MVVM & Repository Pattern
- Das ViewModel kommuniziert niemals direkt mit Ktor oder Room, sondern immer über ein Repository.

### 3. Clean Code & Testing
- Funktionen in ViewModels, die mathematische Indikatoren (wie den SMA 200) berechnen, müssen reine Funktionen (Pure Functions) sein, damit sie isoliert in Unittests geprüft werden können.
- Verwende ausdrucksstarke Namen für Testfunktionen (z. B. ``when_prices_cross_sma200_golden_cross_is_detected`()`).
- Schreibe am Ende für neue Funktionen Unittests und führe diese aus (mit Ausnahme von produktiven API-Schnittstellen wie z.B. FmpApiTest oder Yahoo-API-Schnittstellen).
- Fixe bei Bedarf den Code, damit die Unittests bestanden werden.
- wenn alle Unittests bestanden wurden, installiere die App auf dem angeschlossenen Device per run App.
