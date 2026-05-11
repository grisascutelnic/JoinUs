# JoinUs

## Descriere Generală

**JoinUs** este o platformă web inovatoare destinată conectării persoanelor cu interese comune și facilitării organizării de activități și întâlniri în grup. Platforma permite utilizatorilor să creeze, să găsească și să participe la diverse activități (sporturi, hobby-uri, socializare, etc.), să comunice în timp real cu alți participanți, să documenteze și să partajeze amintiri comune, și să interacționeze printr-o comunitate activă și implicată.

**Viziunea Platformei**: JoinUs transformă felul în care oamenii se conectează și colaborează în grup. În loc să se bazeze pe rețele sociale tradiționale, platforma se concentrează pe **experienți colective în timp real** - de la organizarea până la amintiri post-eveniment. Fie că ești cineva care vrea să găsească un grup de jogging local, o comunitate de fotografi, sau pur și simplu să-ți petreci timp cu oameni noi într-un mediu sigur și organizat - JoinUs facilitează toată experiența.

**Scopul Principal**: A crea o ecosistem social **conectat, transparent și responsabil** unde oamenii pot:

- **Descoperi Activități pe Măsura Lor**: Fie pe hartă, prin calendare, prin categorii, sau prin setuplicitate și dificultate, utilizatorii găsesc exact ce-i intereseaza,
- **Se Conecta cu Persoane cu Interese Similare**: Grupuri formate în jurul activităților, cu oameni care se cunosc și colaborează
- **Colabora și Comunica în Timp Real**: Chat de grup, sondaje, și comunicare instant pentru coordonare
- **Construi și Păstra Amintiri Comune**: Album digital colectiv cu fotografii, povestiri, și timeline al evenimentului
- **Oferi Feedback și Recenzii**: Sistem de rating care construiește reputație și trustul în comunitate

---

## Tehnologii Utilizate

### Backend - Java Spring Boot Ecosystem
- **Framework**: Spring Boot 4.0.2
  
  Spring Boot este framework-ul principal care asigură un mediu rapid și eficient de dezvoltare. Include:
  - **Spring MVC**: Controlează rutarea web și gestionează requests/responses. Mapează URL-uri la metode de controller care procesează cererile utilizatorilor.
  - **Spring Data JPA**: Elimină codul repetitiv de acces la baza de date prin furnizarea de repository-uri care handle CRUD automat. Folosim JPA (Jakarta Persistence API) pentru ORM (Object-Relational Mapping).
  - **Spring Session JDBC**: În loc să stocheze sesiunile doar în memorie, le stocheaza în PostgreSQL. Asta permite sesiunile persistente și sincronizarea pe mai multe instanțe de aplicație.
  - **Spring Security**: Framework-ul principal de autentificare și autorizare, care protejează endpoint-urile și gestionează rolurile și permisiunile.
  - **Spring WebSocket**: Suporta pentru WebSocket, care permite comunicare bidirectionala în timp real între client și server (essențial pentru chat-ul în timp real).
  - **Spring OAuth2 Client**: Integrare cu provider-i OAuth2 (Google, GitHub) pentru login cu conturi externe, reducând fricciunea de înregistrare.
  
- **Baza de Date**: PostgreSQL
  - RDBMS relațional robust și open-source, perfect pentru o aplicație cu relații complexe între entități (utilizatori, activități, participări, mesaje, etc.)
  - Suporta transacții ACID, indexare avansată, și tipuri de date complexe
  - Stochează persistent DATE utilizatorilor, activităților, chat-urilor, albumelor de amintiri, sondajelor, și recenziilor

- **Caching**: Redis
  - Memorie cache distribuită care accelerează accesul la date frecvent accesate
  - Reduce apelurile la baza de date pentru informații care nu se schimbă frecvent (categorii, notificări de cache, etc.)
  - Îmbunătățește performanța și scalabilitatea pentru mai mulți utilizatori simultani

- **Limbaj**: Java 21
  - Versiune recent a Java cu features moderne (record types, pattern matching, etc.)
  - Stabilitate și performanță pentru aplicații enterprise
  - Vasta colecție de biblioteci și tools

- **Build Tool**: Maven
  - Gestionează dependințe și compilarea proiectului
  - Permite build reproducibil și consistent

