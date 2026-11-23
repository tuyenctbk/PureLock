# Google Play Store Listing & Accessibility Policy Documentation (PureLock v2.8)

## 1. App Title & Short Description
- **App Title (max 30 chars)**: `PureLock: Privacy App Lock`
- **Short Description (max 80 chars)**: `Offline App Lock & AES-256 Vault. Zero ads, zero telemetry, zero cloud.`

---

## 2. Mandatory AccessibilityService API Disclosure (English)

> **Copy and paste this section into the Full Description in Google Play Console:**

```text
────────────────────────────────────────
ACCESSIBILITY SERVICE API USAGE & DISCLOSURE
────────────────────────────────────────
PureLock uses the AccessibilityService API strictly to provide its core App Lock (App Shield) feature.

• How it is used: PureLock monitors window state change events (TYPE_WINDOW_STATE_CHANGED) in real time solely to detect when a user-protected application is opened in the foreground. Once detected, PureLock immediately presents a secure PIN, Pattern, Knock Code, or Biometric lock overlay to prevent unauthorized access.
• Privacy & Data Protection: PureLock DOES NOT read, capture, or access user screen content, keystrokes, passwords, or personal messages (canRetrieveWindowContent is disabled).
• Zero Data Collection: PureLock does not collect, upload, or transmit your personal vault data, app usage, or credentials.

Accessibility service permission is optional and only requested when you choose to protect applications with App Shield. It can be enabled or disabled anytime in your device's Accessibility Settings.
```

---

## 3. Complete Full Description (English)

```text
PureLock is a lightweight, ad-free privacy suite and encrypted vault built to protect your apps, passwords, 2FA codes, and private notes strictly on your device. Powered by SQLCipher AES-256 and hardware-backed Android Keystore encryption, PureLock runs offline with zero ads, zero telemetry, and zero cloud accounts.

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

Accessibility service permission is optional and only requested when you choose to protect applications with App Shield. It can be enabled or disabled anytime in your device's Accessibility Settings.
```

---

## 4. Multi-Language Accessibility Disclosures (For Translated Store Listings)

> Note: All 30 localized listings (Title, Short Description, and Full Description including these disclosures) are generated and validated in `store_listing.csv` via `python3 generate_store_listing_csv.py`.

### 🇪🇸 Spanish (Español)
```text
────────────────────────────────────────
USO Y DIVULGACIÓN DE LA API DE ACCESIBILIDAD
────────────────────────────────────────
PureLock utiliza la API de AccessibilityService estrictamente para proporcionar su función principal de Bloqueo de Aplicaciones (App Shield).

• Cómo se utiliza: PureLock monitorea eventos de cambio de ventana (TYPE_WINDOW_STATE_CHANGED) exclusivamente para detectar cuándo se abre una app protegida en primer plano y mostrar de inmediato la pantalla de bloqueo seguro (PIN, Patrón, Knock o Biometría).
• Privacidad y Seguridad: PureLock NO lee, captura ni accede al contenido de la pantalla, pulsaciones de teclas, contraseñas ni mensajes personales (canRetrieveWindowContent desactivado).
• Cero recolección de datos: PureLock no recopila, sube ni transmite tu bóveda, actividad de apps ni credenciales.

Este permiso es opcional, solo se solicita al activar App Shield y puede habilitarse o desactivarse en cualquier momento desde Ajustes de Accesibilidad.
```

### 🇧🇷 🇵🇹 Portuguese (Português)
```text
────────────────────────────────────────
USO E DIVULGAÇÃO DA API ACCESSIBILITYSERVICE
────────────────────────────────────────
O PureLock utiliza a API AccessibilityService estritamente para fornecer sua função principal de Bloqueio de Aplicativos (App Shield).

• Como é utilizado: O PureLock monitora eventos de alteração de janela (TYPE_WINDOW_STATE_CHANGED) exclusivamente para detectar quando um app protegido é aberto em primeiro plano e exibir imediatamente a tela de bloqueio seguro (PIN, Padrão, Knock ou Biometria).
• Privacidade e Proteção de Dados: O PureLock NÃO lê, captura ou acessa conteúdo da tela, digitação, senhas ou mensagens pessoais (canRetrieveWindowContent desativado).
• Zero Coleta de Dados: O PureLock não coleta, envia nem transmite dados do cofre, uso de apps ou credenciais.

A permissão é opcional, solicitada apenas ao ativar o App Shield, e pode ser alterada a qualquer momento nas Configurações de Acessibilidade.
```

