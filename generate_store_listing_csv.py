import csv
import re
import sys

# Complete translations for Google Play Store Listing (PureLock v2.8)
# Strict Google Play Console Constraints:
# - Title <= 30 chars
# - Short Description <= 80 chars
# - Full Description <= 4000 chars (Must include AccessibilityService API Disclosure)
# - Policy Truthfulness: Zero ads, zero analytics/telemetry SDKs, zero cloud accounts,
#   100% local on-device encrypted storage (do NOT claim "no Internet permission" since
#   optional Google Play Billing v8 supporter donations merge Play Billing permissions).

listings = [
    {
        "locale": "en-US",
        "language": "English (United States)",
        "title": "PureLock: Privacy App Lock",
        "short_desc": "Offline App Lock & AES-256 Vault. Zero ads, zero telemetry, zero cloud.",
        "full_desc": """PureLock is a lightweight, ad-free privacy suite and encrypted vault built to protect your apps, passwords, 2FA codes, and private notes strictly on your device. Powered by SQLCipher AES-256 and hardware-backed Android Keystore encryption, PureLock runs offline with zero ads, zero telemetry, and zero cloud accounts.

🔒 CORE SECURITY FEATURES:
• App Shield (App Lock) & Custom Schedules: Protect any installed app (Banking, Social, Photos, System) with PIN, 3×3 Pattern (invisible trail), 4-Quadrant Knock Code, or Biometrics (Fingerprint / Face ID). Set per-app time windows and active days, plus Randomized Keypad Scramble and configurable re-lock grace periods.
• Hardware-Encrypted Vault & Credentials: Store passwords, secure notes, cards, 2FA codes, and PINs in a SQLCipher AES-256 database wrapped by Android Keystore TEE, with master credentials encrypted at rest using AES-256-GCM. Includes Favorites, Pinning, configurable Trash auto-purge (7–90 days), and Password Entropy Insights.
• Active Anti-Tamper & Stealth Decoys: Progressive brute-force lockout cooldowns, app-wide screenshot & Recents blocking (FLAG_SECURE), working Calculator disguise, Fake Crash dialog, and a Panic Duress PIN.
• Shake-to-Lock Gesture: Gravity-filtered accelerometer emergency lockdown with adjustable sensitivity (High / Medium / Low).
• Intruder Capture & SHA-256 Audit: Records failed unlock attempts locally with a SHA-256 Cryptographic Vault Integrity Audit and CSV log export.
• Clipboard Auto-Wipe & Password Generator: Generates high-entropy passwords offline and automatically clears copied secrets from clipboard memory.
• Authenticated Offline Autofill, Backups & TV: Master-locked local Android Autofill Service, AES-256-GCM encrypted .plk backup & restore (PBKDF2-HMAC-SHA256) including apps and schedules, Quick Settings Lockdown tiles, Home Screen Vault Widget, and Android TV support.

🛡️ 100% LOCAL PRIVACY GUARANTEE:
Zero ads, zero analytics or telemetry SDKs, zero Advertising ID, and zero cloud accounts. Your encrypted vault, credentials, schedules, and logs never leave your device.

────────────────────────────────────────
ACCESSIBILITY SERVICE API USAGE & DISCLOSURE
────────────────────────────────────────
PureLock uses the AccessibilityService API strictly to provide its core App Lock (App Shield) feature.

• How it is used: PureLock monitors window state change events (TYPE_WINDOW_STATE_CHANGED) in real time solely to detect when a user-protected application is opened in the foreground. Once detected, PureLock immediately presents a secure PIN, Pattern, Knock Code, or Biometric lock overlay to prevent unauthorized access.
• Privacy & Data Protection: PureLock DOES NOT read, capture, or access user screen content, keystrokes, passwords, or personal messages (canRetrieveWindowContent is disabled).
• Zero Data Collection: PureLock does not collect, upload, or transmit your personal vault data, app usage, or credentials.

Accessibility service permission is optional and only requested when you choose to protect applications with App Shield. It can be enabled or disabled anytime in your device's Accessibility Settings."""
    },
    {
        "locale": "es-419",
        "language": "Spanish (Latin America)",
        "title": "PureLock: Bloqueo de Apps",
        "short_desc": "Bloqueo de apps offline y bóveda AES-256. Sin anuncios, rastreo ni nube.",
        "full_desc": """PureLock es una suite de privacidad sin anuncios y bóveda cifrada offline para proteger tus aplicaciones, contraseñas, códigos 2FA y notas privadas directamente en tu dispositivo con SQLCipher AES-256 y Android Keystore.

🔒 FUNCIONES PRINCIPALES:
• Escudo de Apps (App Shield) y Horarios: Protege cualquier aplicación con PIN, Patrón 3×3 (trazo invisible), Código Knock de 4 cuadrantes o Biometría. Configura horarios y días por app, teclado aleatorio y periodo de gracia.
• Bóveda y Credenciales Cifradas por Hardware: Guarda contraseñas, notas seguras, tarjetas y 2FA en SQLCipher AES-256 con credenciales cifradas en reposo mediante Android Keystore AES-256-GCM, papelera con autopurga configurable y análisis de entropía.
• Defensa Activa y Camuflaje: Enfriamiento progresivo contra fuerza bruta, bloqueo de capturas de pantalla (FLAG_SECURE), Calculadora funcional, error falso y PIN de coacción (Duress).
• Agitar para Bloquear: Bloqueo de emergencia instantáneo al agitar el teléfono con sensibilidad ajustable.
• Registro de Intrusos y Auditoría SHA-256: Registra intentos fallidos localmente, verifica la integridad criptográfica SHA-256 y exporta registros en CSV.
• Autolimpieza del Portapapeles y Generador Offline: Crea contraseñas seguras y borra automáticamente los secretos copiados.
• Autocompletado Autenticado, Copias .plk y TV: Autocompletado offline protegido por bloqueo maestro, copias de seguridad cifradas AES-256-GCM (.plk), mosaicos rápidos, widget de inicio y soporte para Android TV.

🛡️ GARANTÍA DE PRIVACIDAD LOCAL:
Sin anuncios, sin telemetría ni rastreadores, sin ID de publicidad y sin cuentas en la nube. Tu bóveda cifrada, credenciales y registros nunca salen de tu dispositivo.

────────────────────────────────────────
USO Y DIVULGACIÓN DE LA API DE ACCESIBILIDAD
────────────────────────────────────────
PureLock utiliza la API de AccessibilityService estrictamente para proporcionar su función principal de Bloqueo de Aplicaciones (App Shield).

• Cómo se utiliza: PureLock monitorea eventos de cambio de ventana (TYPE_WINDOW_STATE_CHANGED) exclusivamente para detectar cuándo se abre una app protegida en primer plano y mostrar de inmediato la pantalla de bloqueo seguro (PIN, Patrón, Knock o Biometría).
• Privacidad y Seguridad: PureLock NO lee, captura ni accede al contenido de la pantalla, pulsaciones de teclas, contraseñas ni mensajes personales (canRetrieveWindowContent desactivado).
• Cero recolección de datos: PureLock no recopila, sube ni transmite tu bóveda, actividad de apps ni credenciales.

Este permiso es opcional, solo se solicita al activar App Shield y puede habilitarse o desactivarse en cualquier momento desde Ajustes de Accesibilidad."""
    },
    {
        "locale": "es-ES",
        "language": "Spanish (Spain)",
        "title": "PureLock: Bloqueo de Apps",
        "short_desc": "Bloqueo de apps offline y bóveda AES-256. Sin anuncios, rastreo ni nube.",
        "full_desc": """PureLock es una suite de seguridad sin anuncios y bóveda cifrada offline diseñada para proteger tus aplicaciones, contraseñas, códigos 2FA y notas privadas en local mediante SQLCipher AES-256 y Android Keystore.

🔒 FUNCIONES PRINCIPALES:
• Escudo de Apps (App Shield) y Horarios: Bloquea cualquier aplicación con PIN, Patrón 3×3 (trazo invisible), Código Knock de 4 cuadrantes o Biometría, con horarios personalizados por app, teclado aleatorio y periodo de gracia.
• Bóveda y Credenciales Cifradas por Hardware: Almacena contraseñas, notas, tarjetas y 2FA en SQLCipher AES-256 y protege tus credenciales maestras en reposo con Android Keystore AES-256-GCM, papelera con autopurga configurable y análisis de entropía.
• Protección Activa y Señuelos: Bloqueo progresivo ante intentos fallidos, protección contra capturas de pantalla (FLAG_SECURE), Calculadora funcional, aviso de cierre falso y PIN de coacción (Duress).
• Agitar para Bloquear: Bloqueo inmediato al agitar el dispositivo con sensibilidad regulable.
• Captura de Intrusos y Auditoría SHA-256: Registra accesos fallidos en local, verifica la integridad criptográfica SHA-256 y exporta en CSV.
• Portapapeles Seguro y Generador Offline: Genera claves robustas y limpia automáticamente el portapapeles tras copiar.
• Autocompletado Local, Copias .plk y TV: Autocompletado offline autenticado, copias cifradas AES-256-GCM (.plk), mosaicos de Ajustes Rápidos, widget de inicio y Android TV.

🛡️ GARANTÍA DE PRIVACIDAD LOCAL:
Sin anuncios, sin telemetría ni rastreo y sin cuentas en la nube. Tu bóveda cifrada, credenciales y registros nunca abandonan tu dispositivo.

────────────────────────────────────────
USO Y DIVULGACIÓN DE LA API DE ACCESIBILIDAD
────────────────────────────────────────
PureLock utiliza la API de AccessibilityService estrictamente para ofrecer su función principal de Bloqueo de Aplicaciones (App Shield).

• Cómo se utiliza: PureLock supervisa los eventos de cambio de ventana (TYPE_WINDOW_STATE_CHANGED) únicamente para detectar cuándo se abre una app protegida y mostrar al instante el bloqueo seguro (PIN, Patrón, Knock o Biometría).
• Privacidad y Seguridad: PureLock NO lee, captura ni accede al contenido de la pantalla, pulsaciones, contraseñas ni mensajes personales (canRetrieveWindowContent desactivado).
• Cero recopilación de datos: PureLock no recopila, sube ni transmite tus datos cifrados, actividad de apps ni credenciales.

Este permiso es opcional y solo se solicita al proteger apps con App Shield. Puedes activarlo o desactivarlo cuando quieras en Ajustes de Accesibilidad."""
    },
    {
        "locale": "pt-BR",
        "language": "Portuguese (Brazil)",
        "title": "PureLock: Bloqueio de Apps",
        "short_desc": "Bloqueio de apps offline e cofre AES-256. Sem anúncios, rastreio ou nuvem.",
        "full_desc": """PureLock é um pacote de privacidade sem anúncios e cofre criptografado offline para proteger aplicativos, senhas, códigos 2FA e notas privadas no seu aparelho com SQLCipher AES-256 e Android Keystore.

🔒 RECURSOS PRINCIPAIS:
• App Shield (Bloqueio de Apps) e Horários: Proteja qualquer aplicativo com PIN, Padrão 3×3 (trilha invisível), Knock Code de 4 quadrantes ou Biometria. Configure horários e dias por app, teclado embaralhado e tempo de carência.
• Cofre e Credenciais Criptografados por Hardware: Guarde senhas, notas seguras, cartões e códigos 2FA em SQLCipher AES-256, com credenciais mestras cifradas em repouso no Android Keystore AES-256-GCM, lixeira configurável e análise de entropia.
• Defesa Ativa e Disfarces: Bloqueio progressivo contra força bruta, proteção contra capturas de tela (FLAG_SECURE), Calculadora funcional, falha falsa e PIN de coação (Duress).
• Agite para Bloquear: Bloqueio de emergência instantâneo ao agitar o celular com sensibilidade ajustável.
• Registro de Intrusos e Auditoria SHA-256: Registra tentativas inválidas localmente, verifica integridade criptográfica SHA-256 e exporta CSV.
• Limpeza da Área de Transferência e Gerador Offline: Cria senhas fortes e apaga segredos copiados automaticamente.
• Preenchimento Automático, Backup .plk e TV: Autofill offline autenticado, backup e restauração cifrados AES-256-GCM (.plk), blocos rápidos, widget e suporte a Android TV.

🛡️ PRIVACIDADE 100% LOCAL:
Sem anúncios, sem telemetria ou rastreamento e sem contas na nuvem. Seu cofre cifrado, credenciais e registros nunca saem do seu dispositivo.

────────────────────────────────────────
USO E DIVULGAÇÃO DA API ACCESSIBILITYSERVICE
────────────────────────────────────────
O PureLock utiliza a API AccessibilityService estritamente para fornecer sua função principal de Bloqueio de Aplicativos (App Shield).

• Como é utilizado: O PureLock monitora eventos de alteração de janela (TYPE_WINDOW_STATE_CHANGED) exclusivamente para detectar quando um app protegido é aberto em primeiro plano e exibir imediatamente a tela de bloqueio seguro (PIN, Padrão, Knock ou Biometria).
• Privacidade e Proteção de Dados: O PureLock NÃO lê, captura ou acessa conteúdo da tela, digitação, senhas ou mensagens pessoais (canRetrieveWindowContent desativado).
• Zero Coleta de Dados: O PureLock não coleta, envia nem transmite dados do cofre, uso de apps ou credenciais.

A permissão é opcional, solicitada apenas ao ativar o App Shield, e pode ser alterada a qualquer momento nas Configurações de Acessibilidade."""
    },
    {
        "locale": "pt-PT",
        "language": "Portuguese (Portugal)",
        "title": "PureLock: Bloqueio de Apps",
        "short_desc": "Bloqueio de apps offline e cofre AES-256. Sem anúncios, rastreio ou nuvem.",
        "full_desc": """PureLock é uma suite de privacidade sem anúncios e cofre encriptado offline para proteger aplicações, palavras-passe, códigos 2FA e notas privadas com SQLCipher AES-256 e Android Keystore.

🔒 FUNCIONALIDADES PRINCIPAIS:
• Bloqueio de Aplicações (App Shield) e Horários: Proteja aplicações com PIN, Padrão 3×3 (trilho invisível), Código Knock de 4 quadrantes ou Biometria. Defina horários por app, teclado aleatório e período de tolerância.
• Cofre e Credenciais Encriptados por Hardware: Guarde palavras-passe, notas, cartões e 2FA em SQLCipher AES-256, com credenciais cifradas em repouso no Android Keystore AES-256-GCM, reciclagem automática configurável e análise de entropia.
• Proteção Ativa e Disfarces: Bloqueio progressivo contra força bruta, proteção contra capturas de ecrã (FLAG_SECURE), Calculadora funcional, erro falso e PIN de coação (Duress).
• Agitar para Bloquear: Bloqueio imediato ao abanar o telemóvel com sensibilidade ajustável.
• Captura de Intrusos e Auditoria SHA-256: Regista tentativas falhadas localmente, verifica integridade SHA-256 e exporta CSV.
• Área de Transferência e Gerador Offline: Cria credenciais seguras e limpa automaticamente segredos copiados.
• Preenchimento Automático, Cópias .plk e TV: Autofill offline autenticado, cópias de segurança encriptadas AES-256-GCM (.plk), mosaicos rápidos, widget e suporte para Android TV.

🛡️ PRIVACIDADE 100% LOCAL:
Sem anúncios, sem telemetria nem rastreamento e sem contas na nuvem. O seu cofre encriptado, credenciais e registos nunca saem do dispositivo.

────────────────────────────────────────
USO E DIVULGAÇÃO DA API ACCESSIBILITYSERVICE
────────────────────────────────────────
O PureLock utiliza a API AccessibilityService estritamente para fornecer a função principal de Bloqueio de Aplicações (App Shield).

• Como é utilizado: O PureLock monitoriza eventos de mudança de janela (TYPE_WINDOW_STATE_CHANGED) apenas para detetar a abertura de uma aplicação protegida e apresentar de imediato o ecrã de bloqueio seguro (PIN, Padrão, Knock ou Biometria).
• Privacidade e Proteção: O PureLock NÃO lê, captura ou acede a conteúdo do ecrã, teclas premidas, palavras-passe ou mensagens pessoais (canRetrieveWindowContent desativado).
• Zero Recolha de Dados: O PureLock não recolhe, carrega nem transmite os seus dados do cofre, utilização de apps ou credenciais.

Esta permissão é opcional, solicitada apenas ao ativar o App Shield, podendo ser gerida a qualquer momento nas Definições de Acessibilidade."""
    },
    {
        "locale": "fr-FR",
        "language": "French (France)",
        "title": "PureLock: Verrouillage Apps",
        "short_desc": "Verrouillage d'apps hors ligne & coffre AES-256. Zéro pub, pistage ni cloud.",
        "full_desc": """PureLock est une suite de confidentialité sans publicité et un coffre-fort chiffré hors ligne conçu pour protéger vos applications, mots de passe, codes 2FA et notes privées via SQLCipher AES-256 et Android Keystore.

🔒 FONCTIONNALITÉS CLÉS :
• Bouclier d'Apps (App Shield) & Plages Horaires : Verrouillez toute application par PIN, Schéma 3×3 (tracé invisible), Code Knock à 4 quadrants ou Biométrie. Définissez des horaires par app, un clavier aléatoire et un délai de grâce.
• Coffre & Identifiants Chiffrés Matériellement : Stockez mots de passe, notes sécurisées, cartes et codes 2FA dans SQLCipher AES-256, avec identifiants maîtres chiffrés au repos via Android Keystore AES-256-GCM, corbeille auto-nettoyante configurable et analyse d'entropie.
• Défense Active & Leurres : Délais progressifs anti-force brute, blocage des captures d'écran et aperçus (FLAG_SECURE), Calculatrice fonctionnelle, faux plantage et PIN de contrainte (Duress).
• Secouer pour Verrouiller : Verrouillage instantané par secousse avec sensibilité réglable (Haute / Moyenne / Basse).
• Capture d'Intrus & Audit SHA-256 : Enregistre les échecs localement, vérifie l'intégrité cryptographique SHA-256 et exporte en CSV.
• Effacement Auto du Presse-papiers & Générateur : Crée des mots de passe robustes hors ligne et purge automatiquement les secrets copiés.
• Saisie Automatique, Sauvegardes .plk & TV : Service Autofill hors ligne authentifié, sauvegardes chiffrées AES-256-GCM (.plk), tuiles rapides, widget d'écran d'accueil et Android TV.

🛡️ CONFIDENTIALITÉ 100% LOCALE :
Zéro publicité, zéro télémétrie ni traqueur, zéro identifiant publicitaire et zéro compte cloud. Votre coffre chiffré, vos identifiants et vos journaux ne quittent jamais votre appareil.

────────────────────────────────────────
UTILISATION DE L'API ACCESSIBILITYSERVICE ET DIVULGATION
────────────────────────────────────────
PureLock utilise l'API AccessibilityService uniquement pour fournir sa fonctionnalité principale de verrouillage d'applications (App Shield).

• Utilisation : PureLock surveille les changements d'état des fenêtres (TYPE_WINDOW_STATE_CHANGED) uniquement pour détecter l'ouverture d'une application protégée au premier plan et afficher instantanément l'écran de verrouillage sécurisé (PIN, Schéma, Knock ou Biométrie).
• Confidentialité : PureLock NE lit, NE capture et N'accède à aucun contenu d'écran, frappe de clavier, mot de passe ou message privé (canRetrieveWindowContent désactivé).
• Zéro collecte de données : PureLock ne collecte, ne téléverse ni ne transmet vos données de coffre, votre usage d'applications ou vos identifiants.

Cette autorisation est facultative, demandée uniquement pour App Shield, et modifiable à tout moment dans les Paramètres d'Accessibilité."""
    },
    {
        "locale": "de-DE",
        "language": "German (Germany)",
        "title": "PureLock: App Sperre & Tresor",
        "short_desc": "Offline App-Sperre & AES-256 Tresor. Keine Werbung, kein Tracking, keine Cloud.",
        "full_desc": """PureLock ist eine werbefreie Datenschutz-Suite und ein verschlüsselter Offline-Tresor für Apps, Passwörter, 2FA-Codes und private Notizen — geschützt durch SQLCipher AES-256 und Android Keystore.

🔒 HAUPTFUNKTIONEN:
• App Shield (App-Sperre) & Zeitpläne: Sichern Sie beliebige Apps per PIN, 3×3-Muster (unsichtbarer Pfad), 4-Quadranten-Knock-Code oder Biometrie — mit individuellen App-Zeitplänen, Zufallstastatur und Kulanzzeit.
• Hardware-Verschlüsselter Tresor & Zugangsdaten: Speichern Sie Passwörter, Notizen, Karten und 2FA-Codes in SQLCipher AES-256; Master-Zugangsdaten werden per Android Keystore AES-256-GCM geschützt. Inkl. konfigurierbarem Papierkorb und Entropie-Analyse.
• Aktiver Manipulationsschutz & Tarnung: Progressive Brute-Force-Sperrfristen, Screenshot- und Vorschauschutz (FLAG_SECURE), funktionierender Taschenrechner, Absturz-Dialog und Notfall-PIN (Duress).
• Schütteln zum Sperren: Sofortige Notfallsperre per Schüttelgeste mit einstellbarer Empfindlichkeit.
• Eindringlings-Schutz & SHA-256-Audit: Protokolliert Fehlversuche lokal, prüft die kryptografische SHA-256-Integrität und bietet CSV-Export.
• Zwischenablage-Auto-Löschung & Generator: Erstellt starke Passwörter offline und löscht kopierte Geheimnisse automatisch.
• Authentifiziertes Autofill, .plk-Backups & TV: Lokaler Autofill-Dienst mit Master-Sperre, AES-256-GCM-verschlüsselte .plk-Backups, Schnelleinstellungen, Widget und Android TV.

🛡️ 100% LOKALER DATENSCHUTZ:
Keine Werbung, keine Telemetrie oder Tracker, keine Werbe-ID und kein Cloud-Konto. Ihr verschlüsselter Tresor, Zugangsdaten und Protokolle verlassen niemals Ihr Gerät.

────────────────────────────────────────
BARRIEREFREIHEITSDIENST (ACCESSIBILITY API) OFFENLEGUNG
────────────────────────────────────────
PureLock verwendet die AccessibilityService-API ausschließlich zur Bereitstellung der App-Sperrfunktion (App Shield).

• Verwendung: PureLock überwacht Fensterstatusänderungen (TYPE_WINDOW_STATE_CHANGED), um zu erkennen, wenn eine geschützte App im Vordergrund geöffnet wird, und zeigt sofort den sicheren Sperrbildschirm (PIN, Muster, Knock-Code oder Biometrie) an.
• Datenschutz: PureLock liest, erfasst oder speichert KEINE Bildschirminhalte, Tastatureingaben, Passwörter oder persönlichen Nachrichten (canRetrieveWindowContent deaktiviert).
• Keine Datenerfassung: PureLock sammelt, überträgt oder teilt keinerlei Tresordaten, App-Nutzung oder Zugangsdaten.

Die Berechtigung ist optional, wird nur für App Shield angefordert und kann jederzeit in den Bedienungshilfen-Einstellungen geändert werden."""
    },
    {
        "locale": "it-IT",
        "language": "Italian (Italy)",
        "title": "PureLock: Blocco App e Vault",
        "short_desc": "Blocco app offline e cassaforte AES-256. Zero pubblicità, tracciamento o cloud.",
        "full_desc": """PureLock è una suite di sicurezza senza pubblicità e una cassaforte crittografata offline per proteggere app, password, codici 2FA e note private con SQLCipher AES-256 e Android Keystore.

🔒 FUNZIONALITÀ PRINCIPALI:
• App Shield (Blocco App) e Orari: Proteggi qualsiasi app con PIN, Sequenza 3×3 (traccia invisibile), Codice Knock a 4 quadranti o Biometria. Imposta orari e giorni personalizzati per app, tastierino casuale e periodo di grazia.
• Cassaforte e Credenziali Crittografate Hardware: Salva password, note, carte e codici 2FA in SQLCipher AES-256, con credenziali master cifrate a riposo tramite Android Keystore AES-256-GCM, cestino auto-svuotante configurabile e analisi dell'entropia.
• Difesa Attiva e Camuffamento: Blocco progressivo anti-forza bruta, blocco screenshot e schermata Recenti (FLAG_SECURE), Calcolatrice funzionante, finto arresto anomalo e PIN di coercizione (Duress).
• Scuoti per Bloccare: Blocco immediato d'emergenza scuotendo il telefono con sensibilità regolabile.
• Cattura Intrusi e Audit SHA-256: Registra i tentativi falliti in locale, esegue verifiche crittografiche SHA-256 ed esporta in CSV.
• Pulizia Appunti e Generatore Offline: Genera password sicure e cancella automaticamente i segreti copiati dagli appunti.
• Compilazione Automatica, Backup .plk e TV: Autofill offline autenticato, backup e ripristino cifrati AES-256-GCM (.plk), riquadri rapidi, widget e supporto Android TV.

🛡️ PRIVACY 100% LOCALE:
Zero pubblicità, zero telemetria o tracciamento, nessun ID pubblicitario e nessun account cloud. Cassaforte, credenziali e registri non lasciano mai il tuo dispositivo.

────────────────────────────────────────
USO E DIVULGAZIONE DELL'API ACCESSIBILITYSERVICE
────────────────────────────────────────
PureLock utilizza l'API AccessibilityService esclusivamente per fornire la sua funzione principale di Blocco App (App Shield).

• Utilizzo: PureLock monitora gli eventi di cambio stato della finestra (TYPE_WINDOW_STATE_CHANGED) unicamente per rilevare quando un'app protetta viene aperta in primo piano e mostrare immediatamente la schermata di blocco sicura (PIN, Sequenza, Knock o Biometria).
• Privacy e Protezione Dati: PureLock NON legge, cattura o accede ai contenuti dello schermo, tasti digitati, password o messaggi personali (canRetrieveWindowContent disattivato).
• Zero Raccolta Dati: PureLock non raccoglie, carica né trasmette dati della cassaforte, utilizzo delle app o credenziali.

L'autorizzazione è facoltativa, richiesta solo attivando App Shield, e può essere attivata o disattivata in qualsiasi momento nelle Impostazioni di Accessibilità."""
    },
    {
        "locale": "ru-RU",
        "language": "Russian (Russia)",
        "title": "PureLock: Блокировка и Сейф",
        "short_desc": "Офлайн-блокировка и сейф AES-256. Без рекламы, телеметрии и облака.",
        "full_desc": """PureLock — автономный комплекс безопасности без рекламы и зашифрованный сейф для защиты приложений, паролей, кодов 2FA и заметок на базе SQLCipher AES-256 и Android Keystore.

🔒 ОСНОВНЫЕ ВОЗМОЖНОСТИ:
• Защита приложений (App Shield) и расписание: Блокировка любых приложений по PIN, графическому ключу 3×3 (невидимый след), 4-квадрантному Knock-коду или биометрии. Индивидуальные графики по времени и дням, случайная клавиатура и отсрочка блокировки.
• Аппаратный шифрованный сейф: Хранение паролей, заметок, карт и 2FA в SQLCipher AES-256 с шифрованием мастер-данных через Android Keystore AES-256-GCM, настраиваемой корзиной автоочистки и анализом энтропии.
• Активная защита и маскировка: Прогрессивная задержка от перебора, запрет скриншотов и превью (FLAG_SECURE), рабочий Калькулятор, ложный сбой и экстренный PIN принуждения (Duress).
• Блокировка встряхиванием: Мгновенная защита при встряхивании с настройкой чувствительности.
• Фиксация взломщиков и аудит SHA-256: Локальный журнал безопасности, проверка целостности SHA-256 и экспорт в CSV.
• Очистка буфера и генератор паролей: Генерация стойких паролей офлайн и автоматическая очистка буфера обмена.
• Автозаполнение, резервные копии .plk и TV: Офлайн-Autofill с мастер-аутентификацией, шифрованные копии AES-256-GCM (.plk), плитки быстрых настроек, виджет и Android TV.

🛡️ 100% ЛОКАЛЬНАЯ КОНФИДЕНЦИАЛЬНОСТЬ:
Без рекламы, телеметрии, трекеров и облачных аккаунтов. Ваш сейф, пароли и журналы никогда не покидают устройство.

────────────────────────────────────────
ИСПОЛЬЗОВАНИЕ API ACCESSIBILITYSERVICE
────────────────────────────────────────
PureLock использует AccessibilityService API исключительно для основной функции блокировки приложений (App Shield).

• Назначение: Отслеживаются только события смены окна (TYPE_WINDOW_STATE_CHANGED) для определения запуска защищенного приложения и мгновенного показа экрана блокировки (PIN, графический ключ, Knock-код или биометрия).
• Конфиденциальность: PureLock НЕ читает, НЕ собирает и НЕ перехватывает содержимое экрана, нажатия клавиш, пароли или личные сообщения (canRetrieveWindowContent отключён).
• Ноль сбора данных: PureLock не собирает и не передаёт данные сейфа, статистику приложений или учётные данные.

Разрешение необязательно, запрашивается только для App Shield и может быть отключено в любой момент в настройках Специальных возможностей."""
    },
    {
        "locale": "ja-JP",
        "language": "Japanese (Japan)",
        "title": "PureLock: アプリロック＆保管庫",
        "short_desc": "完全オフラインのアプリロック＆AES-256保管庫。広告・追跡・クラウドなし。",
        "full_desc": """PureLockは、SQLCipher AES-256とAndroid Keystoreのハードウェア暗号化でアプリ・パスワード・2FAコード・秘密メモを端末内で保護する、広告なし・テレメトリなしのオフラインセキュリティ＆暗号化保管庫です。

🔒 主な機能:
• アプリロック（App Shield）＆カスタムスケジュール: PIN、3×3パターン（軌跡非表示）、4分割ノックコード、生体認証で保護。アプリごとの時間帯・曜日スケジュール、ランダムキーパッド、再ロック猶予時間に対応。
• ハードウェア暗号化保管庫＆認証情報: パスワード、メモ、カード、2FAコードをSQLCipher AES-256で保存し、マスター認証情報もAndroid Keystore AES-256-GCMで保護。ゴミ箱自動削除設定とエントロピー強度分析を搭載。
• 不正対策＆カモフラージュ: 段階的ブルートフォース待機ロック、画面キャプチャ・履歴プレビュー防止（FLAG_SECURE）、電卓ロック、偽クラッシュ画面、緊急用デュレスPINに対応。
• シェイクでロック: 感度調整可能な加速度センサーによる緊急ロックダウン。
• 侵入者記録＆SHA-256監査: 解除失敗のローカル記録、SHA-256整合性チェック、CSVエクスポート。
• クリップボード自動消去＆パスワード生成: 高強度パスワードのローカル生成とコピー後の自動消去。
• 認証付きローカル自動入力・暗号化バックアップ・TV対応: マスター認証付きオフライン自動入力、AES-256-GCM暗号化 .plk バックアップ＆復元、クイック設定タイル、ウィジェット、Android TV対応。

🛡️ 100%ローカルプライバシー保証:
広告、分析・テレメトリSDK、広告ID、クラウドアカウントは一切ありません。暗号化データや記録は端末外へ一切送信されません。

────────────────────────────────────────
ユーザー補助（ACCESSIBILITY SERVICE）APIの利用開示
────────────────────────────────────────
PureLockは、コア機能である「アプリロック（App Shield）」を提供するためにのみAccessibilityService APIを使用します。

・使用目的: 保護対象アプリがフォアグラウンドで起動されたことを検知（TYPE_WINDOW_STATE_CHANGED）し、直ちに安全なロック画面（PIN・パターン・ノックコード・生体認証）を表示します。
・プライバシー保護: 画面上のテキスト、キー入力、パスワード、個人メッセージの閲覧・取得は一切行いません（canRetrieveWindowContentは無効）。
・データ収集ゼロ: 保管庫データ、アプリ利用履歴、認証情報を収集・アップロード・送信することはありません。

ユーザー補助権限は任意で、App Shield使用時のみリクエストされ、端末のユーザー補助設定からいつでも変更できます。"""
    },
    {
        "locale": "ko-KR",
        "language": "Korean (South Korea)",
        "title": "PureLock: 앱 잠금 및 금고",
        "short_desc": "오프라인 앱 잠금 및 AES-256 금고. 광고·추적·클라우드 없는 완벽한 프라이버시.",
        "full_desc": """PureLock은 SQLCipher AES-256 및 Android Keystore 하드웨어 암호화로 앱, 비밀번호, 2FA 코드, 비공개 메모를 기기 내부에서만 안전하게 보호하는 무광고·무추적 오프라인 보안 앱입니다.

🔒 주요 기능:
• 앱 잠금(App Shield) 및 맞춤 일정: PIN, 3×3 패턴(경로 숨김), 4분할 노크 코드, 생체 인증으로 모든 앱 보호. 앱별 요일·시간대 잠금 일정, 랜덤 키패드, 재잠금 유예 시간 지원.
• 하드웨어 암호화 금고 및 인증 정보: 비밀번호, 보안 메모, 카드, 2FA 코드를 SQLCipher AES-256에 저장하며 마스터 인증 정보도 Android Keystore AES-256-GCM으로 암호화. 휴지통 자동 삭제 주기 설정 및 엔트로피 분석 제공.
• 능동 방어 및 위장 화면: 무차별 대입 방지 단계별 대기 잠금, 화면 캡처 및 최근 앱 미리보기 차단(FLAG_SECURE), 계산기 위장, 가짜 오류 화면, 비상 Duress PIN 지원.
• 흔들어서 잠금: 민감도 조절이 가능한 즉시 비상 잠금 제스처.
• 침입자 기록 및 SHA-256 감사: 실패 시도 로컬 기록, SHA-256 무결성 검사, CSV 내보내기.
• 클립보드 자동 삭제 및 비밀번호 생성기: 오프라인 고강도 비밀번호 생성과 복사 후 자동 삭제.
• 인증형 로컬 자동 완성, .plk 백업 및 Android TV: 마스터 잠금 기반 오프라인 자동 완성, AES-256-GCM 암호화 .plk 백업/복원, 빠른 설정 타일, 홈 위젯, Android TV 지원.

🛡️ 100% 로컬 개인정보 보호:
광고, 분석·텔레메트리 SDK, 광고 ID, 클라우드 계정 없이 모든 암호화 데이터와 기록은 기기 내부에만 보관됩니다.

────────────────────────────────────────
접근성(ACCESSIBILITY SERVICE) API 사용 및 고지
────────────────────────────────────────
PureLock은 핵심 기능인 '앱 잠금(App Shield)' 제공을 위해서만 AccessibilityService API를 사용합니다.

• 사용 목적: 보호 앱의 포그라운드 실행을 감지(TYPE_WINDOW_STATE_CHANGED)하여 즉시 보안 잠금 화면(PIN, 패턴, 노크 코드, 생체 인증)을 표시합니다.
• 개인정보 보호: 화면 내용, 키 입력, 비밀번호 또는 개인 메시지를 절대 읽거나 수집·접근하지 않습니다(canRetrieveWindowContent 비활성화).
• 데이터 수집 제로: 금고 데이터, 앱 사용 기록, 인증 정보를 수집·업로드·전송하지 않습니다.

접근성 권한은 선택 사항이며 App Shield 사용 시에만 요청되고 기기의 접근성 설정에서 언제든 변경할 수 있습니다."""
    },
    {
        "locale": "zh-CN",
        "language": "Chinese (Simplified)",
        "title": "PureLock: 应用锁与隐私保险箱",
        "short_desc": "离线应用锁与 AES-256 加密保险箱。无广告、无遥测追踪、无云端。",
        "full_desc": """PureLock 是一款无广告、无遥测追踪的离线隐私应用锁与加密保险箱，依托 SQLCipher AES-256 与 Android Keystore 硬件密钥，在设备本地保护您的应用、密码、2FA 验证码与私密笔记。

🔒 核心功能：
• 应用锁（App Shield）与定时规则：支持 PIN 码、九宫格图案（隐藏轨迹）、四象限敲击密码与生物识别，支持按应用自定义时段与星期锁定、随机键盘和重新锁定宽限期。
• 硬件加密保险箱与主凭据保护：使用 SQLCipher AES-256 存储密码、笔记、卡片与 2FA，主凭据静态经 Android Keystore AES-256-GCM 加密，内置可自定义回收站自动清理与密码熵值分析。
• 主动防篡改与伪装模式：连续输错渐进冷却锁定、全局防截屏与后台模糊保护（FLAG_SECURE）、可运算计算器伪装、模拟崩溃弹窗及紧急胁迫 PIN 码。
• 摇一摇锁屏：灵敏度可调的重力滤波加速度计一键应急锁定。
• 入侵记录与 SHA-256 完整性审计：本地记录失败尝试、执行 SHA-256 加密校验并支持 CSV 导出。
• 剪贴板自动清除与密码生成器：离线生成高强度密码，复制后自动清空剪贴板。
• 认证本地自动填充、加密备份与 TV 支持：受主锁保护的离线自动填充服务、AES-256-GCM 加密 .plk 备份与恢复、快捷磁贴、桌面小组件及 Android TV 支持。

🛡️ 100% 本地隐私保障：
零广告、零分析与遥测 SDK、无广告 ID、无云端账号。您的加密保险箱、凭据、定时规则和日志始终仅存于本机。

────────────────────────────────────────
无障碍服务（ACCESSIBILITY SERVICE）API 使用声明
────────────────────────────────────────
PureLock 仅使用 AccessibilityService API 提供核心“应用锁（App Shield）”功能。

• 使用方式：仅实时监听窗口状态变更事件（TYPE_WINDOW_STATE_CHANGED），用于检测受保护应用进入前台并立即显示安全锁屏（PIN、图案、敲击密码或生物识别）。
• 隐私保护：绝不读取、捕获或访问屏幕内容、按键输入、密码或个人消息（已禁用 canRetrieveWindowContent）。
• 零数据收集：不收集、上传或传输您的保险箱数据、应用使用记录或凭据。

无障碍权限为可选项，仅在启用 App Shield 时申请，您可随时在系统“无障碍设置”中开启或关闭。"""
    },
    {
        "locale": "zh-TW",
        "language": "Chinese (Traditional)",
        "title": "PureLock: 應用程式鎖與保險箱",
        "short_desc": "離線應用程式鎖與 AES-256 加密保險箱。無廣告、無遙測追蹤、無雲端。",
        "full_desc": """PureLock 是一款無廣告、無遙測追蹤的離線隱私應用程式鎖與加密保險箱，採用 SQLCipher AES-256 與 Android Keystore 硬體金鑰，在裝置本機保護您的應用程式、密碼、2FA 驗證碼與私密筆記。

🔒 核心功能：
• 應用程式鎖（App Shield）與排程規則：支援 PIN 碼、3×3 圖形（隱藏軌跡）、四象限敲擊密碼與生物辨識，支援各應用程式自訂時段與星期排程、隨機鍵盤與寬限時間。
• 硬體加密保險箱與主憑證保護：使用 SQLCipher AES-256 儲存密碼、筆記、卡片與 2FA，主憑證以 Android Keystore AES-256-GCM 靜態加密，內建可調式垃圾桶自動清除與熵值強度分析。
• 主動防護與偽裝模式：連續錯誤漸進式冷卻鎖定、全應用防截圖與最近使用保護（FLAG_SECURE）、可運作計算機偽裝、假當機視窗及緊急脅迫 PIN 碼。
• 搖一搖鎖定：靈敏度可調的緊急快速鎖定手勢。
• 入侵紀錄與 SHA-256 稽核：本機記錄解鎖失敗、執行 SHA-256 完整性驗證並支援 CSV 匯出。
• 剪貼簿自動清除與密碼產生器：離線產生高強度密碼，複製後自動清空剪貼簿。
• 認證本機自動填入、加密備份與 TV 支援：受主鎖保護的離線自動填入、AES-256-GCM 加密 .plk 備份與還原、快速設定方塊、桌面小工具與 Android TV 支援。

🛡️ 100% 本機隱私保障：
零廣告、零分析與遙測 SDK、無廣告 ID、無雲端帳號。您的加密保險箱、憑證、排程與紀錄絕不離開裝置。

────────────────────────────────────────
無障礙服務（ACCESSIBILITY SERVICE）API 使用聲明
────────────────────────────────────────
PureLock 僅使用 AccessibilityService API 提供核心「應用程式鎖（App Shield）」功能。

• 使用方式：僅即時監聽視窗狀態變更事件（TYPE_WINDOW_STATE_CHANGED），於受保護應用程式開啟時立即顯示安全鎖定畫面（PIN、圖形、敲擊密碼或生物辨識）。
• 隱私保護：絕不讀取、擷取或存取螢幕內容、按鍵輸入、密碼或私人訊息（已停用 canRetrieveWindowContent）。
• 零資料收集：不會收集、上傳或傳送您的保險箱資料、應用程式使用紀錄或憑證。

無障礙權限為選擇性項目，僅於啟用 App Shield 時請求，您可隨時於裝置的無障礙設定中變更。"""
    },
    {
        "locale": "vi",
        "language": "Vietnamese",
        "title": "PureLock: Khóa ứng dụng",
        "short_desc": "Khóa ứng dụng offline & két AES-256. Không quảng cáo, theo dõi hay đám mây.",
        "full_desc": """PureLock là bộ công cụ bảo mật không quảng cáo, không theo dõi và két mã hóa offline giúp bảo vệ ứng dụng, mật khẩu, mã 2FA và ghi chú riêng tư ngay trên thiết bị với SQLCipher AES-256 và Android Keystore.

🔒 TÍNH NĂNG NỔI BẬT:
• Lá Chắn Ứng Dụng (App Shield) & Lịch Khóa: Khóa bất kỳ ứng dụng nào bằng PIN, Hình vẽ 3×3 (ẩn đường vẽ), Mã gõ 4 vùng (Knock Code) hoặc Sinh trắc học. Hỗ trợ lịch khóa theo khung giờ/ngày cho từng ứng dụng, bàn phím xáo trộn và thời gian ân hạn.
• Két & Thông Tin Xác Thực Mã Hóa Phần Cứng: Lưu mật khẩu, ghi chú, thẻ và mã 2FA trong SQLCipher AES-256; mã khóa chính được mã hóa bằng Android Keystore AES-256-GCM. Có Thùng rác tự dọn tùy chỉnh và Phân tích độ mạnh mật khẩu.
• Phòng Vệ Chủ Động & Ngụy Trang: Khóa chờ tăng dần chống dò mật khẩu, chống chụp màn hình & ẩn xem trước (FLAG_SECURE), Máy tính hoạt động thật, Lỗi ứng dụng giả và PIN khẩn cấp (Duress).
• Lắc Để Khóa: Khóa khẩn cấp tức thì khi lắc điện thoại với 3 mức độ nhạy tùy chỉnh.
• Ghi Nhận Kẻ Xâm Nhập & Kiểm Toán SHA-256: Lưu lịch sử mở khóa sai cục bộ, kiểm tra toàn vẹn mật mã SHA-256 và xuất nhật ký CSV.
• Tự Xóa Bộ Nhớ Tạm & Tạo Mật Khẩu Offline: Tạo mật khẩu mạnh và tự động xóa bí mật đã sao chép khỏi clipboard.
• Tự Điền Xác Thực, Sao Lưu .plk & Android TV: Dịch vụ Autofill cục bộ có xác thực khóa chính, sao lưu/khôi phục mã hóa AES-256-GCM (.plk), Ô Cài Đặt Nhanh, Widget và Android TV.

🛡️ CAM KẾT RIÊNG TƯ 100% CỤC BỘ:
Không quảng cáo, không SDK phân tích hay theo dõi, không ID quảng cáo và không tài khoản đám mây. Két mã hóa, thông tin xác thực và nhật ký luôn nằm trọn trên thiết bị của bạn.

────────────────────────────────────────
CÔNG KHAI SỬ DỤNG ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock sử dụng AccessibilityService API nghiêm ngặt chỉ nhằm phục vụ tính năng Khóa Ứng Dụng (App Shield).

• Mục đích sử dụng: PureLock chỉ theo dõi sự kiện thay đổi cửa sổ (TYPE_WINDOW_STATE_CHANGED) để nhận biết khi ứng dụng được bảo vệ mở lên và hiển thị ngay màn hình khóa an toàn (PIN, Hình vẽ, Knock Code hoặc Sinh trắc học).
• Bảo vệ quyền riêng tư: PureLock KHÔNG đọc, chụp hoặc truy cập nội dung màn hình, thao tác bàn phím, mật khẩu hoặc tin nhắn cá nhân (canRetrieveWindowContent đã tắt).
• Không thu thập dữ liệu: PureLock không thu thập, tải lên hoặc truyền dữ liệu két, lịch sử dùng ứng dụng hay thông tin đăng nhập của bạn.

Quyền Trợ năng là tùy chọn, chỉ yêu cầu khi bật App Shield và có thể bật/tắt bất cứ lúc nào trong Cài đặt Trợ năng."""
    },
    {
        "locale": "id",
        "language": "Indonesian",
        "title": "PureLock: Kunci Aplikasi",
        "short_desc": "Kunci aplikasi offline & brankas AES-256. Tanpa iklan, pelacakan, atau cloud.",
        "full_desc": """PureLock adalah aplikasi keamanan bebas iklan, bebas pelacakan, dan brankas terenkripsi offline untuk melindungi aplikasi, kata sandi, kode 2FA, dan catatan pribadi dengan SQLCipher AES-256 dan Android Keystore.

🔒 FITUR UTAMA:
• App Shield (Kunci Aplikasi) & Jadwal Khusus: Lindungi aplikasi dengan PIN, Pola 3×3 (jejak tak terlihat), Kode Ketuk 4-Kuadran, atau Biometrik. Atur jadwal waktu & hari per aplikasi, keypad acak, dan masa tenggang.
• Brankas & Kredensial Enkripsi Perangkat Keras: Simpan kata sandi, catatan, kartu, dan 2FA di SQLCipher AES-256 dengan kredensial utama terenkripsi Android Keystore AES-256-GCM, tempat sampah otomatis yang dapat diatur, dan wawasan entropi.
• Pertahanan Aktif & Penyamaran: Jeda kunci progresif anti-brute-force, blokir tangkapan layar & pratinjau (FLAG_SECURE), Kalkulator aktif, dialog Crash Palsu, dan PIN Darurat (Duress).
• Goyangkan untuk Mengunci: Kunci darurat seketika dengan sensitivitas akselerometer yang dapat diatur.
• Catatan Penyusup & Audit SHA-256: Merekam percobaan gagal secara lokal, memverifikasi integritas kriptografi SHA-256, dan ekspor CSV.
• Pembersih Clipboard & Generator Offline: Buat kata sandi kuat dan hapus otomatis rahasia yang disalin.
• Autofill Terautentikasi, Cadangan .plk & TV: Autofill lokal terlindungi kunci utama, cadangan & pemulihan terenkripsi AES-256-GCM (.plk), Ubin Cepat, Widget, dan dukungan Android TV.

🛡️ JAMINAN PRIVASI 100% LOKAL:
Tanpa iklan, tanpa SDK analitik atau telemetri, tanpa ID iklan, dan tanpa akun cloud. Brankas terenkripsi, kredensial, dan log tidak pernah meninggalkan perangkat Anda.

────────────────────────────────────────
PENGGUNAAN & PENGUNGKAPAN ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock menggunakan AccessibilityService API semata-mata untuk fitur utama Pengunci Aplikasi (App Shield).

• Cara Penggunaan: Memantau perubahan status jendela (TYPE_WINDOW_STATE_CHANGED) untuk mendeteksi pembukaan aplikasi terlindungi dan segera menampilkan layar kunci aman (PIN, Pola, Knock Code, atau Biometrik).
• Privasi Data: PureLock TIDAK membaca, merekam, atau mengakses konten layar, ketikan, kata sandi, atau pesan pribadi (canRetrieveWindowContent dinonaktifkan).
• Nol Pengumpulan Data: PureLock tidak mengumpulkan, mengunggah, atau mengirimkan data brankas, penggunaan aplikasi, maupun kredensial Anda.

Izin Aksesibilitas bersifat opsional, hanya diminta saat mengaktifkan App Shield, dan dapat diubah kapan saja di Pengaturan Aksesibilitas."""
    },
    {
        "locale": "hi-IN",
        "language": "Hindi",
        "title": "PureLock: ऐप लॉक और वॉल्ट",
        "short_desc": "ऑफ़लाइन ऐप लॉक व AES-256 वॉल्ट। बिना विज्ञापन, ट्रैकिंग या क्लाउड के।",
        "full_desc": """PureLock एक विज्ञापन-मुक्त, ट्रैकिंग-रहित ऑफ़लाइन सुरक्षा ऐप और एन्क्रिप्टेड वॉल्ट है जो SQLCipher AES-256 और Android Keystore से आपके ऐप्स, पासवर्ड, 2FA कोड और निजी नोट्स की डिवाइस पर ही रक्षा करता है।

🔒 मुख्य विशेषताएँ:
• App Shield (ऐप लॉक) व कस्टम शेड्यूल: PIN, 3×3 पैटर्न (अदृश्य रेखा), 4-क्वाड्रंट नॉक कोड या बायोमेट्रिक से ऐप लॉक करें। प्रति-ऐप समय व दिन का शेड्यूल, रैंडम कीपैड और री-लॉक ग्रेस पीरियड शामिल हैं।
• हार्डवेयर-एन्क्रिप्टेड वॉल्ट व क्रेडेंशियल्स: SQLCipher AES-256 में पासवर्ड, नोट्स, कार्ड और 2FA सुरक्षित रखें; मास्टर क्रेडेंशियल Android Keystore AES-256-GCM से एन्क्रिप्ट रहते हैं। कॉन्फ़िगर करने योग्य ट्रैश ऑटो-पर्ज व एन्ट्रॉपी विश्लेषण सहित।
• सक्रिय सुरक्षा व डिकॉय: ब्रूट-फ़ोर्स से बचाव हेतु प्रोग्रेसिव लॉकआउट कूलडाउन, स्क्रीनशॉट व रीसेंट्स ब्लॉक (FLAG_SECURE), कैलकुलेटर लॉक, नकली क्रैश डायलॉग और ड्यूरेस PIN।
• हिलाकर लॉक करें: एडजस्टेबल सेंसर संवेदनशीलता के साथ तुरंत लॉकडाउन।
• घुसपैठिया रिकॉर्ड व SHA-256 ऑडिट: असफल प्रयासों का स्थानीय लॉग, SHA-256 अखंडता जाँच और CSV एक्सपोर्ट।
• क्लिपबोर्ड ऑटो-वाइप व पासवर्ड जनरेटर: मजबूत पासवर्ड बनाएं और कॉपी किए गए सीक्रेट स्वतः साफ करें।
• प्रमाणीकृत लोकल ऑटोफ़िल, .plk बैकअप व Android TV: मास्टर-लॉक आधारित ऑफ़लाइन ऑटोफ़िल, AES-256-GCM एन्क्रिप्टेड .plk बैकअप/रीस्टोर, क्विक टाइल्स, विजेट और Android TV समर्थन।

🛡️ 100% स्थानीय गोपनीयता गारंटी:
कोई विज्ञापन नहीं, कोई एनालिटिक्स/टेलीमेट्री SDK नहीं, कोई विज्ञापन ID नहीं और कोई क्लाउड खाता नहीं। आपका एन्क्रिप्टेड डेटा केवल आपके डिवाइस पर रहता है।

────────────────────────────────────────
ACCESSIBILITY SERVICE API उपयोग और प्रकटीकरण
────────────────────────────────────────
PureLock केवल अपने मुख्य ऐप लॉक (App Shield) फीचर के लिए AccessibilityService API का उपयोग करता है।

• उद्देश्य: सुरक्षित ऐप खुलने का पता लगाने हेतु केवल विंडो परिवर्तन (TYPE_WINDOW_STATE_CHANGED) की निगरानी करता है और तुरंत सुरक्षित लॉक स्क्रीन (PIN, पैटर्न, नॉक कोड या बायोमेट्रिक) दिखाता है।
• गोपनीयता: PureLock स्क्रीन सामग्री, कीस्ट्रोक्स, पासवर्ड या निजी संदेशों को कभी नहीं पढ़ता या एक्सेस नहीं करता (canRetrieveWindowContent अक्षम है)।
• शून्य डेटा संग्रह: PureLock आपके वॉल्ट डेटा, ऐप उपयोग या क्रेडेंशियल्स को एकत्र, अपलोड या प्रसारित नहीं करता।

यह अनुमति वैकल्पिक है, केवल App Shield के लिए माँगी जाती है और डिवाइस की Accessibility Settings से कभी भी बदली जा सकती है।"""
    },
    {
        "locale": "tr-TR",
        "language": "Turkish (Turkey)",
        "title": "PureLock: Uygulama Kilidi",
        "short_desc": "Çevrimdışı uygulama kilidi ve AES-256 kasa. Reklamsız, takipsiz, bulutsuz.",
        "full_desc": """PureLock, uygulamalarınızı, şifrelerinizi, 2FA kodlarınızı ve özel notlarınızı SQLCipher AES-256 ve Android Keystore ile cihazınızda koruyan reklamsız, telemetrisiz çevrimdışı güvenlik kasasıdır.

🔒 TEMEL ÖZELLİKLER:
• App Shield (Uygulama Kilidi) ve Zamanlama: PIN, 3×3 Desen (görünmez iz), 4 Bölgeli Knock Kodu veya Biyometri ile koruma; uygulama bazlı saat/gün programları, rastgele tuş takımı ve yeniden kilitleme gecikmesi.
• Donanım Şifreli Kasa ve Kimlik Bilgileri: Parola, not, kart ve 2FA kodlarını SQLCipher AES-256 ile saklayın; ana kilit bilgileri Android Keystore AES-256-GCM ile şifrelenir. Özelleştirilebilir çöp kutusu ve entropi analizi içerir.
• Aktif Koruma ve Kamuflaj: Kaba kuvvet saldırılarına karşı kademeli bekleme kilidi, ekran görüntüsü engelleme (FLAG_SECURE), çalışan Hesap Makinesi, Sahte Çökme ve acil durum Baskı PIN'i (Duress).
• Kilitlemek İçin Salla: Ayarlanabilir hassasiyetle anında acil kilitleme.
• İzinsiz Giriş Kaydı ve SHA-256 Denetimi: Başarısız denemeleri yerel kaydeder, SHA-256 bütünlük doğrulaması ve CSV dışa aktarım sunar.
• Pano Otomatik Temizleme ve Şifre Üretici: Çevrimdışı güçlü parola oluşturur ve kopyalanan sırları panodan otomatik siler.
• Kimlik Doğrulamalı Autofill, .plk Yedek ve TV: Ana kilit korumalı çevrimdışı Otomatik Doldurma, AES-256-GCM şifreli .plk yedekleme/geri yükleme, Hızlı Ayarlar, Widget ve Android TV desteği.

🛡️ %100 YEREL GİZLİLİK:
Reklam yok, analiz/telemetri SDK'sı yok, reklam kimliği yok ve bulut hesabı yok. Şifreli verileriniz ve günlükleriniz asla cihazınızdan ayrılmaz.

────────────────────────────────────────
ACCESSIBILITY SERVICE API KULLANIMI VE AÇIKLAMASI
────────────────────────────────────────
PureLock, AccessibilityService API'sini yalnızca temel Uygulama Kilidi (App Shield) özelliği için kullanır.

• Kullanım: Korunan bir uygulamanın açıldığını algılamak ve güvenli kilit ekranını (PIN, Desen, Knock Kodu veya Biyometri) göstermek için yalnızca pencere durumu değişikliklerini (TYPE_WINDOW_STATE_CHANGED) izler.
• Gizlilik: Ekran içeriğini, tuş vuruşlarını, parolaları veya kişisel mesajları ASLA okumaz, kaydetmez veya erişmez (canRetrieveWindowContent devre dışıdır).
• Sıfır Veri Toplama: PureLock kasa verilerinizi, uygulama kullanımınızı veya kimlik bilgilerinizi toplamaz, yüklemez veya iletmez.

Bu izin isteğe bağlıdır, yalnızca App Shield için istenir ve cihazınızın Erişilebilirlik Ayarları'ndan dilediğiniz zaman kapatılabilir."""
    },
    {
        "locale": "th",
        "language": "Thai",
        "title": "PureLock: ล็อกแอปและตู้เซฟ",
        "short_desc": "ล็อกแอปออฟไลน์ & ตู้เซฟ AES-256 ไร้โฆษณา ไร้การติดตาม ไร้คลาวด์",
        "full_desc": """PureLock คือชุดความปลอดภัยไร้โฆษณา ไร้การติดตาม และตู้เซฟเข้ารหัสออฟไลน์ ปกป้องแอป รหัสผ่าน รหัส 2FA และโน้ตส่วนตัวด้วย SQLCipher AES-256 และ Android Keystore บนเครื่องของคุณ

🔒 ฟีเจอร์หลัก:
• ล็อกแอป (App Shield) และตารางเวลา: ป้องกันแอปด้วย PIN, รูปแบบ 3×3 (ซ่อนเส้น), รหัสเคาะ 4 ช่อง (Knock Code) หรือไบโอเมตริกซ์ ตั้งตารางเวลาและวันแยกตามแอป พร้อมแป้นตัวเลขสุ่มและเวลาหน่วงล็อกซ้ำ
• ตู้เซฟและรหัสหลักเข้ารหัสฮาร์ดแวร์: เก็บรหัสผ่าน โน้ต บัตร และ 2FA ใน SQLCipher AES-256 พร้อมเข้ารหัสรหัสหลักด้วย Android Keystore AES-256-GCM ถังขยะล้างอัตโนมัติที่ปรับวันได้ และวิเคราะห์ความแข็งแกร่งรหัสผ่าน
• ป้องกันเชิงรุกและโหมดพรางตัว: หน่วงเวลาล็อกแบบขั้นบันไดเมื่อใส่รหัสผิดซ้ำ ป้องกันแคปหน้าจอและพรีวิวแอป (FLAG_SECURE) หน้าจอเครื่องคิดเลข หน้าต่างแอปหยุดทำงานปลอม และ PIN ฉุกเฉิน (Duress)
• เขย่าเพื่อล็อก: ล็อกฉุกเฉินทันทีเมื่อเขย่าเครื่อง ปรับความไวได้ 3 ระดับ
• บันทึกผู้บุกรุกและตรวจสอบ SHA-256: บันทึกการปลดล็อกผิดในเครื่อง ตรวจสอบความสมบูรณ์ SHA-256 และส่งออก CSV
• ล้างคลิปบอร์ดอัตโนมัติและสร้างรหัสผ่านออฟไลน์: สร้างรหัสผ่านปลอดภัยและลบข้อมูลที่คัดลอกอัตโนมัติ
• ป้อนอัตโนมัติแบบยืนยันตัวตน สำรองข้อมูล .plk และ TV: Autofill ออฟไลน์ที่ต้องปลดล็อกหลัก สำรองและกู้คืนไฟล์ .plk เข้ารหัส AES-256-GCM ปุ่มตั้งค่าด่วน วิดเจ็ต และ Android TV

🛡️ ความเป็นส่วนตัวในเครื่อง 100%:
ไม่มีโฆษณา ไม่มี SDK วิเคราะห์หรือติดตาม ไม่มีรหัสโฆษณา และไม่มีบัญชีคลาวด์ ข้อมูลเข้ารหัสและบันทึกทั้งหมดอยู่บนอุปกรณ์ของคุณเท่านั้น

────────────────────────────────────────
การเปิดเผยการใช้งาน ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock ใช้ AccessibilityService API เพื่อฟีเจอร์ล็อกแอป (App Shield) เท่านั้น

• วัตถุประสงค์: ตรวจจับการเปลี่ยนหน้าต่าง (TYPE_WINDOW_STATE_CHANGED) เพื่อรู้เมื่อแอปที่ปกป้องเปิดขึ้นและแสดงหน้าจอล็อกทันที (PIN, รูปแบบ, Knock Code หรือไบโอเมตริกซ์)
• ความเป็นส่วนตัว: ไม่อ่าน บันทึก หรือเข้าถึงเนื้อหาหน้าจอ การพิมพ์ รหัสผ่าน หรือข้อความส่วนตัวใดๆ (ปิดใช้งาน canRetrieveWindowContent)
• ไม่เก็บข้อมูลส่วนบุคคล: PureLock ไม่เก็บรวบรวม อัปโหลด หรือส่งข้อมูลตู้เซฟ การใช้งานแอป หรือรหัสผ่านของคุณ

สิทธิ์การเข้าถึงนี้เป็นทางเลือก ขอเฉพาะเมื่อเปิดใช้ App Shield และสามารถเปิด/ปิดได้ตลอดเวลาในการตั้งค่าการเข้าถึงของอุปกรณ์"""
    },
    {
        "locale": "ar",
        "language": "Arabic",
        "title": "PureLock: قفل التطبيقات وخزنة",
        "short_desc": "قفل تطبيقات دون إنترنت وخزنة AES-256. بلا إعلانات أو تتبع أو سحابة.",
        "full_desc": """PureLock هو تطبيق أمان خالٍ من الإعلانات والتتبع وخزنة مشفرة تعمل دون إنترنت لحماية تطبيقاتك وكلمات مرورك ورموز 2FA وملاحظاتك الخاصة باستخدام SQLCipher AES-256 و Android Keystore.

🔒 الميزات الرئيسية:
• درع التطبيقات (App Shield) والجدولة: اقفل أي تطبيق برمز PIN أو نمط 3×3 (بمسار مخفي) أو رمز النقر الرباعي (Knock Code) أو البصمة، مع جداول أوقات وأيام مخصصة لكل تطبيق ولوحة أرقام عشوائية وفترة سماح.
• خزنة وبيانات اعتماد مشفرة بالأجهزة: احفظ كلمات المرور والملاحظات والبطاقات ورموز 2FA في SQLCipher AES-256 مع تشفير مفاتيح القفل بـ Android Keystore AES-256-GCM وسلة محذوفات قابلة للتخصيص وتحليل قوة كلمات المرور.
• دفاع نشط وتمويه: تهدئة تدريجية ضد التخمين العشوائي، منع لقطات الشاشة ومعاينة التطبيقات الأخيرة (FLAG_SECURE)، آلة حاسبة حقيقية، تعطل وهمي ورمز PIN للإكراه (Duress).
• الهز للقفل: قفل فوري للطوارئ عند هز الجهاز بحساسية قابلة للتعديل.
• سجل الدخلاء وتدقيق SHA-256: تسجيل المحاولات الفاشلة محلياً مع فحص سلامة الخزنة SHA-256 وتصدير CSV.
• مسح الحافظة التلقائي ومولد كلمات المرور: إنشاء كلمات مرور قوية محلياً ومسح الأسرار المنسوخة تلقائياً.
• ملء تلقائي محمي، نسخ .plk و Android TV: خدمة Autofill محلية تتطلب المصادقة، نسخ احتياطي واستعادة مشفر بـ AES-256-GCM (.plk)، مربعات الإعدادات السريعة، ويدجت ودعم Android TV.

🛡️ خصوصية محلية 100%:
بلا إعلانات، بلا أدوات تحليل أو تتبع، بلا معرّف إعلاني وبلا حسابات سحابية. تبقى خزنتك المشفرة وسجلاتك حصراً داخل جهازك.

────────────────────────────────────────
إفصاح استخدام واجهة ACCESSIBILITY SERVICE API
────────────────────────────────────────
يستخدم PureLock واجهة AccessibilityService API حصرياً لتوفير ميزة قفل التطبيقات الرئيسية (App Shield).

• آلية الاستخدام: يراقب PureLock أحداث تغيير حالة النوافذ (TYPE_WINDOW_STATE_CHANGED) فقط لاكتشاف فتح تطبيق محمي وعرض شاشة القفل الآمنة فوراً (PIN أو النمط أو Knock Code أو البصمة).
• حماية الخصوصية: لا يقرأ PureLock أو يسجل أو يصل إلى محتوى الشاشة أو ضغطات المفاتيح أو كلمات المرور أو الرسائل الشخصية (تم تعطيل canRetrieveWindowContent).
• عدم جمع البيانات: لا يجمع PureLock أو يرفع أو ينقل بيانات خزنتك أو استخدام التطبيقات أو بيانات اعتمادك.

هذا الإذن اختياري ولا يُطلب إلا عند تفعيل App Shield، ويمكن تفعيله أو إيقافه في أي وقت من إعدادات إمكانية الوصول."""
    },
    {
        "locale": "nl-NL",
        "language": "Dutch (Netherlands)",
        "title": "PureLock: App-vergrendeling",
        "short_desc": "Offline app-slot & AES-256 kluis. Geen reclame, geen tracking, geen cloud.",
        "full_desc": """PureLock is een advertentie- en trackingvrije privacysuite en offline versleutelde kluis voor je apps, wachtwoorden, 2FA-codes en privénotities — beveiligd met SQLCipher AES-256 en Android Keystore.

🔒 BELANGRIJKSTE FUNCTIES:
• App Shield & Aangepaste Schema's: Vergrendel elke app met PIN, 3×3-Patroon (onzichtbaar spoor), 4-kwadrant Knock Code of Biometrie, met tijdschema's per app, willekeurig toetsenbord en hervergrendel-pauze.
• Hardware-versleutelde Kluis & Gegevens: Bewaar wachtwoorden, notities, kaarten en 2FA in SQLCipher AES-256; hoofdtoegangscodes worden versleuteld via Android Keystore AES-256-GCM. Inclusief instelbare prullenbak en entropie-inzichten.
• Actieve Beveiliging & Camouflage: Progressieve afkoeltijd tegen brute-force, blokkering van schermafbeeldingen en recente-apps (FLAG_SECURE), werkende Rekenmachine, nep-crash en nood-PIN (Duress).
• Schudden om te Vergrendelen: Directe noodvergrendeling met instelbare gevoeligheid.
• Indringersregistratie & SHA-256-audit: Lokale beveiligingslogs, SHA-256 integriteitscontrole en CSV-export.
• Klembord Auto-Wissen & Wachtwoordgenerator: Genereer sterke wachtwoorden offline en wis gekopieerde geheimen automatisch.
• Geauthenticeerde Autofill, .plk-Back-ups & TV: Lokale Autofill achter hoofdvergrendeling, AES-256-GCM versleutelde .plk back-up & herstel, Snelle Tegels, Widget en Android TV-ondersteuning.

🛡️ 100% LOKALE PRIVACY:
Geen advertenties, geen analytics of telemetrie-SDK's, geen advertentie-ID en geen cloudaccounts. Je versleutelde kluis, codes en logs verlaten nooit je apparaat.

────────────────────────────────────────
TOELICHTING ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock gebruikt de AccessibilityService API uitsluitend voor de kernfunctie App-vergrendeling (App Shield).

• Gebruik: PureLock controleert vensterstatuswijzigingen (TYPE_WINDOW_STATE_CHANGED) uitsluitend om te detecteren wanneer een beveiligde app wordt geopend en direct het vergrendelscherm (PIN, Patroon, Knock Code of Biometrie) te tonen.
• Privacy: PureLock leest, registreert of opent NOOIT scherminhoud, toetsaanslagen, wachtwoorden of persoonlijke berichten (canRetrieveWindowContent uitgeschakeld).
• Geen gegevensverzameling: PureLock verzamelt, uploadt of verzendt geen kluisgegevens, app-gebruik of inloggegevens.

Deze toestemming is optioneel, wordt alleen gevraagd voor App Shield en kan altijd worden gewijzigd in de Toegankelijkheidsinstellingen."""
    },
    {
        "locale": "pl-PL",
        "language": "Polish (Poland)",
        "title": "PureLock: Blokada Aplikacji",
        "short_desc": "Blokada aplikacji offline i sejf AES-256. Bez reklam, śledzenia i chmury.",
        "full_desc": """PureLock to wolny od reklam i telemetrii pakiet prywatności oraz zaszyfrowany sejf offline chroniący aplikacje, hasła, kody 2FA i notatki za pomocą SQLCipher AES-256 i Android Keystore.

🔒 GŁÓWNE FUNKCJE:
• App Shield (Blokada Aplikacji) i Harmonogramy: Zabezpiecz aplikacje kodem PIN, Wzorem 3×3 (niewidoczna ścieżka), Kodem Knock (4 ćwiartki) lub Biometrią. Ustaw harmonogramy godzin i dni dla aplikacji, losową klawiaturę i czas karencji.
• Sejf i Poświadczenia Szyfrowane Sprzętowo: Przechowuj hasła, notatki, karty i kody 2FA w SQLCipher AES-256 z głównymi poświadczeniami szyfrowanymi w spoczynku przez Android Keystore AES-256-GCM, konfigurowalnym koszem i analizą entropii.
• Aktywna Obrona i Kamuflaż: Progresywna blokada czasowa przed atakami brute-force, blokowanie zrzutów ekranu (FLAG_SECURE), działający Kalkulator, fałszywy błąd i PIN przymusu (Duress).
• Potrząśnij, aby Zablokować: Natychmiastowa blokada awaryjna z regulacją czułości akcelerometru.
• Rejestr Intruzów i Audyt SHA-256: Lokalne logi bezpieczeństwa, kryptograficzny audyt integralności SHA-256 i eksport CSV.
• Auto-czyszczenie Schowka i Generator Haseł: Twórz silne hasła offline i automatycznie usuwaj skopiowane sekrety ze schowka.
• Uwierzytelnione Autouzupełnianie, Kopie .plk i TV: Lokalny Autofill chroniony blokadą główną, szyfrowane kopie zapasowe AES-256-GCM (.plk), Szybkie Kafelki, Widżet i Android TV.

🛡️ 100% LOKALNEJ PRYWATNOŚCI:
Zero reklam, zero telemetrii i analityki, brak identyfikatora reklamowego i brak kont w chmurze. Twój zaszyfrowany sejf, dane i dzienniki nigdy nie opuszczają urządzenia.

────────────────────────────────────────
UJAWNIENIE UŻYCIA ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock korzysta z AccessibilityService API wyłącznie w celu zapewnienia głównej funkcji Blokady Aplikacji (App Shield).

• Zastosowanie: Monitoruje zdarzenia zmiany okna (TYPE_WINDOW_STATE_CHANGED) wyłącznie w celu wykrycia uruchomienia chronionej aplikacji i natychmiastowego wyświetlenia ekranu blokady (PIN, Wzór, Knock Code lub Biometria).
• Prywatność: PureLock NIE odczytuje, NIE przechwytuje i NIE uzyskuje dostępu do zawartości ekranu, naciśnięć klawiszy, haseł ani prywatnych wiadomości (canRetrieveWindowContent wyłączone).
• Zero zbierania danych: PureLock nie gromadzi, nie przesyła ani nie udostępnia danych sejfu, historii aplikacji ani poświadczeń.

Uprawnienie jest opcjonalne, wymagane tylko dla App Shield i można je w dowolnym momencie zmienić w Ustawieniach Ułatwień Dostępu."""
    },
    {
        "locale": "uk",
        "language": "Ukrainian",
        "title": "PureLock: Блокування і Сейф",
        "short_desc": "Офлайн-блокування додатків і сейф AES-256. Без реклами, стеження та хмари.",
        "full_desc": """PureLock — автономний комплекс приватності без реклами й телеметрії та зашифрований сейф для захисту додатків, паролів, кодів 2FA і нотаток на базі SQLCipher AES-256 та Android Keystore.

🔒 ОСНОВНІ МОЖЛИВОСТІ:
• App Shield (Блокування додатків) і Розклади: Захист додатків PIN-кодом, графічним ключем 3×3 (невидимий слід), 4-квадрантним Knock-кодом або біометрією. Налаштуйте розклади за часом і днями для кожного додатка, випадкову клавіатуру та період відтермінування.
• Апаратно зашифрований сейф: Зберігання паролів, нотаток, карток і 2FA у SQLCipher AES-256 із шифруванням майстер-даних через Android Keystore AES-256-GCM, налаштовуваним кошиком автоочищення та аналізом ентропії.
• Активний захист і маскування: Прогресивна пауза проти підбору паролів, захист від скриншотів і прев'ю (FLAG_SECURE), робочий Калькулятор, фальшивий збій та екстрений PIN примусу (Duress).
• Струснути для блокування: Миттєве аварійне блокування з налаштуванням чутливості.
• Фіксація зловмисників і аудит SHA-256: Локальний журнал безпеки, перевірка цілісності SHA-256 та експорт CSV.
• Автоочищення буфера і генератор паролів: Створення надійних паролів офлайн та автоматичне очищення буфера обміну.
• Автозаповнення з автентифікацією, Бекапи .plk і TV: Локальний Autofill під майстер-замком, шифровані резервні копії AES-256-GCM (.plk), швидкі плитки, віджет і Android TV.

🛡️ 100% ЛОКАЛЬНА ПРИВАТНІСТЬ:
Без реклами, телеметрії, трекерів і хмарних акаунтів. Ваш зашифрований сейф, облікові дані та журнали ніколи не покидають пристрій.

────────────────────────────────────────
РОЗКРИТТЯ ВИКОРИСТАННЯ ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock використовує AccessibilityService API виключно для основної функції блокування додатків (App Shield).

• Призначення: Відстежує лише події зміни вікна (TYPE_WINDOW_STATE_CHANGED) для виявлення запуску захищеного додатку та миттєвого показу екрана блокування (PIN, графічний ключ, Knock-код або біометрія).
• Приватність: PureLock НЕ читає, НЕ перехоплює та НЕ збирає вміст екрана, натискання клавіш, паролі чи особисті повідомлення (canRetrieveWindowContent вимкнено).
• Нуль збору даних: PureLock не збирає, не завантажує і не передає дані сейфа, використання додатків чи паролі.

Дозвіл є необов'язковим, запитується лише для App Shield і може бути вимкнений у будь-який момент у налаштуваннях Спеціальних можливостей."""
    },
    {
        "locale": "sv-SE",
        "language": "Swedish (Sweden)",
        "title": "PureLock: Applås & Valv",
        "short_desc": "Offline applås & AES-256 valv. Ingen reklam, ingen spårning, inget moln.",
        "full_desc": """PureLock är en reklam- och spårningsfri integritetssvit och ett offline krypterat valv för appar, lösenord, 2FA-koder och anteckningar — skyddat med SQLCipher AES-256 och Android Keystore.

🔒 HUVUDFUNKTIONER:
• App Shield (Applås) & Scheman: Lås appar med PIN, 3×3-Mönster (osynligt spår), 4-kvadrant Knock-kod eller Biometri, med anpassade tidsscheman per app, slumpad knappsats och nådatid.
• Hårdvarukrypterat Valv & Inloggning: Lagra lösenord, anteckningar, kort och 2FA i SQLCipher AES-256 med huvuduppgifter krypterade via Android Keystore AES-256-GCM, inställbar papperskorg och entropianalys.
• Aktivt Skydd & Kamouflage: Progressiv fördröjning mot brute-force, blockering av skärmdumpar (FLAG_SECURE), fungerande Miniräknare, falsk kraschdialog och nöd-PIN (Duress).
• Skaka för att Låsa: Omedelbar nödlåsning med justerbar känslighet.
• Inkräktarlogg & SHA-256-granskning: Lokala säkerhetsloggar, SHA-256 integritetskontroll och CSV-export.
• Auto-rensning av Urklipp & Lösenordsgenerator: Skapa starka lösenord offline och rensa kopierade hemligheter automatiskt.
• Autentiserad Autofyll, .plk-Backuper & TV: Lokal Autofyll bakom huvudlåset, AES-256-GCM krypterade .plk-säkerhetskopior, Snabbpaneler, Widget och Android TV.

🛡️ 100% LOKAL INTEGRITET:
Ingen reklam, ingen analys eller telemetri, inget annons-ID och inga molnkonton. Ditt krypterade valv och dina loggar lämnar aldrig enheten.

────────────────────────────────────────
ANVÄNDNING AV ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock använder AccessibilityService API enbart för kärnfunktionen Applås (App Shield).

• Användning: Övervakar fönsterändringar (TYPE_WINDOW_STATE_CHANGED) enbart för att upptäcka när en skyddad app öppnas och direkt visa låsskärmen (PIN, Mönster, Knock-kod eller Biometri).
• Integritet: PureLock läser, sparar eller överför ALDRIG skärminnehåll, tangenttryckningar, lösenord eller privata meddelanden (canRetrieveWindowContent inaktiverat).
• Ingen datainsamling: PureLock samlar inte in, laddar upp eller skickar dina valvdata, appanvändning eller inloggningsuppgifter.

Behörigheten är valfri, begärs endast för App Shield och kan ändras när som helst i enhetens Tillgänglighetsinställningar."""
    },
    {
        "locale": "da-DK",
        "language": "Danish (Denmark)",
        "title": "PureLock: Applås & Boks",
        "short_desc": "Offline applås & AES-256 boks. Ingen reklamer, ingen sporing, ingen sky.",
        "full_desc": """PureLock er en reklame- og sporingsfri privatlivssuite og offline krypteret boks til apps, adgangskoder, 2FA-koder og private noter — beskyttet med SQLCipher AES-256 og Android Keystore.

🔒 NØGLEFUNKTIONER:
• App Shield (Applås) & Tidsplaner: Lås apps med PIN, 3×3-Mønster (usynligt spor), 4-kvadrant Knock-kode eller Biometri, med tilpassede tidsplaner pr. app, tilfældigt tastatur og henstandstid.
• Hardwarekrypteret Boks & Legitimationsoplysninger: Gem adgangskoder, noter, kort og 2FA i SQLCipher AES-256 med hovedkoder krypteret i Android Keystore AES-256-GCM, justerbar papirkurv og entropianalyse.
• Aktiv Beskyttelse & Camouflage: Progressiv venteperiode mod brute-force, blokering af skærmbilleder (FLAG_SECURE), fungerende Lommeregner, falsk nedbrud og nød-PIN (Duress).
• Ryst for at Låse: Øjeblikkelig nødlåsning med justerbar følsomhed.
• Ubuden Gæst-log & SHA-256-revision: Lokale sikkerhedslogs, SHA-256 integritetstjek og CSV-eksport.
• Auto-ryd Udklipsholder & Adgangskodegenerator: Opret stærke koder offline og ryd kopierede hemmeligheder automatisk.
• Godkendt Autofyld, .plk-Backups & TV: Lokal Autofyld beskyttet af hovedlås, AES-256-GCM krypterede .plk-backups, Hurtigfelter, Widget og Android TV.

🛡️ 100% LOKALT PRIVATLIV:
Ingen reklamer, ingen telemetri eller sporing, intet annonce-ID og ingen skykonti. Din krypterede boks og logs forlader aldrig din enhed.

────────────────────────────────────────
OPLYSNING OM ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock bruger udelukkende AccessibilityService API til kernefunktionen Applås (App Shield).

• Brug: Overvåger vinduesændringer (TYPE_WINDOW_STATE_CHANGED) udelukkende for at opdage, når en beskyttet app åbnes, og straks vise låseskærmen (PIN, Mønster, Knock-kode eller Biometri).
• Privatliv: PureLock læser, gemmer eller tilgår ALDRIG skærmindhold, tastetryk, adgangskoder eller private beskeder (canRetrieveWindowContent deaktiveret).
• Ingen dataindsamling: PureLock indsamler, uploader eller sender ikke dine boksdata, appbrug eller adgangskoder.

Tilladelsen er valgfri, anmodes kun for App Shield og kan når som helst ændres i enhedens Tilgængelighedsindstillinger."""
    },
    {
        "locale": "fi-FI",
        "language": "Finnish (Finland)",
        "title": "PureLock: Sovelluslukko",
        "short_desc": "Offline-sovelluslukko & AES-256 holvi. Ei mainoksia, seurantaa tai pilveä.",
        "full_desc": """PureLock on mainokseton ja seurannaton offline-tietosuojapaketti sekä salattu holvi sovelluksille, salasanoille, 2FA-koodeille ja muistiinpanoille — suojattu SQLCipher AES-256:lla ja Android Keystorella.

🔒 PÄÄOMINAISUUDET:
• App Shield (Sovelluslukko) & Aikataulut: Suojaa sovellukset PIN-koodilla, 3×3-kuviolla (näkymätön viiva), 4-alueisella Knock-koodilla tai biometriikalla; sovelluskohtaiset aikataulut, satunnainen näppäimistö ja viiveaika.
• Laitteistosalattu Holvi & Tunnukset: Tallenna salasanat, muistiinpanot, kortit ja 2FA SQLCipher AES-256 -kantaan; päätunnukset salataan Android Keystore AES-256-GCM:llä. Säädettävä roskakori ja entropia-analyysi.
• Aktiivinen Suojaus & Naamiointi: Asteittainen brute-force-lukitusviive, kuvakaappausten esto (FLAG_SECURE), toimiva Laskin, valekaatumisikkuna ja hätä-PIN (Duress).
• Ravista Lukitaksesi: Välitön hätälukitus säädettävällä herkkyydellä.
• Tunkeilijaloki & SHA-256-tarkastus: Paikalliset lokit, SHA-256-eheystarkistus ja CSV-vienti.
• Leikepöydän Automaattityhjennys & Salasanageneraattori: Luo vahvoja salasanoja offline-tilassa ja tyhjennä kopioidut salaisuudet automaattisesti.
• Todennettu Automaattitäyttö, .plk-Varmuuskopiot & TV: Päälukon takana toimiva paikallinen Autofill, AES-256-GCM salatut .plk-varmuuskopiot, Pikaruudut, Widget ja Android TV.

🛡️ 100% PAIKALLINEN TIETOSUOJA:
Ei mainoksia, ei analytiikka- tai telemetria-SDK:ita, ei mainostunnusta eikä pilvitilejä. Salatut tietosi ja lokisi eivät koskaan poistu laitteeltasi.

────────────────────────────────────────
ACCESSIBILITY SERVICE API -TIEDONANTO
────────────────────────────────────────
PureLock käyttää AccessibilityService API -rajapintaa ainoastaan ydintoimintoonsa Sovelluslukkoon (App Shield).

• Käyttö: Seuraa ikkunan tilan muutoksia (TYPE_WINDOW_STATE_CHANGED) vain havaitakseen suojatun sovelluksen avaamisen ja näyttääkseen lukitusnäytön (PIN, Kuvio, Knock-koodi tai Biometriikka) heti.
• Tietosuoja: PureLock EI KOSKAAN lue, tallenna tai lähetä näytön sisältöä, näppäinpainalluksia, salasanoja tai yksityisviestejä (canRetrieveWindowContent pois käytöstä).
• Ei tiedonkeruuta: PureLock ei kerää, lataa tai välitä holvitietojasi, sovelluskäyttöäsi tai tunnuksiasi.

Lupa on valinnainen, sitä pyydetään vain App Shieldiä varten ja sen voi muuttaa milloin tahansa laitteen Esteettömyysasetuksissa."""
    },
    {
        "locale": "no-NO",
        "language": "Norwegian (Norway)",
        "title": "PureLock: Applås og Hvelv",
        "short_desc": "Offline applås & AES-256 hvelv. Ingen reklame, ingen sporing, ingen sky.",
        "full_desc": """PureLock er en reklame- og sporingsfri personvernpakke og offline kryptert hvelv for apper, passord, 2FA-koder og private notater — beskyttet med SQLCipher AES-256 og Android Keystore.

🔒 HOVEDFUNKSJONER:
• App Shield (Applås) & Tidsplaner: Lås apper med PIN, 3×3-Mønster (usynlig spor), 4-kvadrant Knock-kode eller Biometri, med egne tidsplaner per app, tilfeldig tastatur og utsettelsestid.
• Maskinvarekryptert Hvelv & Pålogging: Lagre passord, notater, kort og 2FA i SQLCipher AES-256 med hovedkoder kryptert i Android Keystore AES-256-GCM, justerbar papirkurv og entropianalyse.
• Aktiv Beskyttelse & Kamuflasje: Progressiv ventetid mot brute-force, skjermbildeblokkering (FLAG_SECURE), fungerende Kalkulator, falsk krasjdialog og nød-PIN (Duress).
• Rist for å Låse: Umiddelbar nødlåsing med justerbar følsomhet.
• Inntrengerlogg & SHA-256-revisjon: Lokale sikkerhetslogger, SHA-256 integritetssjekk og CSV-eksport.
• Auto-tømming av Utklippstavle & Passordgenerator: Lag sterke passord offline og slett kopierte hemmeligheter automatisk.
• Autentisert Autofyll, .plk-Sikkerhetskopier & TV: Lokal Autofyll bak hovedlås, AES-256-GCM krypterte .plk-sikkerhetskopier, Hurtigfliser, Widget og Android TV.

🛡️ 100% LOKALT PERSONVERN:
Ingen reklame, ingen analyse eller telemetri, ingen annonse-ID og ingen skykontoer. Krypterte data og logger forlater aldri enheten din.

────────────────────────────────────────
OPPLYSNING OM ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock bruker AccessibilityService API utelukkende til kjernefunksjonen Applås (App Shield).

• Bruk: Overvåker vindusendringer (TYPE_WINDOW_STATE_CHANGED) kun for å oppdage når en beskyttet app åpnes og vise låseskjermen (PIN, Mønster, Knock-kode eller Biometri) umiddelbart.
• Personvern: PureLock leser, lagrer eller sender ALDRI skjerminnhold, tastetrykk, passord eller private meldinger (canRetrieveWindowContent deaktivert).
• Ingen datainnsamling: PureLock samler ikke inn, laster opp eller overfører hvelvdata, appbruk eller påloggingsinformasjon.

Tillatelsen er valgfri, bes kun om for App Shield og kan når som helst endres i Tilgjengelighetsinnstillingene."""
    },
    {
        "locale": "cs-CZ",
        "language": "Czech (Czechia)",
        "title": "PureLock: Zámek Aplikací",
        "short_desc": "Offline zámek aplikací a trezor AES-256. Bez reklam, sledování a cloudu.",
        "full_desc": """PureLock je bezpečnostní sada bez reklam a telemetrie a offline šifrovaný trezor pro vaše aplikace, hesla, 2FA kódy a soukromé poznámky — chráněný pomocí SQLCipher AES-256 a Android Keystore.

🔒 HLAVNÍ FUNKCE:
• App Shield (Zámek aplikací) a Rozvrhy: Zamkněte aplikace pomocí PIN, Vzoru 3×3 (neviditelná stopa), 4-kvadrantového Knock kódu nebo Biometrie. Nastavte časové rozvrhy pro jednotlivé aplikace, náhodnou klávesnici a dobu odkladu.
• Hardwarově šifrovaný trezor: Ukládejte hesla, poznámky, karty a 2FA v SQLCipher AES-256 s hlavními údaji šifrovanými přes Android Keystore AES-256-GCM, nastavitelným košem a analýzou entropie.
• Aktivní ochrana a Kamufláž: Progresivní prodleva proti hrubé síle, blokování snímků obrazovky (FLAG_SECURE), funkční Kalkulačka, falešný pád a nátlakový PIN (Duress).
• Zatřesení pro zamknutí: Okamžité nouzové uzamčení s nastavitelnou citlivostí.
• Záznam narušitelů a SHA-256 audit: Lokální bezpečnostní protokoly, kryptografické ověření integrity SHA-256 a export CSV.
• Automatické mazání schránky a generátor hesel: Generujte silná hesla offline a automaticky mazejte zkopírovaná tajemství.
• Ověřené automatické vyplňování, Zálohy .plk a TV: Lokální Autofill chráněný hlavním zámkem, šifrované zálohy AES-256-GCM (.plk), rychlé dlaždice, widget a Android TV.

🛡️ 100% LOKÁLNÍ SOUKROMÍ:
Žádné reklamy, žádná telemetrie ani sledování, žádné reklamní ID ani cloudové účty. Váš šifrovaný trezor a záznamy nikdy neopustí zařízení.

────────────────────────────────────────
PROHLÁŠENÍ K ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock používá AccessibilityService API výhradně pro hlavní funkci Zámek aplikací (App Shield).

• Použití: Sleduje události změny okna (TYPE_WINDOW_STATE_CHANGED) pouze za účelem zjištění otevření chráněné aplikace a okamžitého zobrazení zamykací obrazovky (PIN, Vzor, Knock kód nebo Biometrie).
• Soukromí: PureLock NIKDY nečte, neshromažďuje ani nepřenáší obsah obrazovky, stisky kláves, hesla ani osobní zprávy (canRetrieveWindowContent vypnuto).
• Žádný sběr dat: PureLock neshromažďuje, nenahrává ani neodesílá data trezoru, používání aplikací ani přihlašovací údaje.

Oprávnění je volitelné, vyžaduje se pouze pro App Shield a lze jej kdykoli změnit v Nastavení přístupnosti."""
    },
    {
        "locale": "hu-HU",
        "language": "Hungarian (Hungary)",
        "title": "PureLock: Alkalmazászár",
        "short_desc": "Offline alkalmazászár és AES-256 széf. Reklám, követés és felhő nélkül.",
        "full_desc": """A PureLock egy reklám- és követésmentes offline adatvédelmi csomag és titkosított széf alkalmazásai, jelszavai, 2FA kódjai és jegyzetei védelmére SQLCipher AES-256 és Android Keystore segítségével.

🔒 FŐBB FUNKCIÓK:
• App Shield (Alkalmazászár) és Ütemezés: Zároljon bármely appot PIN-kóddal, 3×3 Mintával (láthatatlan útvonal), 4-negyedes Knock kóddal vagy Biometriával. Állítson be alkalmazásonkénti időzítést, véletlen billentyűzetet és türelmi időt.
• Hardveresen Titkosított Széf: Tároljon jelszavakat, jegyzeteket, kártyákat és 2FA kódokat SQLCipher AES-256 adatbázisban, Android Keystore AES-256-GCM mesterhitelesítéssel, állítható lomtárral és entrópia-elemzéssel.
• Aktív Védelem és Álcázás: Fokozatos brute-force késleltetés, képernyőkép- és előnézet-tiltás (FLAG_SECURE), működő Számológép álca, hamis összeomlás és kényszerített vész-PIN (Duress).
• Rázásra Zárolás: Azonnali vészzárolás állítható érzékenységgel.
• Behatoló Napló és SHA-256 Audit: Helyi biztonsági naplók, SHA-256 integritás-ellenőrzés és CSV export.
• Vágólap Auto-törlés és Jelszógenerátor: Erős jelszavak generálása offline és a másolt adatok automatikus törlése.
• Hitelesített Automatikus Kitöltés, .plk Mentés és TV: Mesterzárral védett helyi Autofill, AES-256-GCM titkosított .plk biztonsági mentések, Gyorsbeállítások, Widget és Android TV.

🛡️ 100% HELYI ADATVÉDELEM:
Nincs reklám, nincs analitika vagy telemetria, nincs hirdetési azonosító és nincs felhőfiók. Titkosított adatai és naplói soha nem hagyják el az eszközét.

────────────────────────────────────────
ACCESSIBILITY SERVICE API NYILATKOZAT
────────────────────────────────────────
A PureLock kizárólag az Alkalmazászár (App Shield) fő funkcióhoz használja az AccessibilityService API-t.

• Használat: Kizárólag az ablakállapot-változásokat (TYPE_WINDOW_STATE_CHANGED) figyeli a védett appok megnyitásának észleléséhez és a zárolási képernyő (PIN, Minta, Knock kód vagy Biometria) azonnali megjelenítéséhez.
• Adatvédelem: A PureLock SOHA nem olvas, rögzít vagy továbbít képernyőtartalmat, billentyűleütéseket, jelszavakat vagy személyes üzeneteket (canRetrieveWindowContent letiltva).
• Nulla adatgyűjtés: A PureLock nem gyűjt, nem tölt fel és nem továbbít széfadatokat, alkalmazáshasználatot vagy hitelesítő adatokat.

Az engedély opcionális, csak az App Shield használatához szükséges, és bármikor módosítható a Kisegítő lehetőségek beállításaiban."""
    },
    {
        "locale": "ro",
        "language": "Romanian",
        "title": "PureLock: Blocare Aplicații",
        "short_desc": "Blocare aplicații offline și seif AES-256. Fără reclame, urmărire sau cloud.",
        "full_desc": """PureLock este o suită de confidențialitate fără reclame și fără telemetrie și un seif criptat offline pentru aplicații, parole, coduri 2FA și notițe private — protejat cu SQLCipher AES-256 și Android Keystore.

🔒 FUNCȚII PRINCIPALE:
• App Shield (Blocare Aplicații) și Programe: Protejează orice aplicație cu PIN, Model 3×3 (traseu invizibil), Cod Knock în 4 cadrane sau Biometrie, cu intervale orare și zile personalizate per aplicație, tastatură aleatorie și perioadă de grație.
• Seif și Credențiale Criptate Hardware: Salvează parole, notițe, carduri și 2FA în SQLCipher AES-256, cu credențiale principale criptate prin Android Keystore AES-256-GCM, coș de gunoi configurabil și analiză a entropiei.
• Protecție Activă și Camuflaj: Blocare progresivă împotriva forței brute, blocarea capturilor de ecran (FLAG_SECURE), Calculator funcțional, eroare falsă și PIN sub constrângere (Duress).
• Scutură pentru Blocare: Blocare de urgență instantanee cu sensibilitate reglabilă.
• Jurnal Intruși și Audit SHA-256: Înregistrează încercările eșuate local, verifică integritatea criptografică SHA-256 și exportă CSV.
• Curățare Clipboard și Generator Offline: Creează parole puternice și șterge automat secretele copiate.
• Completare Automată Autentificată, Backup .plk și TV: Autofill local protejat de blocarea principală, backup-uri criptate AES-256-GCM (.plk), Dale Rapide, Widget și Android TV.

🛡️ CONFIDENȚIALITATE 100% LOCALĂ:
Fără reclame, fără SDK-uri de analiză sau telemetrie, fără ID publicitar și fără conturi în cloud. Seiful criptat și jurnalele tale nu părăsesc niciodată dispozitivul.

────────────────────────────────────────
UTILIZAREA ȘI DIVULGAREA ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock folosește AccessibilityService API strict pentru funcția principală de Blocare Aplicații (App Shield).

• Cum se utilizează: Monitorizează evenimentele de schimbare a ferestrei (TYPE_WINDOW_STATE_CHANGED) doar pentru a detecta deschiderea unei aplicații protejate și a afișa imediat ecranul de blocare (PIN, Model, Knock Code sau Biometrie).
• Confidențialitate: PureLock NU citește, NU colectează și NU accesează conținutul ecranului, tastele apăsate, parolele sau mesajele personale (canRetrieveWindowContent dezactivat).
• Zero colectare de date: PureLock nu colectează, nu încarcă și nu transmite datele seifului, utilizarea aplicațiilor sau credențialele tale.

Permisiunea este opțională, solicitată doar pentru App Shield, și poate fi modificată oricând din Setările de Accesibilitate."""
    },
    {
        "locale": "el-GR",
        "language": "Greek (Greece)",
        "title": "PureLock: Κλείδωμα Εφαρμογών",
        "short_desc": "Offline κλείδωμα & θησαυροφυλάκιο AES-256. Χωρίς διαφημίσεις ή παρακολούθηση.",
        "full_desc": """Το PureLock είναι μια σουίτα απορρήτου χωρίς διαφημίσεις και χωρίς τηλεμετρία και ένα offline κρυπτογραφημένο θησαυροφυλάκιο για εφαρμογές, κωδικούς, 2FA και σημειώσεις με SQLCipher AES-256 και Android Keystore.

🔒 ΚΥΡΙΑ ΧΑΡΑΚΤΗΡΙΣΤΙΚΑ:
• App Shield (Κλείδωμα Εφαρμογών) & Προγράμματα: Κλειδώστε εφαρμογές με PIN, Μοτίβο 3×3 (αόρατη διαδρομή), Κωδικό Knock 4 τεταρτημορίων ή Βιομετρικά, με ωράρια ανά εφαρμογή, τυχαίο πληκτρολόγιο και περίοδο χάριτος.
• Κρυπτογραφημένο Θησαυροφυλάκιο: Αποθηκεύστε κωδικούς, σημειώσεις, κάρτες και 2FA σε SQLCipher AES-256 με κρυπτογράφηση κύριων στοιχείων μέσω Android Keystore AES-256-GCM, ρυθμιζόμενο κάδο και ανάλυση εντροπίας.
• Ενεργή Προστασία & Καμουφλάζ: Προοδευτική καθυστέρηση κατά brute-force, αποκλεισμός στιγμιοτύπων οθόνης (FLAG_SECURE), λειτουργική Αριθμομηχανή, ψεύτικο σφάλμα και PIN έκτακτης ανάγκης (Duress).
• Κούνημα για Κλείδωμα: Άμεσο κλείδωμα έκτακτης ανάγκης με ρυθμιζόμενη ευαισθησία.
• Καταγραφή Εισβολέων & Έλεγχος SHA-256: Τοπικά αρχεία ασφαλείας, κρυπτογραφικός έλεγχος ακεραιότητας SHA-256 και εξαγωγή CSV.
• Καθαρισμός Προχείρου & Γεννήτρια Κωδικών: Δημιουργεί ισχυρούς κωδικούς offline και διαγράφει αυτόματα τα αντιγραμμένα μυστικά.
• Αυτόματη Συμπλήρωση, Αντίγραφα .plk & TV: Τοπικό Autofill με κύρια επαλήθευση, κρυπτογραφημένα αντίγραφα AES-256-GCM (.plk), Πλακίδια, Widget και Android TV.

🛡️ 100% ΤΟΠΙΚΟ ΑΠΟΡΡΗΤΟ:
Χωρίς διαφημίσεις, χωρίς SDK ανάλυσης ή τηλεμετρίας, χωρίς διαφημιστικό ID και χωρίς λογαριασμούς cloud. Τα κρυπτογραφημένα δεδομένα σας δεν φεύγουν ποτέ από τη συσκευή σας.

────────────────────────────────────────
ΓΝΩΣΤΟΠΟΙΗΣΗ ΧΡΗΣΗΣ ACCESSIBILITY SERVICE API
────────────────────────────────────────
Το PureLock χρησιμοποιεί το AccessibilityService API αποκλειστικά για τη βασική λειτουργία Κλειδώματος Εφαρμογών (App Shield).

• Χρήση: Παρακολουθεί αλλαγές παραθύρων (TYPE_WINDOW_STATE_CHANGED) μόνο για να εντοπίζει πότε ανοίγει μια προστατευμένη εφαρμογή και να εμφανίζει άμεσα την οθόνη κλειδώματος (PIN, Μοτίβο, Knock Code ή Βιομετρικά).
• Απόρρητο: Το PureLock ΔΕΝ διαβάζει, ΔΕΝ καταγράφει και ΔΕΝ αποκτά πρόσβαση σε περιεχόμενο οθόνης, πληκτρολογήσεις, κωδικούς ή προσωπικά μηνύματα (το canRetrieveWindowContent είναι απενεργοποιημένο).
• Μηδενική συλλογή δεδομένων: Το PureLock δεν συλλέγει, δεν ανεβάζει ούτε μεταδίδει τα δεδομένα του θησαυροφυλακίου, τη χρήση εφαρμογών ή τα διαπιστευτήριά σας.

Η άδεια είναι προαιρετική, ζητείται μόνο για το App Shield και μπορεί να αλλάξει ανά πάσα στιγμή στις Ρυθμίσεις Προσβασιμότητας."""
    }
]