### Frontend - Web Technologies
- **Template Engine**: Thymeleaf
  - Motor de renderare server-side care generează HTML dinamic
  - Integrare perfectă cu Spring Security (putem accesa $​{#authentication} și alte Spring var în template)
  - Permite iterare peste liste (activități, participanți), condiții, și formatare de date în template-uri HTML
  - Alternativă la JSP tradițional, mai curată și mai intuitivă
  
- **CSS Framework**: Bootstrap 5
  - Framework CSS modern si responsive care asigură ca site-ul funcționează pe mobile, tableta, și desktop
  - Grid system flexibil pentru layout-uri responsive
  - Componente pre-styled (butoane, carduri, modale, etc.) care accelerează dezvoltarea
  - Bootstrap Icons: Icoame vectoriale mici, clară și consistente (pentru navigație, butoane, etc.)

- **JavaScript**: JavaScript vanilla + Biblioteci UI
  - JavaScript nativ în browser pentru interactivitate (validare form, animații, AJAX)
  - Biblioteci pentru WebSocket client (comunicare real-time cu server-ul)
  - Charts.js sau similar pentru grafice în admin dashboard

### Servicii Externe și Biblioteci
- **Lombok**: Biblioteca Java care reduce boilerplate prin generarea automată de getteri, setteri, constructori, și equals/hashCode metode. Pur și simplu adaugă adanotatii (@Getter, @Setter, @Data) și Lombok handle-ul restul.
  
- **Jackson**: Biblioteca standard pentru JSON serialization/deserialization, permițând convertirea între Java obiecte si JSON (esential pentru API responses si WebSocket messages).
  
- **Cloudinary API**: Serviciu cloud pentru stocarea și gestionarea imaginilor (avatare, imagini de activități, fotografii din memory book). Cloudinary handle resizingul, optimizarea, și servirea imaginilor rapid din CDN global. Nu trebuie să stocheze imagini mare pe my server.
  
- **Pollinations.ai / External Image Generation (Optional)**: API pentru generarea imaginilor AI dacă administratorii vor crea imagini placeholder sau promotional pentru activități.

---

## Funcționalități Principale

### 1. **Autentificare și Autorizare** 
   
   Sistemul de autentificare asigură securitatea și accesul controlat la platformă pentru toți utilizatorii.
   
    - **Înregistrare și Login cu Email/Parolă**: Utilizatorii pot crea conturi noi prin introducerea unui email unic și a unei parole sigure. Sistemul validează informațiile și stochează acreditările în baza de date. La login, utilizatorul este autentificat și i se generează o sesiune persistentă.
    
    - **Autentificare OAuth2 (Google, GitHub, etc.)**: Utilizatorii pot să se conecteze direct folosind conturi externe (Google, GitHub), eliminând necesitatea unei noi parole și accelerând procesul de înregistrare.
    
    - **Roluri de Utilizator (USER, ADMIN)**: Platforma are două tipuri de roluri:
       - **USER**: Utilizatori obișnuiți cu acces la toate funcționalitățile standard
       - **ADMIN**: Administratori cu acces la panoul de control și capacități de moderare
    
    - **Recuperare Parolă prin Email**: Utilizatorii care și-au uitat parola pot solicita o resetare prin email. Se trimite un link de confirmare care le permite să stabilească o nouă parolă într-un mod sigur.
    
    - **Gestionarea Sesiunilor Utilizatorilor**: Sesiunile sunt stocate în baza de date PostgreSQL prin Spring Session JDBC, permițând accesul persistent și sincronizarea pe mai multe instanțe. Utilizatorii rămân conectați între sesiuni până când se deconectează explicit sau sesiunea expiră.

### 2. **Gestionarea Profilului Utilizator**
   
   Profilul utilizatorului este centrul identității digitale din platformă, unde utilizatorii și-au putea personaliza informațiile și să-și comunice prezența.
   
    - **Creare și Editare Profil Personalizat**: La înregistrare, utilizatorul creează un profil cu informații de bază. El poate edita în orice moment: nume complet, data nașterii, biografia personală și preferințele. Schimbările sunt salvate imediat în baza de date.
    
    - **Încărcare și Schimbare Imagine de Profil**: Utilizatorii pot încarc imagini de profil care sunt stocate prin serviciul Cloudinary. Imaginile sunt redimensionate și optimizate pentru performanță. Orice imagine nouă înlocuiește cea anterioară.
    
    - **Vizualizare Profil Public**: Fiecare utilizator are un profil public pe care alți utilizatori pot să îl vadă. Profilul afișează informații precum numele, avatar-ul, biografia și statisticile. Utilizatorii au control asupra ce informații sunt publice (opțiuni de confidențialitate).
    
    - **Vedere Activități Personale și Istoricul Participărilor**: În panoul personal, utilizatorii pot vedea:
       - Activitățile pe care le-au creat și care sunt actuale
       - Istoricul complet al activităților la care au participat (trecute și viitoare)
       - Starea participării lor la fiecare activitate
    
    - **Statistici Utilizator**: Profilul afișează mesuri de reputație și angajament: numărul de activități create, numărul de participări, ratingul mediu din recenzii, și alte metricii care ajută la construirea trustului în comunitate.

### 3. **Crearea și Gestionarea Activităților**
   
   Activitățile sunt nucleul platformei - ele sunt evenimentele pe care utilizatorii le creează și la care se conectează cu alții.
   
    - **Creare Activități Noi cu Detalii Complete**:
       - **Titlu și Descriere**: Utilizatorii furnizează un titlu atractiv și o descriere detaliată a activității
       - **Categorie**: Selectarea din categorii predefinite (sport, hobby, socializare, etc.)
       - **Data și Ora**: Setarea datei și orei exacte a activității
       - **Locație și Adresă**: Specificarea locației pe hartă și a unei adrese textuale
       - **Capacitate**: Setarea numărului maxim de participanți acceptați
       - **Imagine**: Încărcarea unei imagini reprezentative pentru activitate
       - **Tag-uri**: Adăugarea de cuvinte cheie pentru a folosi în căutare
    
    - **Editare și Ștergere Activități**: Creatorul unei activități poate edita orice detaliu (dată, loc, descriere) oricând. Dacă activitatea apare necorespunzătoare, creatorul o poate șterge, dar numai dacă nu au participat alți utilizatori sau cu mandat administrativ.
    
    - **Filtrare și Căutare Activități**: Utilizatorii pot căuta activități după:
       - Text (titlu, descriere, locație)
       - Categorie
       - Interval de dată
       - Distanță de locația curentă
    
    - **Vizualizare Activități pe Hartă Interactivă**: O hartă interactivă (Leaflet) afișează toutes activitățile ca markeri. Utilizatorii pot vedea toate evenimentele în zona lor și pot clica pe markeri pentru a vedea detaliile.
    
    - **Calendar cu Activități Planificate**: Un calendar vizual arată activitățile planificate cu culori diferite pentru fiecare categorie. Utilizatorii pot naviga lunar și pot vedea în colpo de ochi ce se întâmplă.
    
    - **Notificări pentru Activități Apropiate**: Sistemul trimite notificări anticipate (de ex., 24 de ore înainte) pentru a reaminti utilizatorilor despre activitățile la care sunt înscriși.

### 4. **Participarea la Activități**
   
   Sistemul de participare gestionează cum utilizatorii se înscriu la activități și cum creatorii aprobă sau resping participanții.
   
    - **Înscrierea la Activități**: Utilizatorul poate cere să participe la o activitate cu un singur clic. Cererea de participare intră inițial în stare "PENDING" (în așteptare).
    
    - **Anularea Participării**: Utilizatorii pot anula participarea lor la o activitate oricând (dacă organizatorul permite). O notificare este trimisă organizatorului.
    
    - **Stări de Participare Granulare**:
       - **PENDING**: Cererea inițială, în așteptare de aprobare
       - **APPROVED**: Utilizatorul a fost acceptat și va participa
       - **LEFT**: Utilizatorul și-a anulat participarea
       - **REJECTED**: Organizatorul a respins cererea
       - **EXCLUDED**: Utilizatorul a fost exclus de organizator
       - **BLOCKED**: Utilizatorul nu mai poate participa la viitoarele activități (posibil din cauza comportamentului necorespunzător)
    
    - **Aprovare Participanți**: Creatorul activității primește o listă de cereri și poate aproba sau respinge fiecare participant individual. Dacă activitatea a atins capacitatea maximă, nu mai pot fi acceptați alți participanți.
    
    - **Vizualizare Listă Participanți**: La detail o activitate, se vede lista tuturor participanților acceptați, cu avatarurile și numele lor. Utilizatorii pot accesa rapid profilurile celorlalți.
    
    - **Statistici Participare**: Activitățile afișează metrici cum ar fi numărul de participanți, numărul de cereri în așteptare, și procentul de capacitate utilizată.

### 5. **Chat și Comunicare în Timp Real**
   
   Comunicarea instantanee este esențială pentru ca grupurile participante să se coordoneze și să interacționeze.
   
    - **Chat WebSocket pentru Comunicare Instantanee**: Fiecare activitate are propriul chat de grup. Conexiunile WebSocket asigură că mesajele sunt primite în real-time fără delay și fără necesitatea de refresh. Utilizatorii conectați la chat pot vedea imediat ce scriu alții.
    
    - **Tipuri de Mesaje Variate**:
       - **Text Simplu**: Mesaje de text normal
       - **Imagini**: Utilizatorii pot să încarce imagini direct în chat, care sunt stocate și afișate inline
       - **Fișiere**: Posibilitatea de a partaja documente, PDF-uri, etc.
       - **Reacții Emoji**: Utilizatorii pot reacționa la mesaje cu emoji-uri (like, laugh, etc.) fără a trimite mesaje suplimentare
    
    - **Status de Citit/Necitit**: Sistemul urmărește dacă mesajele au fost văzute de utilizatori, oferind feed-back asupra cine a citit mesajele. Un mesaj poate fi marcat și ca "văzut" de fiecare participant.
    
    - **Marcare Mesaje ca Văzute**: Utilizatorii pot marca manual mesajele ca văzute pentru a gestiona notificările, sau sistemul le pune la văzute automat când intră în chat.
    
    - **Notificări pentru Mesaje Noi**: Atunci când un utilizator primește un mesaj nou în chat și nu este conectat, i se trimite o notificare. Dacă deja este pe pagina chat-ului, nu se trimite notificare redundantă.
    
    - **Istoric Chat Persistent**: Toate mesajele sunt salvate în baza de date. Utilizatorii care intră mai târziu pot vedea întreaga conversație anterioară și pot prinde aspectul.

### 6. **Album de Amintiri (Memory Book)**
   
   Memory Book-ul este o colecție colectivă a amintirilor unei activități, jucând rolul unui jurnal digital.
   
    - **Creare Album Colectiv**: După ce o activitate se încheie, participanții pot contribui la un album de amintiri. Fiecare activitate are propriul album în care toți pot adăuga conținut.
    
    - **Adăugare Intrări Text**: Participanții pot scrie povestiri, comentarii despre activitate și impresii personale. Intrările sunt ordonate cronologic și formează o narative a evenimentului.
    
    - **Încărcare și Organizare Fotografii de Grup**: Utilizatorii pot încarca fotografii de la activitate. Imaginile sunt organizate cronologic și se pot sorta. Cloudinary gestionează stocarea și resizarea optimă.
    
    - **Tag-uri și Comentarii la Fotografii**: Fiecare fotografie poate fi taggată (de exemplu, s-ar putea etichieta persoane în imagine). Alte utilizatori pot lasa comentarii sub fotografii.
    
    - **Timeline Vizual**: Album-ul se afișează ca o cronologie intuitivă unde textele și fotografiile sunt intercalate. Utilizatorii pot naviga temporal prin amintiri.
    
    - **Vizualizare și Descărcare Album Complet**: Utilizatorii pot vedea albumul finalizat. De asemenea, există opțiune de a descărca album-ul complet (txt + imagini) ca backup personal.

### 7. **Sondaje și Votări** (Polls)
    
    Sondajele ajută grupurile să ia decizii colective rapid și egal.
    
    - **Creare Sondaje**: Pe o activitate, creatorul sau orice participant autorizat poate crea sondaje. De exemplu: "Unde mâncăm după?" cu opțiuni diferite.

    - **Opțiuni Vocale Multiple**: Fiecare sondaj are 2-N opțiuni din care utilizatorii pot alege. Utilizatorii pot vota pentru o singură opțiune sau mai multe, în funcție de setări.

    - **Vizualizare Rezultate în Timp Real**: Pe măsură ce participanții votează, graficele se actualizează live. Utilizatorii pot vedea în timp real cum se distribuie voturile.

    - **Notificări pentru Noi Sondaje**: Când un sondaj nou este creat, toți participanții la activitate primesc o notificare. Asta le încurajează să participeze și să-și exprime opiniile.

    - **Istoricul Sondajelor**: Toate sondajele dintr-o activitate (chiar și cele închise) sunt în istoric. Utilizatorii pot revizui cum s-au luat deciziile în trecut.

### 8. **Sistem de Recenzii și Evaluări**
    
    Recenziile construiesc reputația și trustul în comunitate.
    
    - **Plasare Recenzii la Alți Utilizatori**: După o activitate, participanții pot lasa recenzii unii despre alții. Scopul este să evalueze colaborarea, atitudinea și respectarea agreementelor.

    - **Rating pe Scale 1-5 Stele**: Fiecare recenzie include un rating numeric (1-5 stele) care arată satisfacția cu persoana. Sistemul calculează automat media pentru profil.

    - **Text Descriptiv pentru Feedback**: Alături de stele, utilizatorii pot scrie comentarii detaliate: "Persoană foarte prietenoasă și punctuală", etc. Asta ajută comunitatea să înțeleagă calitățile.

    - **Vizualizare Profil cu Recenzii Agregate**: Profilul utilizatorului afișează:
       - Rating mediu din toate recenziile
       - Numărul total de recenzii
       - Ultimele recenzii scrise
       - Tendință în timp (recenziile se îmbunătățesc sau deteriorează?)

    - **Protecție Împotriva Recenziilor Duble**: Sistemul limitează fiecare utilizator la o singură recenzie per altul per activitate. Evită spam-ul și votul multiplu.

### 9. **Notificări și Alertări**
    
    Notificările țin utilizatorii la curent cu tot ce se întâmplă pe platformă.
    
    - **Notificări Multiple**:
       - **Invitații la Activități**: Când cineva este invitat direct la o activitate
       - **Aprobări/Respingeri Participării**: Când organizatorul decide asupra cererii
       - **Mesaje Noi în Chat**: Când cineva i-a scris în chat
       - **Amintiri de Activități Apropiate**: Notificări anticipate cu 24h-1h înainte de eveniment
       - **Noi Sondaje**: Când s-a creat un sondaj la o activitate
       - **Novi Participanți**: Când cineva nou s-a înscris la "ta" activitate
    
    - **Stare de Notificare (Citit/Necitit)**: Fiecare notificare are stare. Atunci când este citită, este marcată ca atare. Utilizatorii au contador al notificărilor necitite.
    
    - **Tipuri de Notificări Configurabile**: Utilizatorii pot alege care tipuri de notificări să primească. De exemplu, s-ar putea dezactiva notificările peste joasă importanță.
    
    - **Filtrare și Sortare Notificări**: Centrul de notificări permite filtrarea după tip, dată și cuvinte cheie. Utilizatorii pot vedea doar ce-i interesează.

### 10. **Panou Administrator (Admin Dashboard)**
   
   Dashboard-ul admin oferă control și monitorizare asupra platformei.
   
    - **Monitorizare Activități**: Administratorii pot vedea toate activitățile, pot le marca ca validate/inspectate, și pot șterge activități care incalcă termeniia serviciului (conținut nepotrivit, locații invalide, etc.).
    
    - **Gestionare Utilizatori**: Administratorii pot:
       - Vedea profil complet al oricărui utilizator
       - Dezactiva conturi care abuzează
       - Șterge utilizatori (cu impact!)
       - Rezeta parole dacă necesare
    
    - **Moderation Comentarii și Mesaje**: Administratorii pot monitoriza chat-urile și comentariile pe memory book-uri. Dacă ceva este nepotrivit, pot șterge mesaje sau avertiza utilizatorul.
    
    - **Rapoarte și Statistici Generale**:
       - Grafice ale creșterii utilizatorilor în timp
       - Activități create și participări
       - Mesaje și activitate în chat
       - Metrici de engagement
    
    - **Gestionare Categorii de Activități**: Administratorii pot adăuga, edita sau șterge categorii din care utilizatorii pot alege la crearea activităților.
    
    - **Control asupra Conținutului Incriminat**: Sistem de raportare și review pentru conținut care incalcă reguli. Administratorii pot actiona rapid.

### 11. **Hartă Interactivă și Localizare**
   
   Harta interactivă este o modalitate intuitivă de a descoperi și naviga activități.
   
    - **Vizualizare Activități pe Hartă**: Folosind Leaflet.js, fiecare activitate este reprezentată cu un marker pe hartă. Harta este interactivă și permite zoom și pan.
    
    - **Filtrare După Locație**: Utilizatorii pot afisa doar activitățile dintr-o anumită zonă geografică (de ex., doar în rază de 10 km de locația curentă).
    
    - **Distanță de Utilizateur**: Pentru fiecare activitate, se afișează distanța approximativă de la locația curentă a utilizatorului. Asta ajută la planuirea deplasării.
    
    - **Direcții și Rute**: Utilizatorii pot obține indicații turn-by-turn către locția activității, integrând cu Google Maps sau servicii similare pentru navigație.

---

## Setup și Rulare

### Cerințe Preliminare
- **Java 21 JDK**: Runtime-ul Java necesar pentru a compila și rula aplicația Spring Boot
- **Docker și Docker Compose**: Pentru a rula PostgreSQL și Redis în containere (alternativă la instalare locală)
- **Maven**: Build tool pentru a compila și empacheta aplicația (sau folosiți `./mvnw` sau `mvnw.cmd` care sunt script-uri Maven wrapper incluse)
- **Git**: Pentru clonarea repository-ului (opțional, dacă nu ai deja codul)

### Instalare și Pornire

1. **Clonează repository-ul**
   ```bash
   git clone <repository-url>
   cd JoinUs
   ```

2. **Pornește Serviciile Dependente (PostgreSQL + Redis)**
   ```bash
   docker compose up -d
   ```
   Asta va crea și porni două containere:
   - **PostgreSQL**: Baza de date relațională pe portul 5432
   - **Redis**: Memorie cache pe portul 6379
   
   Containerele vor porni în background și se va salva stare lor pentru sesiuni viitoare.

3. **Compilează și Rulează Aplicația**
   ```bash
   ./mvnw spring-boot:run
   ```
   Sau pe Windows:
   ```cmd
   mvnw.cmd spring-boot:run
   ```
   
   Asta va:
   - Descarca dependințele Maven (prima oară)
   - Compila codul Java
   - Pornire embedded Tomcat server
   - Inițializa baza de date din `schema.sql`
   
   Așteptă până când vezi:
   ```
   Tomcat started on port(s): 8080 (http)
   JoinusApplication started in X.XXX seconds
   ```

4. **Accesează Aplicația în Browser**
   - URL: `http://localhost:8080`
   - Homepage-ul ar trebui să se încarce cu pagina de login/înregistrare

5. **Oprire Servicii**
   ```bash
   docker compose down
   ```
   Asta va opri și șterge containerele (datele din baza de date vor fi șterse dacă nu ai setat persistență în docker-compose.yml).

### Variabile de Mediu și Configurație

Editeaza `src/main/resources/application.properties` și configurează:

- `spring.datasource.url=jdbc:postgresql://localhost:5432/joinus` - URL baza de date PostgreSQL
- `spring.datasource.username=postgres` - Utilizator PostgreSQL
- `spring.datasource.password=password` - Parola PostgreSQL
- `spring.redis.host=localhost` - Host Redis
- `spring.redis.port=6379` - Port Redis
- `oauth2.client-id=<your-google-oauth-id>` - Client ID pentru Google OAuth (opțional)
- `oauth2.client-secret=<your-google-oauth-secret>` - Client Secret pentru Google OAuth (opțional)
- `cloudinary.cloud-name=<your-cloudinary-cloud>` - Cloud name din Cloudinary
- `cloudinary.api-key=<your-cloudinary-api-key>` - API Key din Cloudinary
- `cloudinary.api-secret=<your-cloudinary-api-secret>` - API Secret din Cloudinary

Dacă vrei să testezi fără OAuth2 și Cloudinary, poți sări peste acele configurări și sistemul va merge cu login normal.

---

## Structura Proiectului

```
JoinUs/
├── src/
│   ├── main/
│   │   ├── java/com/scutelnic/joinus/
│   │   │   ├── controller/
│   │   │   │   ├── ActivityController.java          # CRUD activități + detail
│   │   │   │   ├── ActivityParticipationController.java # Join/Leave activități
│   │   │   │   ├── ActivityChatApiController.java       # API pentru mesaje chat
│   │   │   │   ├── ActivityChatWebSocketController.java # WebSocket real-time chat
│   │   │   │   ├── ActivityMemoryController.java   # Memory book - intrări + fotografii
│   │   │   │   ├── NotificationApiController.java  # Notificări utilizator
│   │   │   │   ├── AuthController.java             # Login, register, logout
│   │   │   │   ├── AdminController.java            # Admin dashboard
│   │   │   │   ├── PageController.java             # Rute HTML pentru pagini
│   │   │   │   └── GlobalModelAttributes.java      # Attributuri globale pentru templates
│   │   │   │
│   │   │   ├── service/
│   │   │   │   ├── ActivityService.java            # Logică pentru crearea/editarea activităților
│   │   │   │   ├── ActivityParticipationService.java # Logică pentru participări
│   │   │   │   ├── ActivityChatService.java        # Salvare și retrieval mesaje
│   │   │   │   ├── ActivityMemoryService.java      # Album de amintiri
│   │   │   │   ├── NotificationService.java        # Creare și trimitere notificări
│   │   │   │   ├── UserService.java                # Profil utilizator
│   │   │   │   ├── UserReviewService.java          # Recenzii utilizatori
│   │   │   │   ├── CloudinaryService.java          # Upload imagini
│   │   │   │   ├── CustomUserDetailsService.java   # Spring Security user details
│   │   │   │   ├── OAuthAccountService.java        # OAuth2 account linkage
│   │   │   │   ├── PollinationsImageService.java   # AI image generation (opțional)
│   │   │   │   └── ActivityUnreadService.java      # Track mesaje necitite
│   │   │   │
│   │   │   ├── repository/
│   │   │   │   ├── ActivityRepository.java         # JPA queries pentru activități
│   │   │   │   ├── ActivityParticipationRepository.java 
│   │   │   │   ├── ActivityMessageRepository.java  # Mesaje chat
│   │   │   │   ├── ActivityMemoryBookRepository.java
│   │   │   │   ├── ActivityPollRepository.java     # Sondaje
│   │   │   │   ├── ActivityPollVoteRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── UserReviewRepository.java
│   │   │   │   ├── NotificationRepository.java
│   │   │   │   └── etc.
│   │   │   │
│   │   │   ├── entity/
│   │   │   │   ├── User.java                       # Utilizatori platform
│   │   │   │   ├── Activity.java                   # Activități
│   │   │   │   ├── ActivityParticipation.java      # Participări (join table)
│   │   │   │   ├── ActivityMessage.java            # Mesaje chat
│   │   │   │   ├── ActivityMessageReaction.java    # Reacții emoji
│   │   │   │   ├── ActivityMemoryBook.java         # Album de amintiri pe activitate
│   │   │   │   ├── ActivityMemoryEntry.java        # Intrări text în album
│   │   │   │   ├── ActivityMemoryPhoto.java        # Fotografii în album
│   │   │   │   ├── ActivityPoll.java               # Sondaje
│   │   │   │   ├── ActivityPollOption.java         # Opțiuni sondaj
│   │   │   │   ├── ActivityPollVote.java           # Voturi
│   │   │   │   ├── UserReview.java                 # Recenzii utilizatori
│   │   │   │   ├── Notification.java               # Notificări
│   │   │   │   ├── NotificationType.java           # Enum tipuri notificări
│   │   │   │   ├── ParticipationStatus.java        # Enum stări participare
│   │   │   │   ├── Role.java                       # Enum roluri (USER, ADMIN)
│   │   │   │   └── etc.
│   │   │   │
│   │   │   ├── dto/
│   │   │   │   ├── ActivityDTO.java                # DTO pentru trimiterea datelor API
│   │   │   │   ├── ActivityMessageDTO.java         # Format mesaje pentru WebSocket
│   │   │   │   ├── NotificationDTO.java
│   │   │   │   ├── UserProfileDTO.java
│   │   │   │   └── etc.
│   │   │   │
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java             # Spring Security configurare
│   │   │   │   ├── WebSocketConfig.java            # WebSocket configurare
│   │   │   │   ├── CloudinaryConfig.java           # Cloudinary setup
│   │   │   │   └── JpaConfig.java                  # JPA/Hibernate settings
│   │   │   │
│   │   │   └── JoinusApplication.java              # Clasa main @SpringBootApplication
│   │   │
│   │   └── resources/
│   │       ├── templates/                           # Thymeleaf HTML
│   │       │   ├── index.html                      # Homepage, feed de activități
│   │       │   ├── activities.html                 # Listing activități cu filtru
│   │       │   ├── activity-detail.html            # Detail activitate
│   │       │   ├── create-activity.html            # Form creare activitate
│   │       │   ├── chat.html                       # Chat view pentru o activitate
│   │       │   ├── memories.html                   # Memory book view
│   │       │   ├── calendar.html                   # Calendar view
│   │       │   ├── map.html                        # Hartă interactivă
│   │       │   ├── profile.html                    # Profil propriu
│   │       │   ├── profile-edit.html               # Edit profil
│   │       │   ├── user-profile.html               # Profil public altui utilizator
│   │       │   ├── admin.html                      # Admin dashboard
│   │       │   ├── forum.html                      # Forum / discussion board
│   │       │   ├── forgot-password.html            # Password reset
│   │       │   ├── event-listing.html              # Listing cu date/luni
│   │       │   └── fragments/                      # Thymeleaf fragments (header, nav, etc.)
│   │       │
│   │       ├── static/
│   │       │   ├── css/
│   │       │   │   ├── bootstrap.min.css           # Bootstrap CSS
│   │       │   │   ├── bootstrap-icons.css         # Bootstrap Icons
│   │       │   │   └── styles.css                  # Custom styles
│   │       │   ├── js/
│   │       │   │   ├── chat.js                     # WebSocket chat client
│   │       │   │   ├── map.js                      # Leaflet map init
│   │       │   │   ├── calendar.js                 # Calendar logic
│   │       │   │   ├── notifications.js            # Client-side notificări
│   │       │   │   └── util.js                     # Utility functions
│   │       │   ├── fonts/
│   │       │   │   └── bootstrap-icons.woff*       # Icon fonts
│   │       │   └── images/
│   │       │       └── uploads/                    # User-uploaded images (temp/fallback)
│   │       │
│   │       ├── application.properties              # Configurație Maven profiles
│   │       └── schema.sql                          # SQL pentru inițializare bază date
│   │
│   └── test/
│       └── java/com/scutelnic/joinus/
│           ├── JoinusApplicationTests.java         # Teste Spring Boot
│           └── etc.
│
├── .mvn/                          # Maven wrapper files
├── mvnw / mvnw.cmd                # Maven wrapper scripts
├── docker-compose.yml             # Docker compose pentru PostgreSQL + Redis
├── pom.xml                        # Maven project object model (dependințe + plugins)
├── .gitignore
├── .git/                          # Git repository
└── README.md                      # Documentație
```

### Explainere Directoare Cheie

- **controller/**: Receivează HTTP requests de la utilizatori și tripla procesarea. Folosesc @RestController pentru API endpoints și @Controller pentru page HTML renders.

- **service/**: Conține logica de business care nu ar trebui în controller. Exemplu: `ActivityService.createActivity()` validează datele, salvează în baza de date, și poate trimite notificări.

- **repository/**: JPA interfaces care extind `CrudRepository` sau `JpaRepository` și permit query-uri la baza de date cu minim cod.

- **entity/**: Clase marcate cu `@Entity` care mapează tabelele din baza de date. Lombok `@Getter` `@Setter` elimină getter/setter boilerplate.

- **dto/**: Data Transfer Objects - versiuni "slim" ale entităților care sunt trimise ca JSON pe API, evitând lazy loading issues și exposing unnecessary fields.

- **config/**: Configurare Spring beans - SecurityConfig (Spring Security), WebSocketConfig (pentru chat real-time), etc.

- **templates/**: HTML template-uri Thymeleaf care sunt renderizate server-side cu date din model.