### 🇫🇷 French (Français)
```text
────────────────────────────────────────
UTILISATION DE L'API ACCESSIBILITYSERVICE ET DIVULGATION
────────────────────────────────────────
PureLock utilise l'API AccessibilityService uniquement pour fournir sa fonctionnalité principale de verrouillage d'applications (App Shield).

• Utilisation : PureLock surveille les changements d'état des fenêtres (TYPE_WINDOW_STATE_CHANGED) uniquement pour détecter l'ouverture d'une application protégée au premier plan et afficher instantanément l'écran de verrouillage sécurisé (PIN, Schéma, Knock ou Biométrie).
• Confidentialité : PureLock NE lit, NE capture et N'accède à aucun contenu d'écran, frappe de clavier, mot de passe ou message privé (canRetrieveWindowContent désactivé).
• Zéro collecte de données : PureLock ne collecte, ne téléverse ni ne transmet vos données de coffre, votre usage d'applications ou vos identifiants.

Cette autorisation est facultative, demandée uniquement pour App Shield, et modifiable à tout moment dans les Paramètres d'Accessibilité.
```

### 🇩🇪 German (Deutsch)
```text
────────────────────────────────────────
BARRIEREFREIHEITSDIENST (ACCESSIBILITY API) OFFENLEGUNG
────────────────────────────────────────
PureLock verwendet die AccessibilityService-API ausschließlich zur Bereitstellung der App-Sperrfunktion (App Shield).

• Verwendung: PureLock überwacht Fensterstatusänderungen (TYPE_WINDOW_STATE_CHANGED), um zu erkennen, wenn eine geschützte App im Vordergrund geöffnet wird, und zeigt sofort den sicheren Sperrbildschirm (PIN, Muster, Knock-Code oder Biometrie) an.
• Datenschutz: PureLock liest, erfasst oder speichert KEINE Bildschirminhalte, Tastatureingaben, Passwörter oder persönlichen Nachrichten (canRetrieveWindowContent deaktiviert).
• Keine Datenerfassung: PureLock sammelt, überträgt oder teilt keinerlei Tresordaten, App-Nutzung oder Zugangsdaten.

Die Berechtigung ist optional, wird nur für App Shield angefordert und kann jederzeit in den Bedienungshilfen-Einstellungen geändert werden.
```

### 🇮🇹 Italian (Italiano)
```text
────────────────────────────────────────
USO E DIVULGAZIONE DELL'API ACCESSIBILITYSERVICE
────────────────────────────────────────
PureLock utilizza l'API AccessibilityService esclusivamente per fornire la sua funzione principale di Blocco App (App Shield).

• Utilizzo: PureLock monitora gli eventi di cambio stato della finestra (TYPE_WINDOW_STATE_CHANGED) unicamente per rilevare quando un'app protetta viene aperta in primo piano e mostrare immediatamente la schermata di blocco sicura (PIN, Sequenza, Knock o Biometria).
• Privacy e Protezione Dati: PureLock NON legge, cattura o accede ai contenuti dello schermo, tasti digitati, password o messaggi personali (canRetrieveWindowContent disattivato).
• Zero Raccolta Dati: PureLock non raccoglie, carica né trasmette dati della cassaforte, utilizzo delle app o credenziali.

L'autorizzazione è facoltativa, richiesta solo attivando App Shield, e può essere attivata o disattivata in qualsiasi momento nelle Impostazioni di Accessibilità.
```