# Strict Google Play Console length & policy compliance validation
FORBIDDEN_PATTERNS = [
    r"no internet permission",
    r"without internet permission",
    r"sin permiso de internet",
    r"sem nenhuma permissão de internet",
    r"sans aucune permission internet",
    r"ohne internetberechtigung",
    r"senza autorizzazioni internet",
    r"без разрешения на доступ к интернету",
    r"インターネット通信権限",
    r"인터넷 권한",
    r"没有任何联网权限",
    r"無任何網路連線權限",
    r"không yêu cầu quyền truy cập internet",
    r"tanpa izin akses internet",
    r"कोई इंटरनेट अनुमति नहीं",
    r"internet izni olmadan",
    r"ไม่มีการขอสิทธิ์เข้าถึงอินเทอร์เน็ต",
    r"لا يطلب أي إذن للوصول للإنترنت",
]

errors = []
seen_locales = set()
for item in listings:
    locale = item["locale"]
    if locale in seen_locales:
        errors.append(f"Duplicate locale: {locale}")
    seen_locales.add(locale)

    t_len = len(item["title"])
    s_len = len(item["short_desc"])
    f_len = len(item["full_desc"])
    if t_len == 0 or t_len > 30:
        errors.append(f"Invalid Title length in {locale}: {t_len} chars ('{item['title']}')")
    if s_len == 0 or s_len > 80:
        errors.append(f"Invalid Short Desc length in {locale}: {s_len} chars ('{item['short_desc']}')")
    if f_len == 0 or f_len > 4000:
        errors.append(f"Invalid Full Desc length in {locale}: {f_len} chars")
    if "TYPE_WINDOW_STATE_CHANGED" not in item["full_desc"]:
        errors.append(f"Missing TYPE_WINDOW_STATE_CHANGED disclosure in {locale}")
    if "canRetrieveWindowContent" not in item["full_desc"]:
        errors.append(f"Missing canRetrieveWindowContent disclosure in {locale}")

    combined = f"{item['title']}\n{item['short_desc']}\n{item['full_desc']}".lower()
    for pat in FORBIDDEN_PATTERNS:
        if re.search(pat, combined, re.IGNORECASE):
            errors.append(f"Forbidden inaccurate permission claim ('{pat}') found in {locale}")

if errors:
    for err in errors:
        print("ERROR:", err)
    sys.exit(1)

csv_path = "store_listing.csv"
fieldnames = [
    "Language / Locale",
    "Language Name",
    "Title (<=30)",
    "Short Description (<=80)",
    "Full Description (<=4000)",
]

with open(csv_path, "w", newline="", encoding="utf-8-sig") as f:
    writer = csv.writer(f)
    writer.writerow(fieldnames)
    for item in listings:
        writer.writerow([
            item["locale"],
            item["language"],
            item["title"],
            item["short_desc"],
            item["full_desc"],
        ])

print(f"Successfully generated {csv_path} with {len(listings)} validated languages!")
