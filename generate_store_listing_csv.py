import csv
import sys

# Complete translations for Google Play Store Listing (PureLock v2.6)
# Strict Google Play Console Constraints:
# - Title <= 30 chars
# - Short Description <= 80 chars
# - Full Description <= 4000 chars (Must include AccessibilityService API Disclosure)

listings = [
    {
        "locale": "en-US",
        "language": "English (United States)",
        "title": "PureLock: Privacy App Lock",
        "short_desc": "Offline App Lock & SQLCipher AES-256 Vault. Zero ads, zero cloud, pure privacy.",
        "full_desc": """PureLock is a lightweight, ad-free privacy suite and encrypted vault built to protect your apps, passwords, 2FA codes, and private notes strictly on your device. Powered by SQLCipher AES-256 and hardware-backed Android Keystore encryption, PureLock works 100% offline with zero cloud accounts and zero ads.

🔒 CORE SECURITY FEATURES:
• App Shield (App Lock): Protect any installed app (Banking, Social, Photos, System) with PIN, 3×3 Pattern, 4-Quadrant Knock Code, or Biometrics (Fingerprint / Face ID).
• Hardware-Encrypted Vault: Store passwords, secure notes, cards, 2FA codes, and PINs in a SQLCipher AES-256 database wrapped by Android Keystore. Includes Favorites, Pinning, Trash auto-purge, and Entropy Insights.
• Camouflage & Stealth Decoys: Disguise your lock screen as a working Calculator or Fake Crash dialog, plus Randomized Keypad Scramble, Invisible Pattern Trail, and a Panic Duress PIN.
• Shake-to-Lock Gesture: Gravity-filtered accelerometer emergency lockdown with adjustable sensitivity (High / Medium / Low).
• Intruder Capture & Audit Analytics: Records failed unlock attempts locally with a SHA-256 Cryptographic Vault Integrity Audit and CSV log export.
• Clipboard Auto-Wipe & Password Generator: Generates high-entropy passwords offline and automatically wipes copied secrets from clipboard memory.
• Local Autofill, Quick Tiles, Widget & TV: Offline Android Autofill Service, Quick Settings Lockdown tile, Home Screen Vault Widget, encrypted .plk backups, and Android TV support.

🛡️ 100% LOCAL PRIVACY GUARANTEE:
PureLock operates offline without cloud sync, ads, or user accounts. Your encrypted vault, PINs, patterns, and logs remain strictly on your device.

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
        "short_desc": "Bloqueo de apps offline y bóveda AES-256 SQLCipher. Sin anuncios ni nube.",
        "full_desc": """PureLock es una suite de privacidad sin anuncios y bóveda cifrada 100% offline para proteger tus aplicaciones, contraseñas, códigos 2FA y notas privadas directamente en tu dispositivo con SQLCipher AES-256 y Android Keystore.

🔒 FUNCIONES PRINCIPALES:
• Escudo de Apps (App Shield): Protege cualquier aplicación con PIN, Patrón 3×3 (con trazo invisible), Código Knock de 4 cuadrantes o Biometría. Incluye teclado aleatorio y periodo de gracia configurable.
• Bóveda Cifrada por Hardware: Guarda contraseñas, notas seguras, tarjetas, 2FA y PIN con cifrado SQLCipher AES-256, papelera con autopurga y estadísticas de entropía.
• Camuflaje y Señuelos: Disfraza el bloqueo como Calculadora funcional o diálogo de error falso, además de PIN de coacción (Duress) para emergencias.
• Agitar para Bloquear: Bloqueo de emergencia instantáneo al agitar el teléfono con sensibilidad ajustable.
• Registro de Intrusos y Auditoría SHA-256: Registra intentos fallidos localmente, verifica la integridad criptográfica SHA-256 y exporta registros en CSV.
• Autolimpieza del Portapapeles y Generador Offline: Crea contraseñas seguras y borra automáticamente los secretos copiados.
• Autocompletado, Mosaicos y Widget: Servicio de autocompletado offline, mosaicos rápidos, widget de inicio, copias cifradas .plk y soporte para Android TV.

🛡️ GARANTÍA DE PRIVACIDAD LOCAL:
PureLock funciona sin cuentas en la nube ni anuncios. Tus datos cifrados permanecen exclusivamente en tu dispositivo.

────────────────────────────────────────
USO Y DIVULGACIÓN DE LA API DE ACCESIBILIDAD
────────────────────────────────────────
PureLock utiliza la API de AccessibilityService estrictamente para proporcionar su función principal de Bloqueo de Aplicaciones (App Shield).

• Cómo se utiliza: PureLock monitorea eventos de cambio de ventana (TYPE_WINDOW_STATE_CHANGED) exclusivamente para detectar cuándo se abre una app protegida en primer plano y mostrar de inmediato la pantalla de bloqueo seguro (PIN, Patrón, Knock o Biometría).
• Privacidad y Seguridad: PureLock NO lee, captura ni accede al contenido de la pantalla, pulsaciones de teclas, contraseñas ni mensajes personales.
• Cero recolección de datos personales: Tus secretos y actividad de apps nunca salen de tu dispositivo."""
    },
    {
        "locale": "es-ES",
        "language": "Spanish (Spain)",
        "title": "PureLock: Bloqueo de Apps",
        "short_desc": "Bloqueo de apps offline y bóveda AES-256 SQLCipher. Sin anuncios ni nube.",
        "full_desc": """PureLock es una suite de seguridad sin anuncios y bóveda cifrada 100% offline diseñada para proteger tus aplicaciones, contraseñas, códigos 2FA y notas privadas en local mediante SQLCipher AES-256 y Android Keystore.

🔒 FUNCIONES PRINCIPALES:
• Escudo de Apps (App Shield): Bloquea cualquier aplicación con PIN, Patrón (trazo invisible), Código Knock de 4 cuadrantes o Biometría, con teclado aleatorio y periodo de gracia.
• Bóveda Cifrada por Hardware: Almacena contraseñas, notas, tarjetas y 2FA en una base de datos SQLCipher AES-256 con papelera automática y análisis de entropía.
• Camuflaje y Señuelos: Pantalla de Calculadora funcional, aviso de cierre falso y PIN de coacción para situaciones de emergencia.
• Agitar para Bloquear: Bloqueo inmediato al agitar el dispositivo con sensibilidad regulable.
• Captura de Intrusos y Auditoría SHA-256: Registra accesos fallidos en local y verifica la integridad criptográfica de la bóveda.
• Portapapeles Seguro y Generador Offline: Genera claves robustas y limpia automáticamente el portapapeles tras copiar.
• Autocompletado, Accesos Rápidos y Widget: Autocompletado local, mosaicos de Ajustes Rápidos, widget de pantalla, copias cifradas .plk y Android TV.

🛡️ GARANTÍA DE PRIVACIDAD LOCAL:
Sin sincronización en la nube, sin cuentas y sin anuncios. Todos tus datos permanecen únicamente en tu dispositivo.

────────────────────────────────────────
USO Y DIVULGACIÓN DE LA API DE ACCESIBILIDAD
────────────────────────────────────────
PureLock utiliza la API de AccessibilityService estrictamente para ofrecer su función principal de Bloqueo de Aplicaciones (App Shield).

• Cómo se utiliza: PureLock supervisa los eventos de cambio de ventana (TYPE_WINDOW_STATE_CHANGED) únicamente para detectar cuándo se abre una app protegida y mostrar al instante el bloqueo seguro (PIN, Patrón, Knock o Biometría).
• Privacidad y Seguridad: PureLock NO lee, captura ni accede al contenido de la pantalla, pulsaciones, contraseñas ni mensajes personales.
• Cero recopilación de datos personales: Tus datos cifrados permanecen exclusivamente en tu dispositivo."""
    },
    {
        "locale": "pt-BR",
        "language": "Portuguese (Brazil)",
        "title": "PureLock: Bloqueio de Apps",
        "short_desc": "Bloqueio de apps offline e cofre AES-256 SQLCipher. Sem anúncios e sem nuvem.",
        "full_desc": """PureLock é um pacote de privacidade sem anúncios e cofre criptografado 100% offline para proteger aplicativos, senhas, códigos 2FA e notas privadas no seu aparelho com SQLCipher AES-256 e Android Keystore.

🔒 RECURSOS PRINCIPAIS:
• App Shield (Bloqueio de Apps): Proteja qualquer aplicativo com PIN, Padrão (trilha invisível), Knock Code de 4 quadrantes ou Biometria, com teclado embaralhado e tempo de carência.
• Cofre Criptografado por Hardware: Guarde senhas, notas seguras, cartões e códigos 2FA em SQLCipher AES-256 com lixeira automática e análise de entropia.
• Camuflagem e Disfarces: Disfarce a tela de bloqueio como Calculadora funcional ou diálogo de falha falsa, além de PIN de coação (Duress).
• Agite para Bloquear: Bloqueio de emergência instantâneo ao agitar o celular com sensibilidade ajustável.
• Registro de Intrusos e Auditoria SHA-256: Registra tentativas inválidas localmente e realiza verificação criptográfica de integridade.
• Limpeza Automática da Área de Transferência e Gerador Offline: Cria senhas fortes e apaga segredos copiados automaticamente.
• Preenchimento Automático, Blocos Rápidos e Widget: Autofill offline, blocos de Configurações Rápidas, widget na tela inicial, backup cifrado .plk e Android TV.

🛡️ PRIVACIDADE 100% LOCAL:
Sem contas na nuvem e sem anúncios. Seus dados criptografados permanecem exclusivamente no seu dispositivo.

────────────────────────────────────────
USO E DIVULGAÇÃO DA API ACCESSIBILITYSERVICE
────────────────────────────────────────
O PureLock utiliza a API AccessibilityService estritamente para fornecer sua função principal de Bloqueio de Aplicativos (App Shield).

• Como é utilizado: O PureLock monitora eventos de alteração de janela (TYPE_WINDOW_STATE_CHANGED) exclusivamente para detectar quando um app protegido é aberto em primeiro plano e exibir imediatamente a tela de bloqueio seguro (PIN, Padrão, Knock ou Biometria).
• Privacidade e Proteção de Dados: O PureLock NÃO lê, captura ou acessa conteúdo da tela, digitação, senhas ou mensagens pessoais.
• Zero Coleta de Dados Pessoais: Seus segredos permanecem estritamente offline no aparelho."""
    },
    {
        "locale": "pt-PT",
        "language": "Portuguese (Portugal)",
        "title": "PureLock: Bloqueio de Apps",
        "short_desc": "Bloqueio de apps offline e cofre AES-256 SQLCipher. Sem anúncios nem nuvem.",
        "full_desc": """PureLock é uma suite de privacidade sem anúncios e cofre encriptado 100% offline para proteger aplicações, palavras-passe, códigos 2FA e notas privadas com SQLCipher AES-256 e Android Keystore.

🔒 FUNCIONALIDADES PRINCIPAIS:
• Bloqueio de Aplicações (App Shield): Proteja aplicações com PIN, Padrão invisível, Código Knock de 4 quadrantes ou Biometria, com teclado aleatório e período de tolerância.
• Cofre Encriptado por Hardware: Guarde palavras-passe, notas, cartões e 2FA em SQLCipher AES-256 com reciclagem automática e estatísticas de entropia.
• Camuflagem e Disfarces: Disfarce de Calculadora funcional, erro falso e PIN de coação de emergência.
• Agitar para Bloquear: Bloqueio imediato ao abanar o telemóvel com sensibilidade ajustável.
• Captura de Intrusos e Auditoria SHA-256: Regista tentativas falhadas localmente e verifica a integridade criptográfica do cofre.
• Limpeza de Área de Transferência e Gerador Offline: Cria credenciais seguras e limpa automaticamente segredos copiados.
• Preenchimento Automático, Mosaicos e Widget: Autofill offline, mosaicos rápidos, widget, cópias encriptadas .plk e suporte para Android TV.

🛡️ PRIVACIDADE 100% LOCAL:
Sem nuvem, sem contas e sem anúncios. Os seus dados permanecem exclusivamente no dispositivo.

────────────────────────────────────────
USO E DIVULGAÇÃO DA API ACCESSIBILITYSERVICE
────────────────────────────────────────
O PureLock utiliza a API AccessibilityService estritamente para fornecer a função principal de Bloqueio de Aplicações (App Shield).

• Como é utilizado: O PureLock monitoriza eventos de mudança de janela (TYPE_WINDOW_STATE_CHANGED) apenas para detetar a abertura de uma aplicação protegida e apresentar de imediato o ecrã de bloqueio seguro.
• Privacidade: O PureLock NÃO lê, captura ou acede a conteúdo do ecrã, teclas premidas, palavras-passe ou mensagens pessoais."""
    },
    {
        "locale": "fr-FR",
        "language": "French (France)",
        "title": "PureLock: Verrouillage Apps",
        "short_desc": "Verrouillage d'apps hors ligne & coffre AES-256 SQLCipher. Zéro pub, zéro cloud.",
        "full_desc": """PureLock est une suite de confidentialité sans publicité et un coffre-fort chiffré 100% hors ligne conçu pour protéger vos applications, mots de passe, codes 2FA et notes privées via SQLCipher AES-256 et Android Keystore.

🔒 FONCTIONNALITÉS CLÉS :
• Bouclier d'Apps (App Shield) : Verrouillez toute application par PIN, Schéma (tracé invisible), Code Knock à 4 quadrants ou Biométrie, avec clavier aléatoire et délai de grâce.
• Coffre Chiffré Matériel : Stockez mots de passe, notes sécurisées, cartes et codes 2FA dans une base SQLCipher AES-256 avec corbeille auto-nettoyante et analyse d'entropie.
• Camouflage & Leurres : Déguisez l'écran de verrouillage en Calculatrice fonctionnelle ou faux plantage, plus un PIN de contrainte (Duress) d'urgence.
• Secouer pour Verrouiller : Verrouillage instantané par secousse avec sensibilité réglable (Haute / Moyenne / Basse).
• Capture d'Intrus & Audit SHA-256 : Enregistre les échecs localement et vérifie l'intégrité cryptographique SHA-256 de votre coffre.
• Effacement Auto du Presse-papiers & Générateur : Crée des mots de passe robustes hors ligne et purge automatiquement les secrets copiés.
• Saisie Automatique, Tuiles & Widget : Service Autofill hors ligne, tuiles rapides, widget d'écran d'accueil, sauvegardes chiffrées .plk et Android TV.

🛡️ CONFIDENTIALITÉ 100% LOCALE :
Sans cloud, sans compte et sans publicité. Vos données chiffrées ne quittent jamais votre appareil.

────────────────────────────────────────
UTILISATION DE L'API ACCESSIBILITYSERVICE ET DIVULGATION
────────────────────────────────────────
PureLock utilise l'API AccessibilityService uniquement pour fournir sa fonctionnalité principale de verrouillage d'applications (App Shield).

• Utilisation : PureLock surveille les changements d'état des fenêtres (TYPE_WINDOW_STATE_CHANGED) uniquement pour détecter l'ouverture d'une application protégée au premier plan et afficher instantanément l'écran de verrouillage sécurisé (PIN, Schéma, Knock ou Biométrie).
• Confidentialité : PureLock NE lit, NE capture et N'accède à aucun contenu d'écran, frappe de clavier, mot de passe ou message privé.
• Zéro collecte de données personnelles : Vos secrets restent strictement hors ligne sur votre appareil."""
    },
    {
        "locale": "de-DE",
        "language": "German (Germany)",
        "title": "PureLock: App Sperre & Tresor",
        "short_desc": "Offline App-Sperre & AES-256 SQLCipher Tresor. Ohne Werbung, ohne Cloud.",
        "full_desc": """PureLock ist eine werbefreie Datenschutz-Suite und ein verschlüsselter Offline-Tresor für Apps, Passwörter, 2FA-Codes und private Notizen — geschützt durch SQLCipher AES-256 und Android Keystore.

🔒 HAUPTFUNKTIONEN:
• App Shield (App-Sperre): Sichern Sie beliebige Apps per PIN, Muster (unsichtbarer Pfad), 4-Quadranten-Knock-Code oder Biometrie — inkl. Zufallstastatur und Kulanzzeit.
• Hardware-Verschlüsselter Tresor: Speichern Sie Passwörter, Notizen, Karten und 2FA-Codes in einer SQLCipher-AES-256-Datenbank mit Papierkorb und Entropie-Analyse.
• Tarnung & Köder: Tarnen Sie die Sperre als funktionierenden Taschenrechner oder Absturz-Dialog, plus Notfall-PIN (Duress) für Zwangssituationen.
• Schütteln zum Sperren: Sofortige Notfallsperre per Schüttelgeste mit einstellbarer Empfindlichkeit.
• Eindringlings-Schutz & SHA-256-Audit: Protokolliert Fehlversuche lokal und prüft die kryptografische Integrität Ihres Tresors.
• Zwischenablage-Auto-Löschung & Generator: Erstellt starke Passwörter offline und löscht kopierte Geheimnisse automatisch.
• Lokales Autofill, Kacheln & Widget: Offline-Autofill-Dienst, Schnelleinstellungen, Startbildschirm-Widget, verschlüsselte .plk-Backups und Android TV.

🛡️ 100% LOKALER DATENSCHUTZ:
Ohne Cloud-Konto und ohne Werbung. Ihre verschlüsselten Daten bleiben ausschließlich auf Ihrem Gerät.

────────────────────────────────────────
BARRIEREFREIHEITSDIENST (ACCESSIBILITY API) OFFENLEGUNG
────────────────────────────────────────
PureLock verwendet die AccessibilityService-API ausschließlich zur Bereitstellung der App-Sperrfunktion (App Shield).

• Verwendung: PureLock überwacht Fensterstatusänderungen (TYPE_WINDOW_STATE_CHANGED), um zu erkennen, wenn eine geschützte App im Vordergrund geöffnet wird, und zeigt sofort den sicheren Sperrbildschirm (PIN, Muster, Knock-Code oder Biometrie) an.
• Datenschutz: PureLock liest, erfasst oder speichert KEINE Bildschirminhalte, Tastatureingaben, Passwörter oder persönlichen Nachrichten.
• Keine Datenerfassung: Ihre Tresordaten bleiben vollständig offline auf Ihrem Gerät."""
    },
    {
        "locale": "it-IT",
        "language": "Italian (Italy)",
        "title": "PureLock: Blocco App e Vault",
        "short_desc": "Blocco app offline e cassaforte AES-256 SQLCipher. Zero pubblicità, zero cloud.",
        "full_desc": """PureLock è una suite di sicurezza senza pubblicità e una cassaforte crittografata 100% offline per proteggere app, password, codici 2FA e note private con SQLCipher AES-256 e Android Keystore.

🔒 FUNZIONALITÀ PRINCIPALI:
• App Shield (Blocco App): Proteggi qualsiasi app con PIN, Sequenza (traccia invisibile), Codice Knock a 4 quadranti o Biometria, con tastierino casuale e periodo di grazia.
• Cassaforte Crittografata Hardware: Salva password, note, carte e codici 2FA in SQLCipher AES-256 con cestino automatico e analisi dell'entropia.
• Camuffamento e Specchietti: Maschera il blocco come Calcolatrice funzionante o finto arresto anomalo, più PIN di coercizione (Duress).
• Scuoti per Bloccare: Blocco immediato d'emergenza scuotendo il telefono con sensibilità regolabile.
• Cattura Intrusi e Audit SHA-256: Registra i tentativi falliti in locale ed esegue verifiche crittografiche SHA-256 con esportazione CSV.
• Pulizia Appunti e Generatore Offline: Genera password sicure e cancella automaticamente i segreti copiati dagli appunti.
• Compilazione Automatica, Riquadri e Widget: Autofill locale, riquadri rapidi, widget, backup cifrati .plk e supporto Android TV.

🛡️ PRIVACY 100% LOCALE:
Nessun account cloud, nessuna pubblicità. Tutti i tuoi dati rimangono esclusivamente sul tuo dispositivo.

────────────────────────────────────────
USO E DIVULGAZIONE DELL'API ACCESSIBILITYSERVICE
────────────────────────────────────────
PureLock utilizza l'API AccessibilityService esclusivamente per fornire la sua funzione principale di Blocco App (App Shield).

• Utilizzo: PureLock monitora gli eventi di cambio stato della finestra (TYPE_WINDOW_STATE_CHANGED) unicamente per rilevare quando un'app protetta viene aperta in primo piano e mostrare immediatamente la schermata di blocco sicura.
• Privacy e Protezione Dati: PureLock NON legge, cattura o accede ai contenuti dello schermo, tasti digitati, password o messaggi personali."""
    },
    {
        "locale": "ru-RU",
        "language": "Russian (Russia)",
        "title": "PureLock: Блокировка и Сейф",
        "short_desc": "Офлайн блокировка и сейф AES-256 SQLCipher. Без рекламы и облака.",
        "full_desc": """PureLock — автономный комплекс безопасности без рекламы и зашифрованный сейф для защиты приложений, паролей, кодов 2FA и заметок на базе SQLCipher AES-256 и Android Keystore.

🔒 ОСНОВНЫЕ ВОЗМОЖНОСТИ:
• Защита приложений (App Shield): Блокировка любых приложений по PIN, графическому ключу (невидимый след), 4-квадрантному Knock-коду или биометрии, со случайной клавиатурой и периодом отсрочки.
• Аппаратный шифрованный сейф: Хранение паролей, заметок, карт и 2FA в базе SQLCipher AES-256 с корзиной и анализом энтропии паролей.
• Маскировка и приманки: Режим рабочего Калькулятора, ложный сбой приложения и экстренный PIN-код принуждения (Duress).
• Блокировка встряхиванием: Мгновенная защита при встряхивании с настройкой чувствительности.
• Фиксация взломщиков и аудит SHA-256: Локальный журнал безопасности, проверка целостности SHA-256 и экспорт в CSV.
• Очистка буфера и генератор паролей: Генерация стойких паролей офлайн и автоматическая очистка буфера обмена.
• Автозаполнение, плитки и виджет: Локальный Autofill, плитки быстрых настроек, виджет, шифрованные резервные копии .plk и поддержка Android TV.

🛡️ 100% ЛОКАЛЬНАЯ КОНФИДЕНЦИАЛЬНОСТЬ:
Без облачных аккаунтов и рекламы. Все зашифрованные данные хранятся строго на вашем устройстве.

────────────────────────────────────────
ИСПОЛЬЗОВАНИЕ API ACCESSIBILITYSERVICE
────────────────────────────────────────
PureLock использует AccessibilityService API исключительно для основной функции блокировки приложений (App Shield).

• Назначение: Отслеживаются только события смены окна (TYPE_WINDOW_STATE_CHANGED) для определения запуска защищенного приложения и мгновенного показа экрана блокировки.
• Конфиденциальность: PureLock НЕ читает, НЕ собирает и НЕ перехватывает содержимое экрана, нажатия клавиш, пароли или личные сообщения."""
    },
    {
        "locale": "ja-JP",
        "language": "Japanese (Japan)",
        "title": "PureLock: アプリロック＆保管庫",
        "short_desc": "完全オフラインのアプリロック＆AES-256 SQLCipher保管庫。広告なし。",
        "full_desc": """PureLockは、SQLCipher AES-256とAndroid Keystoreのハードウェア暗号化でアプリ・パスワード・2FAコード・秘密メモを端末内で保護する、広告なしのオフラインセキュリティ＆暗号化保管庫アプリです。

🔒 主な機能:
• アプリロック（App Shield）: PIN、パターン（軌跡非表示）、4分割ノックコード、生体認証で保護。ランダムキーパッドや再ロック猶予時間にも対応。
• ハードウェア暗号化保管庫: パスワード、メモ、カード、2FAコードをSQLCipher AES-256で保存。ゴミ箱自動削除とエントロピー強度分析を搭載。
• カモフラージュ＆おとり機能: 電卓ロック、偽クラッシュ画面、緊急用デュレスPINに対応。
• シェイクでロック: 感度調整可能な加速度センサーによる緊急ロックダウン。
• 侵入者記録＆SHA-256監査: 解除失敗のローカル記録、SHA-256整合性チェック、CSVエクスポート。
• クリップボード自動消去＆パスワード生成: 高強度パスワードのローカル生成とコピー後の自動消去。
• ローカル自動入力・クイックタイル・ウィジェット・Android TV対応。

🛡️ 100%ローカルプライバシー保証:
クラウド同期・アカウント・広告は一切なく、すべての暗号化データは端末内にのみ保存されます。

────────────────────────────────────────
ユーザー補助（ACCESSIBILITY SERVICE）APIの利用開示
────────────────────────────────────────
PureLockは、コア機能である「アプリロック（App Shield）」を提供するためにのみAccessibilityService APIを使用します。

・使用目的: 保護対象アプリがフォアグラウンドで起動されたことを検知（TYPE_WINDOW_STATE_CHANGED）し、直ちに安全なロック画面を表示します。
・プライバシー保護: 画面上のテキスト、キー入力、パスワード、個人メッセージの閲覧・収集・送信は一切行いません。"""
    },
    {
        "locale": "ko-KR",
        "language": "Korean (South Korea)",
        "title": "PureLock: 앱 잠금 및 금고",
        "short_desc": "오프라인 앱 잠금 및 AES-256 SQLCipher 금고. 광고 없음, 완벽한 프라이버시.",
        "full_desc": """PureLock은 SQLCipher AES-256 및 Android Keystore 하드웨어 암호화로 앱, 비밀번호, 2FA 코드, 비공개 메모를 기기 내부에서만 안전하게 보호하는 무광고 오프라인 보안 앱입니다.

🔒 주요 기능:
• 앱 잠금(App Shield): PIN, 패턴(경로 숨김), 4분할 노크 코드, 생체 인증으로 모든 앱 보호. 랜덤 키패드 및 재잠금 유예 시간 지원.
• 하드웨어 암호화 금고: 비밀번호, 보안 메모, 카드, 2FA 코드를 SQLCipher AES-256에 저장. 휴지통 자동 삭제 및 엔트로피 분석 제공.
• 위장 및 미끼 화면: 작동하는 계산기 위장, 가짜 오류 화면, 강요 상황용 비상 Duress PIN 지원.
• 흔들어서 잠금: 민감도 조절이 가능한 즉시 비상 잠금 제스처.
• 침입자 기록 및 SHA-256 무결성 감사: 실패 시도 로컬 기록, SHA-256 무결성 검사, CSV 내보내기.
• 클립보드 자동 삭제 및 비밀번호 생성기: 오프라인 고강도 비밀번호 생성과 복사 후 자동 삭제.
• 로컬 자동 완성, 빠른 설정 타일, 홈 위젯, 암호화 .plk 백업 및 Android TV 지원.

🛡️ 100% 로컬 개인정보 보호:
클라우드 계정과 광고 없이 모든 암호화 데이터는 사용자 기기 내부에만 보관됩니다.

────────────────────────────────────────
접근성(ACCESSIBILITY SERVICE) API 사용 및 고지
────────────────────────────────────────
PureLock은 핵심 기능인 '앱 잠금(App Shield)' 제공을 위해서만 AccessibilityService API를 사용합니다.

• 사용 목적: 보호 앱의 포그라운드 실행 감지(TYPE_WINDOW_STATE_CHANGED) 후 즉시 보안 잠금 화면을 표시합니다.
• 개인정보 보호: 화면 내용, 키 입력, 비밀번호 또는 개인 메시지를 읽거나 수집·전송하지 않습니다."""
    },
    {
        "locale": "zh-CN",
        "language": "Chinese (Simplified)",
        "title": "PureLock: 应用锁与隐私保险箱",
        "short_desc": "离线应用锁与 AES-256 SQLCipher 保险箱。无广告、无云端，极致隐私。",
        "full_desc": """PureLock 是一款无广告、完全离线的隐私应用锁与加密保险箱，依托 SQLCipher AES-256 与 Android Keystore 硬件密钥保护您的应用、密码、2FA 验证码与私密笔记。

🔒 核心功能：
• 应用锁（App Shield）：支持 PIN 码、九宫格图案（隐藏轨迹）、四象限敲击密码与生物识别，支持随机键盘与重新锁定宽限期。
• 硬件加密保险箱：以 SQLCipher AES-256 本地存储密码、安全笔记、卡片与 2FA，内置回收站自动清理与熵值强度分析。
• 伪装与诱饵模式：支持可正常运算的计算器伪装、模拟崩溃弹窗以及紧急胁迫 PIN 码。
• 摇一摇锁屏：灵敏度可调的加速度传感器紧急一键锁定。
• 入侵记录与 SHA-256 完整性审计：本地记录失败尝试、执行 SHA-256 加密校验并支持 CSV 导出。
• 剪贴板自动清除与密码生成器：离线生成高强度密码，复制后自动清空剪贴板。
• 本地自动填充、快捷磁贴、桌面小组件、加密 .plk 备份与 Android TV 支持。

🛡️ 100% 本地隐私保障：
无云端账号、无广告。您的加密数据始终严格保存在设备本地。

────────────────────────────────────────
无障碍服务（ACCESSIBILITY SERVICE）API 使用声明
────────────────────────────────────────
PureLock 仅使用 AccessibilityService API 提供核心“应用锁（App Shield）”功能。

• 使用方式：仅监听窗口状态变更事件（TYPE_WINDOW_STATE_CHANGED），在受保护应用进入前台时立即显示安全锁屏。
• 隐私保护：绝不读取、采集或上传屏幕内容、按键输入、密码或个人消息。"""
    },
    {
        "locale": "zh-TW",
        "language": "Chinese (Traditional)",
        "title": "PureLock: 應用程式鎖與保險箱",
        "short_desc": "離線應用程式鎖與 AES-256 SQLCipher 保險箱。無廣告、無雲端，極致隱私。",
        "full_desc": """PureLock 是一款無廣告、完全離線的隱私應用程式鎖與加密保險箱，採用 SQLCipher AES-256 與 Android Keystore 硬體金鑰保護您的應用程式、密碼、2FA 驗證碼與私密筆記。

🔒 核心功能：
• 應用程式鎖（App Shield）：支援 PIN 碼、圖形（隱藏軌跡）、四象限敲擊密碼與生物辨識，並支援隨機鍵盤與寬限時間。
• 硬體加密保險箱：使用 SQLCipher AES-256 儲存密碼、筆記、卡片與 2FA，內建垃圾桶自動清除與熵值強度分析。
• 偽裝與誘餌模式：支援可運作的計算機偽裝、假當機對話框以及緊急脅迫 PIN 碼。
• 搖一搖鎖定：靈敏度可調的緊急快速鎖定手勢。
• 入侵紀錄與 SHA-256 稽核：本機記錄解鎖失敗、執行 SHA-256 完整性驗證並支援 CSV 匯出。
• 剪貼簿自動清除與密碼產生器：離線產生高強度密碼，複製後自動清空剪貼簿。
• 本機自動填入、快速設定方塊、桌面小工具、加密 .plk 備份與 Android TV 支援。

🛡️ 100% 本機隱私保障：
無雲端帳號、無廣告。所有加密資料皆僅保留於您的裝置本機。

────────────────────────────────────────
無障礙服務（ACCESSIBILITY SERVICE）API 使用聲明
────────────────────────────────────────
PureLock 僅使用 AccessibilityService API 提供核心「應用程式鎖（App Shield）」功能。

• 使用方式：僅監聽視窗狀態變更事件（TYPE_WINDOW_STATE_CHANGED），於受保護應用程式開啟時立即顯示安全鎖定畫面。
• 隱私保護：絕不讀取、擷取或傳送螢幕內容、按鍵輸入、密碼或私人訊息。"""
    },
    {
        "locale": "vi",
        "language": "Vietnamese",
        "title": "PureLock: Khóa ứng dụng",
        "short_desc": "Khóa ứng dụng offline & két AES-256 SQLCipher. Không quảng cáo, không đám mây.",
        "full_desc": """PureLock là bộ công cụ bảo mật không quảng cáo và két mã hóa 100% offline giúp bảo vệ ứng dụng, mật khẩu, mã 2FA và ghi chú riêng tư trên thiết bị với SQLCipher AES-256 và Android Keystore.

🔒 TÍNH NĂNG NỔI BẬT:
• Lá Chắn Ứng Dụng (App Shield): Khóa ứng dụng bằng PIN, Hình vẽ (ẩn đường vẽ), Mã gõ 4 vùng (Knock Code) hoặc Sinh trắc học, kèm bàn phím xáo trộn và thời gian ân hạn khóa lại.
• Két Mã Hóa Phần Cứng: Lưu mật khẩu, ghi chú, thẻ và mã 2FA trong cơ sở dữ liệu SQLCipher AES-256 với Thùng rác tự dọn và Phân tích độ mạnh mật khẩu.
• Ngụy Trang & Mồi Nhử: Màn hình Máy tính hoạt động thật, hộp thoại Lỗi ứng dụng giả và mã PIN khẩn cấp (Duress PIN).
• Lắc Để Khóa: Khóa khẩn cấp tức thì khi lắc điện thoại với 3 mức độ nhạy tùy chỉnh.
• Ghi Nhận Kẻ Xâm Nhập & Kiểm Toán SHA-256: Lưu lịch sử mở khóa sai cục bộ, kiểm tra toàn vẹn mật mã SHA-256 và xuất nhật ký CSV.
• Tự Xóa Bộ Nhớ Tạm & Tạo Mật Khẩu Offline: Tạo mật khẩu mạnh và tự động xóa bí mật đã sao chép khỏi clipboard.
• Tự Điền Cục Bộ, Ô Cài Đặt Nhanh, Widget & Android TV: Hỗ trợ Autofill offline, sao lưu mã hóa .plk và điều khiển TV.

🛡️ CAM KẾT RIÊNG TƯ 100% CỤC BỘ:
Không tài khoản đám mây, không quảng cáo. Mọi dữ liệu mã hóa luôn nằm trọn trên thiết bị của bạn.

────────────────────────────────────────
CÔNG KHAI SỬ DỤNG ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock sử dụng AccessibilityService API nghiêm ngặt chỉ nhằm phục vụ tính năng Khóa Ứng Dụng (App Shield).

• Mục đích sử dụng: PureLock chỉ theo dõi sự kiện thay đổi cửa sổ (TYPE_WINDOW_STATE_CHANGED) để nhận biết khi ứng dụng được bảo vệ mở lên và hiển thị ngay màn hình khóa an toàn (PIN, Hình vẽ, Knock Code hoặc Sinh trắc học).
• Bảo vệ quyền riêng tư: PureLock KHÔNG đọc, chụp hoặc truy cập nội dung màn hình, thao tác bàn phím, mật khẩu hoặc tin nhắn cá nhân."""
    },
    {
        "locale": "id",
        "language": "Indonesian",
        "title": "PureLock: Kunci Aplikasi",
        "short_desc": "Kunci aplikasi offline & brankas AES-256 SQLCipher. Tanpa iklan, tanpa cloud.",
        "full_desc": """PureLock adalah aplikasi keamanan bebas iklan dan brankas terenkripsi 100% offline untuk melindungi aplikasi, kata sandi, kode 2FA, dan catatan pribadi dengan SQLCipher AES-256 dan Android Keystore.

🔒 FITUR UTAMA:
• App Shield (Kunci Aplikasi): Lindungi aplikasi dengan PIN, Pola (jejak tak terlihat), Kode Ketuk 4-Kuadran, atau Biometrik, plus keypad acak dan masa tenggang kunci ulang.
• Brankas Enkripsi Perangkat Keras: Simpan kata sandi, catatan, kartu, dan 2FA di SQLCipher AES-256 dengan tempat sampah otomatis dan wawasan entropi.
• Penyamaran & Umpan: Samarkan layar kunci sebagai Kalkulator aktif atau dialog Crash Palsu, plus PIN Darurat (Duress).
• Goyangkan untuk Mengunci: Kunci darurat seketika dengan sensitivitas akselerometer yang dapat diatur.
• Catatan Penyusup & Audit SHA-256: Merekam percobaan gagal secara lokal dan memverifikasi integritas kriptografi SHA-256.
• Pembersih Clipboard & Generator Offline: Buat kata sandi kuat dan hapus otomatis rahasia yang disalin.
• Autofill Lokal, Ubin Cepat, Widget & Android TV: Mendukung cadangan terenkripsi .plk dan navigasi TV.

🛡️ JAMINAN PRIVASI 100% LOKAL:
Tanpa akun cloud dan tanpa iklan. Semua data terenkripsi tetap berada di perangkat Anda.

────────────────────────────────────────
PENGGUNAAN & PENGUNGKAPAN ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock menggunakan AccessibilityService API semata-mata untuk fitur utama Pengunci Aplikasi (App Shield).

• Cara Penggunaan: Memantau perubahan status jendela (TYPE_WINDOW_STATE_CHANGED) untuk mendeteksi pembukaan aplikasi terlindungi dan segera menampilkan layar kunci aman.
• Privasi Data: PureLock TIDAK membaca, merekam, atau mengakses konten layar, ketikan, kata sandi, atau pesan pribadi."""
    },
    {
        "locale": "hi-IN",
        "language": "Hindi",
        "title": "PureLock: ऐप लॉक और वॉल्ट",
        "short_desc": "ऑफ़लाइन ऐप लॉक व AES-256 SQLCipher वॉल्ट। बिना विज्ञापन, पूर्ण गोपनीयता।",
        "full_desc": """PureLock एक विज्ञापन-मुक्त 100% ऑफ़लाइन सुरक्षा ऐप और एन्क्रिप्टेड वॉल्ट है जो SQLCipher AES-256 और Android Keystore से आपके ऐप्स, पासवर्ड, 2FA कोड और निजी नोट्स की रक्षा करता है।

🔒 मुख्य विशेषताएँ:
• App Shield (ऐप लॉक): PIN, पैटर्न (अदृश्य रेखा), 4-क्वाड्रंट नॉक कोड या बायोमेट्रिक से ऐप लॉक करें; रैंडम कीपैड और री-लॉक ग्रेस पीरियड सहित।
• हार्डवेयर-एन्क्रिप्टेड वॉल्ट: SQLCipher AES-256 में पासवर्ड, नोट्स, कार्ड और 2FA सुरक्षित रखें, ऑटो-पर्ज ट्रैश व एन्ट्रॉपी विश्लेषण के साथ।
• कैमोफ़्लाज व डिकॉय: कैलकुलेटर लॉक, नकली क्रैश डायलॉग और आपातकालीन ड्यूरेस PIN।
• हिलाकर लॉक करें: एडजस्टेबल सेंसर संवेदनशीलता के साथ तुरंत लॉकडाउन।
• घुसपैठिया रिकॉर्ड व SHA-256 ऑडिट: असफल प्रयासों का स्थानीय लॉग, SHA-256 अखंडता जाँच और CSV एक्सपोर्ट।
• क्लिपबोर्ड ऑटो-वाइप व पासवर्ड जनरेटर: मजबूत पासवर्ड बनाएं और कॉपी किए गए सीक्रेट स्वतः साफ करें।
• लोकल ऑटोफ़िल, क्विक टाइल्स, विजेट और Android TV समर्थन।

🛡️ 100% स्थानीय गोपनीयता गारंटी:
कोई क्लाउड खाता नहीं, कोई विज्ञापन नहीं। आपका डेटा केवल आपके डिवाइस पर रहता है।

────────────────────────────────────────
ACCESSIBILITY SERVICE API उपयोग और प्रकटीकरण
────────────────────────────────────────
PureLock केवल अपने मुख्य ऐप लॉक (App Shield) फीचर के लिए AccessibilityService API का उपयोग करता है।

• उद्देश्य: सुरक्षित ऐप खुलने का पता लगाने हेतु केवल विंडो परिवर्तन (TYPE_WINDOW_STATE_CHANGED) की निगरानी करता है और तुरंत लॉक स्क्रीन दिखाता है।
• गोपनीयता: PureLock स्क्रीन सामग्री, कीस्ट्रोक्स, पासवर्ड या निजी संदेशों को कभी नहीं पढ़ता या एकत्र नहीं करता।"""
    },
    {
        "locale": "tr-TR",
        "language": "Turkish (Turkey)",
        "title": "PureLock: Uygulama Kilidi",
        "short_desc": "Çevrimdışı uygulama kilidi ve AES-256 SQLCipher kasa. Reklamsız, tam gizlilik.",
        "full_desc": """PureLock, uygulamalarınızı, şifrelerinizi, 2FA kodlarınızı ve özel notlarınızı SQLCipher AES-256 ve Android Keystore ile cihazınızda koruyan reklamsız, %100 çevrimdışı güvenlik kasasıdır.

🔒 TEMEL ÖZELLİKLER:
• App Shield (Uygulama Kilidi): PIN, Desen (görünmez iz), 4 Bölgeli Knock Kodu veya Biyometri ile koruma; rastgele tuş takımı ve yeniden kilitleme gecikmesi.
• Donanım Şifreli Kasa: Parola, not, kart ve 2FA kodlarını SQLCipher AES-256 ile saklayın; otomatik çöp kutusu ve entropi analizi.
• Kamuflaj ve Tuzaklar: Çalışan Hesap Makinesi kilidi, Sahte Çökme ekranı ve acil durum Baskı PIN'i (Duress).
• Kilitlemek İçin Salla: Ayarlanabilir hassasiyetle anında acil kilitleme.
• İzinsiz Giriş Kaydı ve SHA-256 Denetimi: Başarısız denemeleri yerel kaydeder, SHA-256 bütünlük doğrulaması ve CSV dışa aktarım sunar.
• Pano Otomatik Temizleme ve Şifre Üretici: Çevrimdışı güçlü parola oluşturur ve kopyalanan sırları panodan otomatik siler.
• Yerel Otomatik Doldurma, Hızlı Ayarlar, Widget ve Android TV desteği.

🛡️ %100 YEREL GİZLİLİK:
Bulut hesabı yok, reklam yok. Şifreli verileriniz yalnızca cihazınızda kalır.

────────────────────────────────────────
ACCESSIBILITY SERVICE API KULLANIMI VE AÇIKLAMASI
────────────────────────────────────────
PureLock, AccessibilityService API'sini yalnızca temel Uygulama Kilidi (App Shield) özelliği için kullanır.

• Kullanım: Korunan bir uygulamanın açıldığını algılamak ve güvenli kilit ekranını göstermek için yalnızca pencere durumu değişikliklerini (TYPE_WINDOW_STATE_CHANGED) izler.
• Gizlilik: Ekran içeriğini, tuş vuruşlarını, parolaları veya kişisel mesajları ASLA okumaz, kaydetmez veya aktarmaz."""
    },
    {
        "locale": "th",
        "language": "Thai",
        "title": "PureLock: ล็อกแอปและตู้เซฟ",
        "short_desc": "ล็อกแอปออฟไลน์ & ตู้เซฟ AES-256 SQLCipher ไร้โฆษณา ไร้คลาวด์ เป็นส่วนตัวสูงสุด",
        "full_desc": """PureLock คือชุดความปลอดภัยไร้โฆษณาและตู้เซฟเข้ารหัสออฟไลน์ 100% ปกป้องแอป รหัสผ่าน รหัส 2FA และโน้ตส่วนตัวด้วย SQLCipher AES-256 และ Android Keystore บนเครื่องของคุณ

🔒 ฟีเจอร์หลัก:
• ล็อกแอป (App Shield): ป้องกันแอปด้วย PIN, รูปแบบ (ซ่อนเส้น), รหัสเคาะ 4 ช่อง (Knock Code) หรือไบโอเมตริกซ์ พร้อมแป้นตัวเลขสุ่มและเวลาหน่วงล็อกซ้ำ
• ตู้เซฟเข้ารหัสฮาร์ดแวร์: เก็บรหัสผ่าน โน้ต บัตร และ 2FA ใน SQLCipher AES-256 พร้อมถังขยะล้างอัตโนมัติและวิเคราะห์ความแข็งแกร่งรหัสผ่าน
• โหมดพรางตัวและตัวลวง: หน้าจอเครื่องคิดเลขใช้งานได้จริง หน้าต่างแอปหยุดทำงานปลอม และ PIN ฉุกเฉิน (Duress)
• เขย่าเพื่อล็อก: ล็อกฉุกเฉินทันทีเมื่อเขย่าเครื่อง ปรับความไวได้ 3 ระดับ
• บันทึกผู้บุกรุกและตรวจสอบ SHA-256: บันทึกการปลดล็อกผิดในเครื่อง ตรวจสอบความสมบูรณ์ SHA-256 และส่งออก CSV
• ล้างคลิปบอร์ดอัตโนมัติและสร้างรหัสผ่านออฟไลน์: สร้างรหัสผ่านปลอดภัยและลบข้อมูลที่คัดลอกอัตโนมัติ
• ป้อนอัตโนมัติในเครื่อง ปุ่มตั้งค่าด่วน วิดเจ็ต สำรองข้อมูล .plk เข้ารหัส และรองรับ Android TV

🛡️ ความเป็นส่วนตัวในเครื่อง 100%:
ไม่มีบัญชีคลาวด์ ไม่มีโฆษณา ข้อมูลเข้ารหัสทั้งหมดอยู่บนอุปกรณ์ของคุณเท่านั้น

────────────────────────────────────────
การเปิดเผยการใช้งาน ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock ใช้ AccessibilityService API เพื่อฟีเจอร์ล็อกแอป (App Shield) เท่านั้น

• วัตถุประสงค์: ตรวจจับการเปลี่ยนหน้าต่าง (TYPE_WINDOW_STATE_CHANGED) เพื่อรู้เมื่อแอปที่ปกป้องเปิดขึ้นและแสดงหน้าจอล็อกทันที
• ความเป็นส่วนตัว: ไม่อ่าน บันทึก หรือเข้าถึงเนื้อหาหน้าจอ การพิมพ์ รหัสผ่าน หรือข้อความส่วนตัวใดๆ"""
    },
    {
        "locale": "ar",
        "language": "Arabic",
        "title": "PureLock: قفل التطبيقات وخزنة",
        "short_desc": "قفل تطبيقات دون إنترنت وخزنة AES-256 SQLCipher. بلا إعلانات، خصوصية تامة.",
        "full_desc": """PureLock هو تطبيق أمان خالٍ من الإعلانات وخزنة مشفرة تعمل بنسبة 100% دون اتصال بالإنترنت لحماية تطبيقاتك وكلمات مرورك ورموز 2FA وملاحظاتك الخاصة باستخدام SQLCipher AES-256 و Android Keystore.

🔒 الميزات الرئيسية:
• درع التطبيقات (App Shield): اقفل أي تطبيق برمز PIN أو نمط (بمسار مخفي) أو رمز النقر الرباعي (Knock Code) أو البصمة، مع لوحة أرقام عشوائية وفترة سماح لإعادة القفل.
• خزنة مشفرة بالأجهزة: احفظ كلمات المرور والملاحظات والبطاقات ورموز 2FA بقاعدة بيانات SQLCipher AES-256 مع سلة محذوفات تلقائية وتحليل قوة كلمات المرور.
• التمويه والشاشات الوهمية: تمويه شاشة القفل كآلة حاسبة حقيقية أو تنبيه تعطل وهمي، مع رمز PIN للطوارئ والإكراه (Duress).
• الهز للقفل: قفل فوري للطوارئ عند هز الجهاز بحساسية قابلة للتعديل.
• سجل الدخلاء وتدقيق SHA-256: تسجيل المحاولات الفاشلة محلياً مع فحص سلامة الخزنة SHA-256 وتصدير CSV.
• مسح الحافظة التلقائي ومولد كلمات المرور: إنشاء كلمات مرور قوية محلياً ومسح الأسرار المنسوخة تلقائياً.
• الملء التلقائي المحلي، مربعات الإعدادات السريعة، ويدجت الشاشة، نسخ احتياطي مشفر .plk ودعم Android TV.

🛡️ خصوصية محلية 100%:
بلا حسابات سحابية وبلا إعلانات. تبقى جميع بياناتك المشفرة حصراً داخل جهازك.

────────────────────────────────────────
إفصاح استخدام واجهة ACCESSIBILITY SERVICE API
────────────────────────────────────────
يستخدم PureLock واجهة AccessibilityService API حصرياً لتوفير ميزة قفل التطبيقات الرئيسية (App Shield).

• آلية الاستخدام: يراقب PureLock أحداث تغيير حالة النوافذ (TYPE_WINDOW_STATE_CHANGED) فقط لاكتشاف فتح تطبيق محمي وعرض شاشة القفل الآمنة فوراً.
• حماية الخصوصية: لا يقرأ PureLock أو يسجل أو يصل إلى محتوى الشاشة أو ضغطات المفاتيح أو كلمات المرور أو الرسائل الشخصية."""
    },
    {
        "locale": "nl-NL",
        "language": "Dutch (Netherlands)",
        "title": "PureLock: App-vergrendeling",
        "short_desc": "Offline app-slot & AES-256 SQLCipher-kluis. Geen advertenties, geen cloud.",
        "full_desc": """PureLock is een advertentievrije privacysuite en 100% offline versleutelde kluis voor je apps, wachtwoorden, 2FA-codes en privénotities — beveiligd met SQLCipher AES-256 en Android Keystore.

🔒 BELANGRIJKSTE FUNCTIES:
• App Shield: Vergrendel elke app met PIN, Patroon (onzichtbaar spoor), 4-kwadrant Knock Code of Biometrie, inclusief willekeurig toetsenbord en hervergrendel-pauze.
• Hardware-versleutelde Kluis: Bewaar wachtwoorden, notities, kaarten en 2FA in SQLCipher AES-256 met prullenbak en entropie-inzichten.
• Camouflage & Lokschermen: Werkende Rekenmachine-vermomming, nep-crashdialoog en een nood-PIN (Duress).
• Schudden om te Vergrendelen: Directe noodvergrendeling met instelbare gevoeligheid.
• Indringersregistratie & SHA-256-audit: Lokale beveiligingslogs, SHA-256 integriteitscontrole en CSV-export.
• Klembord Auto-Wissen & Wachtwoordgenerator: Genereer sterke wachtwoorden offline en wis gekopieerde geheimen automatisch.
• Lokaal Automatisch Invullen, Snelle Tegels, Widget, versleutelde .plk-back-ups en Android TV-ondersteuning.

🛡️ 100% LOKALE PRIVACY:
Geen cloudaccounts en geen advertenties. Je versleutelde gegevens verlaten nooit je apparaat.

────────────────────────────────────────
TOELICHTING ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock gebruikt de AccessibilityService API uitsluitend voor de kernfunctie App-vergrendeling (App Shield).

• Gebruik: PureLock controleert vensterstatuswijzigingen (TYPE_WINDOW_STATE_CHANGED) uitsluitend om te detecteren wanneer een beveiligde app wordt geopend en direct het vergrendelscherm te tonen.
• Privacy: PureLock leest, registreert of opent NOOIT scherminhoud, toetsaanslagen, wachtwoorden of persoonlijke berichten."""
    },
    {
        "locale": "pl-PL",
        "language": "Polish (Poland)",
        "title": "PureLock: Blokada Aplikacji",
        "short_desc": "Blokada aplikacji offline i sejf AES-256 SQLCipher. Bez reklam i chmury.",
        "full_desc": """PureLock to wolny od reklam, w 100% offline pakiet prywatności i zaszyfrowany sejf chroniący aplikacje, hasła, kody 2FA i notatki za pomocą SQLCipher AES-256 i Android Keystore.

🔒 GŁÓWNE FUNKCJE:
• App Shield (Blokada Aplikacji): Zabezpiecz aplikacje kodem PIN, Wzorem (niewidoczna ścieżka), Kodem Knock (4 ćwiartki) lub Biometrią, z losową klawiaturą i czasem karencji.
• Sejf Szyfrowany Sprzętowo: Przechowuj hasła, notatki, karty i kody 2FA w bazie SQLCipher AES-256 z automatycznym koszem i analizą entropii.
• Kamuflaż i Przynęty: Działający Kalkulator, fałszywy błąd aplikacji oraz awaryjny PIN przymusu (Duress).
• Potrząśnij, aby Zablokować: Natychmiastowa blokada awaryjna z regulacją czułości akcelerometru.
• Rejestr Intruzów i Audyt SHA-256: Lokalne logi bezpieczeństwa, kryptograficzny audyt integralności SHA-256 i eksport CSV.
• Auto-czyszczenie Schowka i Generator Haseł: Twórz silne hasła offline i automatycznie usuwaj skopiowane sekrety ze schowka.
• Lokalne Autouzupełnianie, Szybkie Kafelki, Widżet, szyfrowane kopie .plk i obsługa Android TV.

🛡️ 100% LOKALNEJ PRYWATNOŚCI:
Bez kont w chmurze i bez reklam. Twoje dane pozostają wyłącznie na Twoim urządzeniu.

────────────────────────────────────────
UJAWNIENIE UŻYCIA ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock korzysta z AccessibilityService API wyłącznie w celu zapewnienia głównej funkcji Blokady Aplikacji (App Shield).

• Zastosowanie: Monitoruje zdarzenia zmiany okna (TYPE_WINDOW_STATE_CHANGED) wyłącznie w celu wykrycia uruchomienia chronionej aplikacji i natychmiastowego wyświetlenia ekranu blokady.
• Prywatność: PureLock NIE odczytuje, NIE przechwytuje i NIE uzyskuje dostępu do zawartości ekranu, naciśnięć klawiszy, haseł ani prywatnych wiadomości."""
    },
    {
        "locale": "uk",
        "language": "Ukrainian",
        "title": "PureLock: Блокування і Сейф",
        "short_desc": "Офлайн-блокування додатків і сейф AES-256 SQLCipher. Без реклами та хмари.",
        "full_desc": """PureLock — це автономний комплекс приватності без реклами та зашифрований сейф для захисту додатків, паролів, кодів 2FA і нотаток на базі SQLCipher AES-256 та Android Keystore.

🔒 ОСНОВНІ МОЖЛИВОСТІ:
• App Shield (Блокування додатків): Захист додатків PIN-кодом, графічним ключем (невидимий слід), 4-квадрантним Knock-кодом або біометрією, з випадковою клавіатурою та періодом відтермінування.
• Апаратно зашифрований сейф: Зберігання паролів, нотаток, карток і 2FA у базі SQLCipher AES-256 з кошиком і аналізом ентропії.
• Маскування та приманки: Режим робочого Калькулятора, фальшивий збій додатку та екстрений PIN примусу (Duress).
• Струснути для блокування: Миттєве аварійне блокування з налаштуванням чутливості.
• Фіксація зловмисників і аудит SHA-256: Локальний журнал безпеки, перевірка цілісності SHA-256 та експорт CSV.
• Автоочищення буфера і генератор паролів: Створення надійних паролів офлайн та автоматичне очищення буфера обміну.
• Локальне автозаповнення, швидкі плитки, віджет, шифровані бекапи .plk і підтримка Android TV.

🛡️ 100% ЛОКАЛЬНА ПРИВАТНІСТЬ:
Без хмарних акаунтів і реклами. Усі зашифровані дані залишаються виключно на вашому пристрої.

────────────────────────────────────────
РОЗКРИТТЯ ВИКОРИСТАННЯ ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock використовує AccessibilityService API виключно для основної функції блокування додатків (App Shield).

• Призначення: Відстежує лише події зміни вікна (TYPE_WINDOW_STATE_CHANGED) для виявлення запуску захищеного додатку та миттєвого показу екрана блокування.
• Приватність: PureLock НЕ читає, НЕ перехоплює та НЕ збирає вміст екрана, натискання клавіш, паролі чи особисті повідомлення."""
    },
    {
        "locale": "sv-SE",
        "language": "Swedish (Sweden)",
        "title": "PureLock: Applås & Valv",
        "short_desc": "Offline applås & AES-256 SQLCipher-valv. Ingen reklam, inget moln.",
        "full_desc": """PureLock är en reklamfri integritetssvit och ett 100% offline krypterat valv för appar, lösenord, 2FA-koder och anteckningar — skyddat med SQLCipher AES-256 och Android Keystore.

🔒 HUVUDFUNKTIONER:
• App Shield (Applås): Lås appar med PIN, Mönster (osynligt spår), 4-kvadrant Knock-kod eller Biometri, med slumpad knappsats och nådatid.
• Hårdvarukrypterat Valv: Lagra lösenord, anteckningar, kort och 2FA i SQLCipher AES-256 med papperskorg och entropianalys.
• Kamouflage & Lockbeten: Fungerande Miniräknare, falsk kraschdialog och nöd-PIN (Duress).
• Skaka för att Låsa: Omedelbar nödlåsning med justerbar känslighet.
• Inkräktarlogg & SHA-256-granskning: Lokala säkerhetsloggar, SHA-256 integritetskontroll och CSV-export.
• Auto-rensning av Urklipp & Lösenordsgenerator: Skapa starka lösenord offline och rensa kopierade hemligheter automatiskt.
• Lokal Autofyll, Snabbpaneler, Widget, krypterade .plk-säkerhetskopior och Android TV.

🛡️ 100% LOKAL INTEGRITET:
Inga molnkonton och ingen reklam. All krypterad data stannar på din enhet.

────────────────────────────────────────
ANVÄNDNING AV ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock använder AccessibilityService API enbart för kärnfunktionen Applås (App Shield).

• Användning: Övervakar fönsterändringar (TYPE_WINDOW_STATE_CHANGED) enbart för att upptäcka när en skyddad app öppnas och direkt visa låsskärmen.
• Integritet: PureLock läser, sparar eller överför ALDRIG skärminnehåll, tangenttryckningar, lösenord eller privata meddelanden."""
    },
    {
        "locale": "da-DK",
        "language": "Danish (Denmark)",
        "title": "PureLock: Applås & Boks",
        "short_desc": "Offline applås & AES-256 SQLCipher-boks. Ingen reklamer, ingen sky.",
        "full_desc": """PureLock er en reklamefri privatlivssuite og 100% offline krypteret boks til apps, adgangskoder, 2FA-koder og private noter — beskyttet med SQLCipher AES-256 og Android Keystore.

🔒 NØGLEFUNKTIONER:
• App Shield (Applås): Lås apps med PIN, Mønster (usynligt spor), 4-kvadrant Knock-kode eller Biometri, med tilfældigt tastatur og henstandstid.
• Hardwarekrypteret Boks: Gem adgangskoder, noter, kort og 2FA i SQLCipher AES-256 med papirkurv og entropianalyse.
• Camouflage & Lokkeskærme: Fungerende Lommeregner, falsk nedbrudsdialog og nød-PIN (Duress).
• Ryst for at Låse: Øjeblikkelig nødlåsning med justerbar følsomhed.
• Ubuden Gæst-log & SHA-256-revision: Lokale sikkerhedslogs, SHA-256 integritetstjek og CSV-eksport.
• Auto-ryd Udklipsholder & Adgangskodegenerator: Opret stærke koder offline og ryd kopierede hemmeligheder automatisk.
• Lokal Autofyld, Hurtigfelter, Widget, krypterede .plk-backups og Android TV.

🛡️ 100% LOKALT PRIVATLIV:
Ingen skykonti og ingen reklamer. Dine krypterede data forbliver udelukkende på din enhed.

────────────────────────────────────────
OPLYSNING OM ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock bruger udelukkende AccessibilityService API til kernefunktionen Applås (App Shield).

• Brug: Overvåger vinduesændringer (TYPE_WINDOW_STATE_CHANGED) udelukkende for at opdage, når en beskyttet app åbnes, og straks vise låseskærmen.
• Privatliv: PureLock læser, gemmer eller tilgår ALDRIG skærmindhold, tastetryk, adgangskoder eller private beskeder."""
    },
    {
        "locale": "fi-FI",
        "language": "Finnish (Finland)",
        "title": "PureLock: Sovelluslukko",
        "short_desc": "Offline-sovelluslukko & AES-256 SQLCipher-holvi. Ei mainoksia, ei pilveä.",
        "full_desc": """PureLock on mainokseton 100% offline-tietosuojapaketti ja salattu holvi sovelluksille, salasanoille, 2FA-koodeille ja muistiinpanoille — suojattu SQLCipher AES-256:lla ja Android Keystorella.

🔒 PÄÄOMINAISUUDET:
• App Shield (Sovelluslukko): Suojaa sovellukset PIN-koodilla, kuviolla (näkymätön viiva), 4-alueisella Knock-koodilla tai biometriikalla; satunnainen näppäimistö ja viiveaika.
• Laitteistosalattu Holvi: Tallenna salasanat, muistiinpanot, kortit ja 2FA SQLCipher AES-256 -tietokantaan roskakorilla ja entropia-analyysilla.
• Naamiointi & Harhautukset: Toimiva Laskin-lukitus, valekaatumisikkuna ja hätä-PIN (Duress).
• Ravista Lukitaksesi: Välitön hätälukitus säädettävällä herkkyydellä.
• Tunkeilijaloki & SHA-256-tarkastus: Paikalliset lokit, SHA-256-eheystarkistus ja CSV-vienti.
• Leikepöydän Automaattityhjennys & Salasanageneraattori: Luo vahvoja salasanoja offline-tilassa ja tyhjennä kopioidut salaisuudet automaattisesti.
• Paikallinen Automaattitäyttö, Pikaruudut, Widget, salatut .plk-varmuuskopiot ja Android TV.

🛡️ 100% PAIKALLINEN TIETOSUOJA:
Ei pilvitilejä eikä mainoksia. Salatut tietosi pysyvät yksinomaan laitteellasi.

────────────────────────────────────────
ACCESSIBILITY SERVICE API -TIEDONANTO
────────────────────────────────────────
PureLock käyttää AccessibilityService API -rajapintaa ainoastaan ydintoimintoonsa Sovelluslukkoon (App Shield).

• Käyttö: Seuraa ikkunan tilan muutoksia (TYPE_WINDOW_STATE_CHANGED) vain havaitakseen suojatun sovelluksen avaamisen ja näyttääkseen lukitusnäytön heti.
• Tietosuoja: PureLock EI KOSKAAN lue, tallenna tai lähetä näytön sisältöä, näppäinpainalluksia, salasanoja tai yksityisviestejä."""
    },
    {
        "locale": "no-NO",
        "language": "Norwegian (Norway)",
        "title": "PureLock: Applås og Hvelv",
        "short_desc": "Offline applås & AES-256 SQLCipher-hvelv. Ingen reklame, ingen sky.",
        "full_desc": """PureLock er en reklamefri personvernpakke og 100% offline kryptert hvelv for apper, passord, 2FA-koder og private notater — beskyttet med SQLCipher AES-256 og Android Keystore.

🔒 HOVEDFUNKSJONER:
• App Shield (Applås): Lås apper med PIN, Mønster (usynlig spor), 4-kvadrant Knock-kode eller Biometri, med tilfeldig tastatur og utsettelsestid.
• Maskinvarekryptert Hvelv: Lagre passord, notater, kort og 2FA i SQLCipher AES-256 med papirkurv og entropianalyse.
• Kamuflasje & Lokkeskjermer: Fungerende Kalkulator, falsk krasjdialog og nød-PIN (Duress).
• Rist for å Låse: Umiddelbar nødlåsing med justerbar følsomhet.
• Inntrengerlogg & SHA-256-revisjon: Lokale sikkerhetslogger, SHA-256 integritetssjekk og CSV-eksport.
• Auto-tømming av Utklippstavle & Passordgenerator: Lag sterke passord offline og slett kopierte hemmeligheter automatisk.
• Lokal Autofyll, Hurtigfliser, Widget, krypterte .plk-sikkerhetskopier og Android TV.

🛡️ 100% LOKALT PERSONVERN:
Ingen skykontoer og ingen reklame. Krypterte data forblir utelukkende på enheten din.

────────────────────────────────────────
OPPLYSNING OM ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock bruker AccessibilityService API utelukkende til kjernefunksjonen Applås (App Shield).

• Bruk: Overvåker vindusendringer (TYPE_WINDOW_STATE_CHANGED) kun for å oppdage når en beskyttet app åpnes og vise låseskjermen umiddelbart.
• Personvern: PureLock leser, lagrer eller sender ALDRI skjerminnhold, tastetrykk, passord eller private meldinger."""
    },
    {
        "locale": "cs-CZ",
        "language": "Czech (Czechia)",
        "title": "PureLock: Zámek Aplikací",
        "short_desc": "Offline zámek aplikací a trezor AES-256 SQLCipher. Bez reklam a bez cloudu.",
        "full_desc": """PureLock je bezpečnostní sada bez reklam a 100% offline šifrovaný trezor pro vaše aplikace, hesla, 2FA kódy a soukromé poznámky — chráněný pomocí SQLCipher AES-256 a Android Keystore.

🔒 HLAVNÍ FUNKCE:
• App Shield (Zámek aplikací): Zamkněte aplikace pomocí PIN, Vzoru (neviditelná stopa), 4-kvadrantového Knock kódu nebo Biometrie, včetně náhodné klávesnice a doby odkladu.
• Hardwarově šifrovaný trezor: Ukládejte hesla, poznámky, karty a 2FA v databázi SQLCipher AES-256 s košem a analýzou entropie.
• Kamufláž a návnady: Funkční Kalkulačka, falešný pád aplikace a nouzový nátlakový PIN (Duress).
• Zatřesení pro zamknutí: Okamžité nouzové uzamčení s nastavitelnou citlivostí.
• Záznam narušitelů a SHA-256 audit: Lokální bezpečnostní protokoly, kryptografické ověření integrity SHA-256 a export CSV.
• Automatické mazání schránky a generátor hesel: Generujte silná hesla offline a automaticky mazejte zkopírovaná tajemství.
• Lokální automatické vyplňování, rychlé dlaždice, widget, šifrované zálohy .plk a Android TV.

🛡️ 100% LOKÁLNÍ SOUKROMÍ:
Žádné cloudové účty ani reklamy. Vaše šifrovaná data zůstávají výhradně v zařízení.

────────────────────────────────────────
PROHLÁŠENÍ K ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock používá AccessibilityService API výhradně pro hlavní funkci Zámek aplikací (App Shield).

• Použití: Sleduje události změny okna (TYPE_WINDOW_STATE_CHANGED) pouze za účelem zjištění otevření chráněné aplikace a okamžitého zobrazení zamykací obrazovky.
• Soukromí: PureLock NIKDY nečte, neshromažďuje ani nepřenáší obsah obrazovky, stisky kláves, hesla ani osobní zprávy."""
    },
    {
        "locale": "hu-HU",
        "language": "Hungarian (Hungary)",
        "title": "PureLock: Alkalmazászár",
        "short_desc": "Offline alkalmazászár és AES-256 SQLCipher széf. Reklám és felhő nélkül.",
        "full_desc": """A PureLock egy reklámmentes, 100%-ban offline adatvédelmi csomag és titkosított széf alkalmazásai, jelszavai, 2FA kódjai és jegyzetei védelmére SQLCipher AES-256 és Android Keystore segítségével.

🔒 FŐBB FUNKCIÓK:
• App Shield (Alkalmazászár): Zároljon bármely appot PIN-kóddal, Mintával (láthatatlan útvonal), 4-negyedes Knock kóddal vagy Biometriával, véletlen billentyűzettel és türelmi idővel.
• Hardveresen Titkosított Széf: Tároljon jelszavakat, jegyzeteket, kártyákat és 2FA kódokat SQLCipher AES-256 adatbázisban, lomttárral és entrópia-elemzéssel.
• Álcázás és Csalik: Működő Számológép álca, hamis összeomlás párbeszéd és kényszerített vész-PIN (Duress).
• Rázásra Zárolás: Azonnali vészzárolás állítható érzékenységgel.
• Behatoló Napló és SHA-256 Audit: Helyi biztonsági naplók, SHA-256 integritás-ellenőrzés és CSV export.
• Vágólap Auto-törlés és Jelszógenerátor: Erős jelszavak generálása offline és a másolt adatok automatikus törlése.
• Helyi Automatikus Kitöltés, Gyorsbeállítások, Widget, titkosított .plk mentések és Android TV.

🛡️ 100% HELYI ADATVÉDELEM:
Nincs felhőfiók, nincs reklám. Titkosított adatai kizárólag az eszközén maradnak.

────────────────────────────────────────
ACCESSIBILITY SERVICE API NYILATKOZAT
────────────────────────────────────────
A PureLock kizárólag az Alkalmazászár (App Shield) fő funkcióhoz használja az AccessibilityService API-t.

• Használat: Kizárólag az ablakállapot-változásokat (TYPE_WINDOW_STATE_CHANGED) figyeli a védett appok megnyitásának észleléséhez és a zárolási képernyő azonnali megjelenítéséhez.
• Adatvédelem: A PureLock SOHA nem olvas, rögzít vagy továbbít képernyőtartalmat, billentyűleütéseket, jelszavakat vagy személyes üzeneteket."""
    },
    {
        "locale": "ro",
        "language": "Romanian",
        "title": "PureLock: Blocare Aplicații",
        "short_desc": "Blocare aplicații offline și seif AES-256 SQLCipher. Fără reclame, fără cloud.",
        "full_desc": """PureLock este o suită de confidențialitate fără reclame și un seif criptat 100% offline pentru aplicații, parole, coduri 2FA și notițe private — protejat cu SQLCipher AES-256 și Android Keystore.

🔒 FUNCȚII PRINCIPALE:
• App Shield (Blocare Aplicații): Protejează orice aplicație cu PIN, Model (traseu invizibil), Cod Knock în 4 cadrane sau Biometrie, cu tastatură aleatorie și perioadă de grație.
• Seif Criptat Hardware: Salvează parole, notițe, carduri și 2FA în SQLCipher AES-256, cu coș de gunoi automat și analiză a entropiei.
• Camuflaj și Momeli: Calculator funcțional, dialog fals de eroare și PIN de urgență sub constrângere (Duress).
• Scutură pentru Blocare: Blocare de urgență instantanee cu sensibilitate reglabilă.
• Jurnal Intruși și Audit SHA-256: Înregistrează încercările eșuate local, verifică integritatea criptografică SHA-256 și exportă CSV.
• Curățare Clipboard și Generator Offline: Creează parole puternice și șterge automat secretele copiate.
• Completare Automată Locală, Dale Rapide, Widget, backup-uri criptate .plk și Android TV.

🛡️ CONFIDENȚIALITATE 100% LOCALĂ:
Fără conturi în cloud și fără reclame. Datele tale criptate rămân exclusiv pe dispozitiv.

────────────────────────────────────────
UTILIZAREA ȘI DIVULGAREA ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock folosește AccessibilityService API strict pentru funcția principală de Blocare Aplicații (App Shield).

• Cum se utilizează: Monitorizează evenimentele de schimbare a ferestrei (TYPE_WINDOW_STATE_CHANGED) doar pentru a detecta deschiderea unei aplicații protejate și a afișa imediat ecranul de blocare.
• Confidențialitate: PureLock NU citește, NU colectează și NU accesează conținutul ecranului, tastele apăsate, parolele sau mesajele personale."""
    },
    {
        "locale": "el-GR",
        "language": "Greek (Greece)",
        "title": "PureLock: Κλείδωμα Εφαρμογών",
        "short_desc": "Κλείδωμα εφαρμογών offline & AES-256 SQLCipher θησαυροφυλάκιο. Χωρίς διαφημίσεις",
        "full_desc": """Το PureLock είναι μια σουίτα απορρήτου χωρίς διαφημίσεις και ένα 100% offline κρυπτογραφημένο θησαυροφυλάκιο για εφαρμογές, κωδικούς, 2FA και σημειώσεις με SQLCipher AES-256 και Android Keystore.

🔒 ΚΥΡΙΑ ΧΑΡΑΚΤΗΡΙΣΤΙΚΑ:
• App Shield (Κλείδωμα Εφαρμογών): Κλειδώστε εφαρμογές με PIN, Μοτίβο (αόρατη διαδρομή), Κωδικό Knock 4 τεταρτημορίων ή Βιομετρικά, με τυχαίο πληκτρολόγιο και περίοδο χάριτος.
• Κρυπτογραφημένο Θησαυροφυλάκιο: Αποθηκεύστε κωδικούς, σημειώσεις, κάρτες και 2FA σε SQLCipher AES-256 με κάδο ανακύκλωσης και ανάλυση εντροπίας.
• Καμουφλάζ & Δολώματα: Λειτουργική Αριθμομηχανή, ψεύτικο σφάλμα εφαρμογής και PIN έκτακτης ανάγκης (Duress).
• Κούνημα για Κλείδωμα: Άμεσο κλείδωμα έκτακτης ανάγκης με ρυθμιζόμενη ευαισθησία.
• Καταγραφή Εισβολέων & Έλεγχος SHA-256: Τοπικά αρχεία ασφαλείας, κρυπτογραφικός έλεγχος ακεραιότητας SHA-256 και εξαγωγή CSV.
• Αυτόματος Καθαρισμός Προχείρου & Γεννήτρια Κωδικών: Δημιουργεί ισχυρούς κωδικούς offline και διαγράφει αυτόματα τα αντιγραμμένα μυστικά.
• Τοπική Αυτόματη Συμπλήρωση, Πλακίδια, Widget, κρυπτογραφημένα αντίγραφα .plk και Android TV.

🛡️ 100% ΤΟΠΙΚΟ ΑΠΟΡΡΗΤΟ:
Χωρίς λογαριασμούς cloud και χωρίς διαφημίσεις. Τα κρυπτογραφημένα δεδομένα σας παραμένουν αποκλειστικά στη συσκευή σας.

────────────────────────────────────────
ΓΝΩΣΤΟΠΟΙΗΣΗ ΧΡΗΣΗΣ ACCESSIBILITY SERVICE API
────────────────────────────────────────
Το PureLock χρησιμοποιεί το AccessibilityService API αποκλειστικά για τη βασική λειτουργία Κλειδώματος Εφαρμογών (App Shield).

• Χρήση: Παρακολουθεί αλλαγές παραθύρων (TYPE_WINDOW_STATE_CHANGED) μόνο για να εντοπίζει πότε ανοίγει μια προστατευμένη εφαρμογή και να εμφανίζει άμεσα την οθόνη κλειδώματος.
• Απόρρητο: Το PureLock ΔΕΝ διαβάζει, ΔΕΝ καταγράφει και ΔΕΝ αποκτά πρόσβαση σε περιεχόμενο οθόνης, πληκτρολογήσεις, κωδικούς ή προσωπικά μηνύματα."""
    }
]

# Strict Google Play Console length validation
errors = []
for item in listings:
    t_len = len(item["title"])
    s_len = len(item["short_desc"])
    f_len = len(item["full_desc"])
    if t_len > 30:
        errors.append(f"Title too long in {item['locale']}: {t_len} chars ('{item['title']}')")
    if s_len > 80:
        errors.append(f"Short Desc too long in {item['locale']}: {s_len} chars ('{item['short_desc']}')")
    if f_len > 4000:
        errors.append(f"Full Desc too long in {item['locale']}: {f_len} chars")

if errors:
    for err in errors:
        print("ERROR:", err)
    sys.exit(1)

csv_path = "store_listing.csv"
fieldnames = ["Language / Locale", "Language Name", "Title (<=30)", "Short Description (<=80)", "Full Description (<=4000)"]

with open(csv_path, "w", newline="", encoding="utf-8-sig") as f:
    writer = csv.writer(f)
    writer.writerow(fieldnames)
    for item in listings:
        writer.writerow([
            item["locale"],
            item["language"],
            item["title"],
            item["short_desc"],
            item["full_desc"]
        ])

print(f"Successfully generated {csv_path} with {len(listings)} validated languages!")