### 🇷🇺 Russian (Русский)
```text
────────────────────────────────────────
ИСПОЛЬЗОВАНИЕ API ACCESSIBILITYSERVICE
────────────────────────────────────────
PureLock использует AccessibilityService API исключительно для основной функции блокировки приложений (App Shield).

• Назначение: Отслеживаются только события смены окна (TYPE_WINDOW_STATE_CHANGED) для определения запуска защищенного приложения и мгновенного показа экрана блокировки (PIN, графический ключ, Knock-код или биометрия).
• Конфиденциальность: PureLock НЕ читает, НЕ собирает и НЕ перехватывает содержимое экрана, нажатия клавиш, пароли или личные сообщения (canRetrieveWindowContent отключён).
• Ноль сбора данных: PureLock не собирает и не передаёт данные сейфа, статистику приложений или учётные данные.

Разрешение необязательно, запрашивается только для App Shield и может быть отключено в любой момент в настройках Специальных возможностей.
```

### 🇯🇵 Japanese (日本語)
```text
────────────────────────────────────────
ユーザー補助（ACCESSIBILITY SERVICE）APIの利用開示
────────────────────────────────────────
PureLockは、コア機能である「アプリロック（App Shield）」を提供するためにのみAccessibilityService APIを使用します。

・使用目的: 保護対象アプリがフォアグラウンドで起動されたことを検知（TYPE_WINDOW_STATE_CHANGED）し、直ちに安全なロック画面（PIN・パターン・ノックコード・生体認証）を表示します。
・プライバシー保護: 画面上のテキスト、キー入力、パスワード、個人メッセージの閲覧・取得は一切行いません（canRetrieveWindowContentは無効）。
・データ収集ゼロ: 保管庫データ、アプリ利用履歴、認証情報を収集・アップロード・送信することはありません。

ユーザー補助権限は任意で、App Shield使用時のみリクエストされ、端末のユーザー補助設定からいつでも変更できます。
```

### 🇰🇷 Korean (한국어)
```text
────────────────────────────────────────
접근성(ACCESSIBILITY SERVICE) API 사용 및 고지
────────────────────────────────────────
PureLock은 핵심 기능인 '앱 잠금(App Shield)' 제공을 위해서만 AccessibilityService API를 사용합니다.

• 사용 목적: 보호 앱의 포그라운드 실행을 감지(TYPE_WINDOW_STATE_CHANGED)하여 즉시 보안 잠금 화면(PIN, 패턴, 노크 코드, 생체 인증)을 표시합니다.
• 개인정보 보호: 화면 내용, 키 입력, 비밀번호 또는 개인 메시지를 절대 읽거나 수집·접근하지 않습니다(canRetrieveWindowContent 비활성화).
• 데이터 수집 제로: 금고 데이터, 앱 사용 기록, 인증 정보를 수집·업로드·전송하지 않습니다.

접근성 권한은 선택 사항이며 App Shield 사용 시에만 요청되고 기기의 접근성 설정에서 언제든 변경할 수 있습니다.
```

### 🇨🇳 Chinese Simplified (简体中文)
```text
────────────────────────────────────────
无障碍服务（ACCESSIBILITY SERVICE）API 使用声明
────────────────────────────────────────
PureLock 仅使用 AccessibilityService API 提供核心“应用锁（App Shield）”功能。

• 使用方式：仅实时监听窗口状态变更事件（TYPE_WINDOW_STATE_CHANGED），用于检测受保护应用进入前台并立即显示安全锁屏（PIN、图案、敲击密码或生物识别）。
• 隐私保护：绝不读取、捕获或访问屏幕内容、按键输入、密码或个人消息（已禁用 canRetrieveWindowContent）。
• 零数据收集：不收集、上传或传输您的保险箱数据、应用使用记录或凭据。

无障碍权限为可选项，仅在启用 App Shield 时申请，您可随时在系统“无障碍设置”中开启或关闭。
```

### 🇹🇼 Chinese Traditional (繁體中文)
```text
────────────────────────────────────────
無障礙服務（ACCESSIBILITY SERVICE）API 使用聲明
────────────────────────────────────────
PureLock 僅使用 AccessibilityService API 提供核心「應用程式鎖（App Shield）」功能。

• 使用方式：僅即時監聽視窗狀態變更事件（TYPE_WINDOW_STATE_CHANGED），於受保護應用程式開啟時立即顯示安全鎖定畫面（PIN、圖形、敲擊密碼或生物辨識）。
• 隱私保護：絕不讀取、擷取或存取螢幕內容、按鍵輸入、密碼或私人訊息（已停用 canRetrieveWindowContent）。
• 零資料收集：不會收集、上傳或傳送您的保險箱資料、應用程式使用紀錄或憑證。

無障礙權限為選擇性項目，僅於啟用 App Shield 時請求，您可隨時於裝置的無障礙設定中變更。
```

### 🇻🇳 Vietnamese (Tiếng Việt)
```text
────────────────────────────────────────
CÔNG KHAI SỬ DỤNG ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock sử dụng AccessibilityService API nghiêm ngặt chỉ nhằm phục vụ tính năng Khóa Ứng Dụng (App Shield).

• Mục đích sử dụng: PureLock chỉ theo dõi sự kiện thay đổi cửa sổ (TYPE_WINDOW_STATE_CHANGED) để nhận biết khi ứng dụng được bảo vệ mở lên và hiển thị ngay màn hình khóa an toàn (PIN, Hình vẽ, Knock Code hoặc Sinh trắc học).
• Bảo vệ quyền riêng tư: PureLock KHÔNG đọc, chụp hoặc truy cập nội dung màn hình, thao tác bàn phím, mật khẩu hoặc tin nhắn cá nhân (canRetrieveWindowContent đã tắt).
• Không thu thập dữ liệu: PureLock không thu thập, tải lên hoặc truyền dữ liệu két, lịch sử dùng ứng dụng hay thông tin đăng nhập của bạn.

Quyền Trợ năng là tùy chọn, chỉ yêu cầu khi bật App Shield và có thể bật/tắt bất cứ lúc nào trong Cài đặt Trợ năng.
```

### 🇮🇩 Indonesian (Bahasa Indonesia)
```text
────────────────────────────────────────
PENGGUNAAN & PENGUNGKAPAN ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock menggunakan AccessibilityService API semata-mata untuk fitur utama Pengunci Aplikasi (App Shield).

• Cara Penggunaan: Memantau perubahan status jendela (TYPE_WINDOW_STATE_CHANGED) untuk mendeteksi pembukaan aplikasi terlindungi dan segera menampilkan layar kunci aman (PIN, Pola, Knock Code, atau Biometrik).
• Privasi Data: PureLock TIDAK membaca, merekam, atau mengakses konten layar, ketikan, kata sandi, atau pesan pribadi (canRetrieveWindowContent dinonaktifkan).
• Nol Pengumpulan Data: PureLock tidak mengumpulkan, mengunggah, atau mengirimkan data brankas, penggunaan aplikasi, maupun kredensial Anda.

Izin Aksesibilitas bersifat opsional, hanya diminta saat mengaktifkan App Shield, dan dapat diubah kapan saja di Pengaturan Aksesibilitas.
```

### 🇮🇳 Hindi (हिन्दी)
```text
────────────────────────────────────────
ACCESSIBILITY SERVICE API उपयोग और प्रकटीकरण
────────────────────────────────────────
PureLock केवल अपने मुख्य ऐप लॉक (App Shield) फीचर के लिए AccessibilityService API का उपयोग करता है।

• उद्देश्य: सुरक्षित ऐप खुलने का पता लगाने हेतु केवल विंडो परिवर्तन (TYPE_WINDOW_STATE_CHANGED) की निगरानी करता है और तुरंत सुरक्षित लॉक स्क्रीन (PIN, पैटर्न, नॉक कोड या बायोमेट्रिक) दिखाता है।
• गोपनीयता: PureLock स्क्रीन सामग्री, कीस्ट्रोक्स, पासवर्ड या निजी संदेशों को कभी नहीं पढ़ता या एक्सेस नहीं करता (canRetrieveWindowContent अक्षम है)।
• शून्य डेटा संग्रह: PureLock आपके वॉल्ट डेटा, ऐप उपयोग या क्रेडेंशियल्स को एकत्र, अपलोड या प्रसारित नहीं करता।

यह अनुमति वैकल्पिक है, केवल App Shield के लिए माँगी जाती है और डिवाइस की Accessibility Settings से कभी भी बदली जा सकती है।
```

### 🇹🇷 Turkish (Türkçe)
```text
────────────────────────────────────────
ACCESSIBILITY SERVICE API KULLANIMI VE AÇIKLAMASI
────────────────────────────────────────
PureLock, AccessibilityService API'sini yalnızca temel Uygulama Kilidi (App Shield) özelliği için kullanır.

• Kullanım: Korunan bir uygulamanın açıldığını algılamak ve güvenli kilit ekranını (PIN, Desen, Knock Kodu veya Biyometri) göstermek için yalnızca pencere durumu değişikliklerini (TYPE_WINDOW_STATE_CHANGED) izler.
• Gizlilik: Ekran içeriğini, tuş vuruşlarını, parolaları veya kişisel mesajları ASLA okumaz, kaydetmez veya erişmez (canRetrieveWindowContent devre dışıdır).
• Sıfır Veri Toplama: PureLock kasa verilerinizi, uygulama kullanımınızı veya kimlik bilgilerinizi toplamaz, yüklemez veya iletmez.

Bu izin isteğe bağlıdır, yalnızca App Shield için istenir ve cihazınızın Erişilebilirlik Ayarları'ndan dilediğiniz zaman kapatılabilir.
```

### 🇹🇭 Thai (ภาษาไทย)
```text
────────────────────────────────────────
การเปิดเผยการใช้งาน ACCESSIBILITY SERVICE API
────────────────────────────────────────
PureLock ใช้ AccessibilityService API เพื่อฟีเจอร์ล็อกแอป (App Shield) เท่านั้น

• วัตถุประสงค์: ตรวจจับการเปลี่ยนหน้าต่าง (TYPE_WINDOW_STATE_CHANGED) เพื่อรู้เมื่อแอปที่ปกป้องเปิดขึ้นและแสดงหน้าจอล็อกทันที (PIN, รูปแบบ, Knock Code หรือไบโอเมตริกซ์)
• ความเป็นส่วนตัว: ไม่อ่าน บันทึก หรือเข้าถึงเนื้อหาหน้าจอ การพิมพ์ รหัสผ่าน หรือข้อความส่วนตัวใดๆ (ปิดใช้งาน canRetrieveWindowContent)
• ไม่เก็บข้อมูลส่วนบุคคล: PureLock ไม่เก็บรวบรวม อัปโหลด หรือส่งข้อมูลตู้เซฟ การใช้งานแอป หรือรหัสผ่านของคุณ

สิทธิ์การเข้าถึงนี้เป็นทางเลือก ขอเฉพาะเมื่อเปิดใช้ App Shield และสามารถเปิด/ปิดได้ตลอดเวลาในการตั้งค่าการเข้าถึงของอุปกรณ์
```

### 🇸🇦 Arabic (العربية)
```text
────────────────────────────────────────
إفصاح استخدام واجهة ACCESSIBILITY SERVICE API
────────────────────────────────────────
يستخدم PureLock واجهة AccessibilityService API حصرياً لتوفير ميزة قفل التطبيقات الرئيسية (App Shield).

• آلية الاستخدام: يراقب PureLock أحداث تغيير حالة النوافذ (TYPE_WINDOW_STATE_CHANGED) فقط لاكتشاف فتح تطبيق محمي وعرض شاشة القفل الآمنة فوراً (PIN أو النمط أو Knock Code أو البصمة).
• حماية الخصوصية: لا يقرأ PureLock أو يسجل أو يصل إلى محتوى الشاشة أو ضغطات المفاتيح أو كلمات المرور أو الرسائل الشخصية (تم تعطيل canRetrieveWindowContent).
• عدم جمع البيانات: لا يجمع PureLock أو يرفع أو ينقل بيانات خزنتك أو استخدام التطبيقات أو بيانات اعتمادك.

هذا الإذن اختياري ولا يُطلب إلا عند تفعيل App Shield، ويمكن تفعيله أو إيقافه في أي وقت من إعدادات إمكانية الوصول.
```

---

## 5. Play Console Step-by-Step Checklist
1. **Main Store Listing**:
   - Go to **Play Console** -> **Grow** -> **Store presence** -> **Main store listing**.
   - Import or copy localized fields from `store_listing.csv` (30 validated locales).
   - Verify the English and translated Full Descriptions retain the complete AccessibilityService disclosure block.
2. **App Content Declaration**:
   - Go to **Policy and programs** -> **App content** -> **Accessibility services**.
   - Answer **Yes** to using AccessibilityService API.
   - Answer **No** to `isAccessibilityTool`.
   - Add your unlisted video demo link showing: *Disclosure Dialog -> Enable in Accessibility Settings -> Opening a locked app -> Lock screen appears*.
3. **Data Safety & Advertising ID**:
   - PureLock includes **zero analytics, zero crashlytics, zero ads, and zero Advertising ID** SDKs.
   - Optional voluntary supporter donations use Google Play Billing Library 8.3.0 (`com.android.vending.BILLING`).
4. **Overlay (`SYSTEM_ALERT_WINDOW`) Declaration**:
   - Go to **Policy and programs** -> **App content** -> **Special app access** (if prompted).
   - Core purpose: Displaying the privacy PIN/pattern/knock/biometric lock overlay immediately on top of protected apps when launched.
5. **Publish / Resubmit**:
   - Save all changes and submit your update for review.

---

## 6. Google Play Policy Compliance Declarations

### A. Package Visibility Policy (`QUERY_ALL_PACKAGES` vs `<queries>`)
- **Status**: Compliant by design. PureLock **DOES NOT** declare the restricted `android.permission.QUERY_ALL_PACKAGES` permission.
- **Implementation**: PureLock utilizes standard Android 11+ (API 30+) `<queries>` elements in `AndroidManifest.xml` targeted specifically at apps with launcher and leanback categories:
  ```xml
  <queries>
      <intent>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.LAUNCHER" />
      </intent>
      <intent>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
      </intent>
  </queries>
  ```
- **Reviewer Justification**: PureLock only queries user-launchable apps that the user may want to select for privacy locking, respecting user privacy and adhering 100% to Google Play's Package Visibility policy without needing high-risk permission exceptions.

### B. Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)
- **Core Functionality**: When a protected application is brought to the foreground, PureLock's accessibility service launches the `LockOverlayActivity` (with `FLAG_SECURE`) to obstruct unauthorized viewing.
- **Zero-Cloud & Zero-Ad Guarantee**: Overlay is never used for advertising, popups, or deceptive clicks. It serves exclusively as a security shield.

### C. Zero-Telemetry & Local Storage Policy (`allowBackup="false"`)
- PureLock removes all third-party analytics, crash-reporting, and network-tracking SDKs.
- Android cloud auto-backup is disabled (`android:allowBackup="false"`) so encrypted databases and Keystore material stay strictly on-device unless explicitly exported by the user via AES-256-GCM encrypted `.plk` offline backups.

### D. Android 13+ Restricted Settings Resolution Guide
For users who sideload or test APKs prior to Play Store installation:
1. Android 13+ blocks Accessibility Services for non-store installations by default with a "Restricted setting" dialog.
2. In-app guide directs users: **App Info -> Top-right menu (⋮) -> Allow restricted settings -> Authenticate -> Return to PureLock to enable Accessibility**.
3. Once downloaded directly via Google Play Store, this restricted setting check is automatically bypassed by the OS.
