# FiveM Sunucu Kurulum + Topluluk Büyütme Rehberi

Bu doküman 4 bölümden oluşuyor:

1. [Donanım değerlendirmesi](#1-donanim)
2. [Debian + Pterodactyl panel kurulumu](#2-debian--panel-kurulumu)
3. [Ücretsiz asset / script kaynakları](#3-ucretsiz-assetler)
4. [Oyuncu çeken sunucu konsepti ve büyütme planı](#4-sunucu-plani)
5. [Discord toplulukları](#5-discord)

---

## 1. Donanım

Senin makinen: **55 GB DDR5 RAM + Ryzen 9 (9950X sınıfı) + nested virtualization**.

> Küçük not: "Ryzen 9 950X" diye bir model yok — muhtemelen **9950X** (Zen 5) veya **7950X**. İkisi de bu iş için üst segment.

### Yeterli mi? Fazlasıyla.

FiveM sunucusunun performansı **neredeyse tamamen tek çekirdek (single-thread) hızına** bağlıdır. `FXServer` ana oyun döngüsünü tek bir thread'de çalıştırır; 16 çekirdeğin çoğu boşta durur. 9950X'in single-thread skoru piyasadaki en iyilerden biri, yani:

| Kaynak | Gereken | Sende |
|---|---|---|
| CPU (64 oyuncu, ESX/QBCore) | 1 güçlü çekirdek + 2-3 yardımcı | 16C/32T |
| RAM (sunucu) | 4–8 GB | 55 GB |
| RAM (MySQL) | 2–4 GB | ✔ |
| Disk | 40–60 GB SSD/NVMe (asset'lerle 100 GB+) | Kontrol et |
| Ağ | 64 oyuncu ≈ 30–50 Mbit upload | Kontrol et |

**Gerçekçi kapasite:** Bu makinede rahatlıkla **2–3 ayrı FiveM sunucusu** (her biri 64–128 slot) + MySQL + Pterodactyl paneli + Discord bot birlikte çalışır.

### Dikkat edilecek gerçek darboğazlar

1. **Nested virtualization ~%5–15 CPU cezası getirir.** FiveM'de bu doğrudan `server thread hitch` olarak hissedilir. Mümkünse FiveM'i en dış katmanda (bare-metal veya tek kat VM) çalıştır, panel/DB'yi iç katmanda tut — tersi değil.
2. **Disk I/O.** Nested kurulumlarda disk genelde ilk çöken yer. MySQL'i mutlaka NVMe'de tut, HDD'ye koyma.
3. **Upload bandwidth** — ev bağlantısıysa asıl sınırın bu olur, CPU değil. 64 oyuncu için en az 50 Mbit stabil upload gerekir. Asset'ler ilk bağlantıda oyuncuya indirilir; bunun için ayrıca **cache server / CDN** (aşağıda) kullan.
4. **CPU governor**: `performance` moda al, `powersave`'de tick süreleri dalgalanır.

```bash
sudo apt install -y linux-cpupower
sudo cpupower frequency-set -g performance
```

### Beklenen performans

- Temiz QBCore/ESX + 40-50 script: **0.10–0.30 ms/tick**, 100+ oyuncuda sorunsuz.
- Kötü yazılmış 200 script: hangi CPU olursa olsun düşer. Performans donanımdan değil, **script hijyeninden** gelir. `txAdmin > Server > Resource Monitor` ile sürekli takip et.

---

## 2. Debian + Panel Kurulumu

İki yol var. **A yolu (txAdmin)** yeni başlayan için doğru seçim; **B yolu (Pterodactyl)** birden fazla sunucu / ekip yönetimi içindir. Sende iyi bir makine olduğu için **ikisini birlikte** kullanmanı öneririm: Pterodactyl container'ı içinde txAdmin.

### Ön hazırlık (Debian 12 "Bookworm")

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl wget git unzip tar xz-utils \
    build-essential ca-certificates gnupg ufw screen
```

Firewall:

```bash
sudo ufw allow 22/tcp          # SSH
sudo ufw allow 30120/tcp       # FiveM oyun portu
sudo ufw allow 30120/udp
sudo ufw allow 40120/tcp       # txAdmin web arayüzü
sudo ufw allow 80,443/tcp      # panel / cache
sudo ufw enable
```

Sunucuyu **root ile çalıştırma**, ayrı kullanıcı aç:

```bash
sudo adduser --disabled-password --gecos "" fivem
sudo mkdir -p /home/fivem/server /home/fivem/txData
sudo chown -R fivem:fivem /home/fivem
```

### A Yolu — txAdmin ile doğrudan kurulum

1. **FXServer artifact'ını indir.** Güncel sürümü şuradan al:
   `https://runtime.fivem.net/artifacts/fivem/build_proot_linux/master/`
   (LATEST RECOMMENDED olanı seç, "latest" değil.)

```bash
sudo -u fivem -i
cd ~/server
wget https://runtime.fivem.net/artifacts/fivem/build_proot_linux/master/<SURUM>/fx.tar.xz
tar xf fx.tar.xz && rm fx.tar.xz
```

2. **MariaDB kur** (host tarafında):

```bash
sudo apt install -y mariadb-server
sudo mysql_secure_installation
sudo mysql -u root -p
```
```sql
CREATE DATABASE fivem CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'fivem'@'localhost' IDENTIFIED BY 'GUCLU_BIR_SIFRE';
GRANT ALL PRIVILEGES ON fivem.* TO 'fivem'@'localhost';
FLUSH PRIVILEGES;
```

3. **Başlat:**

```bash
cd /home/fivem/server
./run.sh +set serverProfile default +set txAdminPort 40120
```

Konsolda tek kullanımlık bir PIN çıkar. Tarayıcıdan `http://SUNUCU_IP:40120` adresine gir, PIN'i yaz, Cfx.re hesabınla bağla. txAdmin sana **recipe** (hazır kurulum tarifi) sunar — `QBCore` veya `ESX` seç, veritabanı bilgilerini gir, gerisini kendisi kurar.

4. **Servis olarak çalıştır** (`/etc/systemd/system/fivem.service`):

```ini
[Unit]
Description=FiveM Server
After=network.target mariadb.service

[Service]
Type=simple
User=fivem
WorkingDirectory=/home/fivem/server
ExecStart=/home/fivem/server/run.sh +set serverProfile default +set txAdminPort 40120
Restart=on-failure
RestartSec=10
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now fivem
sudo journalctl -u fivem -f
```

### B Yolu — Pterodactyl Panel (Debian 12)

Pterodactyl = **Panel** (web arayüzü) + **Wings** (sunucuları Docker'da çalıştıran daemon). Aynı makinede ikisi de olabilir.

**Panel bağımlılıkları:**

```bash
sudo apt install -y php8.2 php8.2-{cli,gd,mysql,pdo,mbstring,tokenizer,bcmath,xml,fpm,curl,zip} \
    mariadb-server nginx tar unzip git redis-server
curl -sS https://getcomposer.org/installer | sudo php -- --install-dir=/usr/local/bin --filename=composer
```

**Panel kurulumu:**

```bash
sudo mkdir -p /var/www/pterodactyl && cd /var/www/pterodactyl
sudo curl -Lo panel.tar.gz https://github.com/pterodactyl/panel/releases/latest/download/panel.tar.gz
sudo tar -xzvf panel.tar.gz && sudo chmod -R 755 storage/* bootstrap/cache/
sudo cp .env.example .env
sudo composer install --no-dev --optimize-autoloader
sudo php artisan key:generate --force
sudo php artisan p:environment:setup
sudo php artisan p:environment:database
sudo php artisan migrate --seed --force
sudo php artisan p:user:make          # admin hesabı
sudo chown -R www-data:www-data /var/www/pterodactyl/*
```

Nginx + SSL (`certbot`) ayarlarını Pterodactyl'in resmi dokümanından birebir uygula:
https://pterodactyl.io/panel/1.0/getting_started.html

**Wings (Docker gerekir):**

```bash
curl -fsSL https://get.docker.com | sudo sh
sudo systemctl enable --now docker
sudo mkdir -p /etc/pterodactyl
sudo curl -Lo /usr/local/bin/wings \
  "https://github.com/pterodactyl/wings/releases/latest/download/wings_linux_amd64"
sudo chmod u+x /usr/local/bin/wings
```

Panelde **Location** ve **Node** oluştur, Node'un "Configuration" sekmesindeki komutu sunucuda çalıştır, sonra:

```bash
sudo systemctl enable --now wings
```

**FiveM egg'ini ekle:** Panel > Nests > Import Egg. FiveM egg'i:
`https://github.com/pterodactyl/parkerd-eggs` veya `https://github.com/Ptero-Eggs/game-eggs` (Games > FiveM). Egg'i import ettikten sonra yeni sunucu oluştururken seç.

> **Nested VM notu:** Wings Docker kullanır. Nested virtualization içinde Docker çalışır ama `overlay2` storage driver'ının aktif olduğunu doğrula (`docker info | grep Storage`). `vfs` görüyorsan disk kullanımı patlar.

### Asset Cache Server (önemli!)

Oyuncular ilk bağlanışta tüm asset'leri indirir. 2 GB'lık bir sunucuda bu, ev bağlantını öldürür. Çözüm: nginx ile cache proxy.

`server.cfg` içinde:
```cfg
set sv_forceIndirectListing true
set sv_listingIPOverride "SUNUCU_IP"
fileServer "https://cdn.senindomainin.com/files"
```

Ya da hazır çözüm: **FiveM Cache Server** (`https://github.com/blattersturm/fivem-cacheserver`) veya Cloudflare önünde bir nginx `proxy_cache`.

---

## 3. Ücretsiz Assetler

Hepsi legal / açık kaynak. **Leak site kullanma** — hem yasal risk hem backdoor riski.

### Framework'ler
| İsim | Link | Not |
|---|---|---|
| **QBCore** | github.com/qbcore-framework | En popüler RP framework'ü, dev topluluk |
| **ESX Legacy** | github.com/esx-framework/esx_core | Klasik, Türkiye'de çok yaygın |
| **Qbox** | github.com/Qbox-project | QBCore'un modernize edilmiş fork'u — **yeni sunucu için önerim bu** |
| **vRP** | github.com/vRP-framework/vRP | Eski ama hâlâ kullanılıyor |

### Overextended (ox) — kalite standardı
Modern FiveM'in temel taşları, hepsi MIT/LGPL:
- `ox_lib` — UI, context menu, notification, callback kütüphanesi
- `ox_inventory` — sektörün en iyi ücretsiz envanteri
- `ox_target` — third-eye hedefleme
- `ox_doorlock` — kapı kilit sistemi
- `oxmysql` — MySQL bağlayıcı (zorunlu)
- `ox_fuel`, `ox_core`
→ `https://github.com/overextended`

### Olmazsa olmaz ücretsiz scriptler
- **pma-voice** — voice chat (proximity + radio). `github.com/AvarianKnight/pma-voice`
- **screenshot-basic** — Discord'a ekran görüntüsü loglama
- **cfx-server-data** — resmi temel resource paketi
- **jg-mechanic** (ücretsiz sürümler), **qb-target** alternatifleri
- **illenium-appearance** — karakter oluşturma menüsü (ücretsiz, çok kaliteli)
- **Renewed-Banking / Renewed-Weathersync** — `github.com/Renewed-Scripts`
- **BadgerDiscordAPI** — Discord rol → oyun içi yetki senkronu

### Harita / MLO (ücretsiz)
- **GTA5-Mods.com** — "Maps" ve "FiveM" kategorisi, lisansı okuyarak indir
- **Forum: forum.cfx.re > Releases** — her gün ücretsiz MLO paylaşılır
- **Gabz** bazı MLO'ları ücretsiz bırakır (Patreon dışı)
- **KeepItRP / Andyyy** — ücretsiz alternatifleri var

### Araç / EUP
- **GTA5-Mods > Vehicles** (redistribution izni olanları seç)
- **EUP for FiveM** — `github.com/hexcell/eup-for-fivem`
- **Vespura'nın vMenu**'sü — admin/oyuncu menüsü, tamamen ücretsiz: `github.com/TomGrobbe/vMenu`

### Lisans uyarısı
Her asset'in `LICENSE` dosyasını oku. Bazı "ücretsiz" paketler **escrow** (şifreli) gelir ve sunucun kapanabilir. `resource.lua` içinde `escrow_ignore` görüyorsan kaynağını doğrula. Bilinmeyen `.lua` dosyalarında `PerformHttpRequest` ile bilinmedik URL'lere veri gönderen kod ara — backdoor'lar genelde böyle olur.

---

## 4. Sunucu Planı

Yeni sunucuların %95'i **ilk 30 günde ölür**. Sebep teknik değil: kimlik yokluğu + boş sunucu kısır döngüsü. Plan buna göre kurgulanmalı.

### 4.1 Konsept seçimi — "başka bir serious RP" açma

Piyasada 500 tane aynı QBCore RP var. Ayrışmak için üç yoldan biri:

**Seçenek A — Niş RP (önerim)**
Dar ama net bir tema: *Türkiye temalı şehir RP* (Türk plakaları, Türk araçları, Türkçe MLO tabelaları), *90'lar RP*, *sadece kırsal/köy RP*, *tek meslek odaklı* (yalnız polis/ambulans academy). Küçük hedef kitle = kolay #1 olma.

**Seçenek B — Düşük giriş engelli "yarı-RP"**
Whitelist yok, başvuru yok, 2 dakikada içeri gir. Aktif oyuncu sayısını hızlı yükseltir. Kural seti kısa: 5 madde, hepsi tek ekranda.

**Seçenek C — Minigame / Roleplay dışı**
Drift, drag race, zombie survival, TDM, prop hunt, hide & seek. Çok daha az script gerekir, çok daha hızlı büyür, moderasyon yükü düşüktür. **Yeni başlayan için en kârlı yol budur** ve buradan RP'ye geçiş yapabilirsin.

### 4.2 Teknik kurulum planı (2 haftalık)

**Hafta 1 — İskelet**
- Gün 1-2: Debian + txAdmin + Qbox/QBCore temel kurulum, DB
- Gün 3: `ox_lib`, `ox_inventory`, `ox_target`, `oxmysql`, `pma-voice`
- Gün 4: `illenium-appearance` + spawn + multichar
- Gün 5: Temel meslekler (polis, sağlık, mekanik) + 3-4 yasal iş
- Gün 6-7: Ekonomi dengesi — **bu en kritik kısım**. Saatlik kazanç / araç fiyatı oranını bir tabloya yaz, rastgele belirleme.

**Hafta 2 — Cila**
- Gün 8-9: 2-3 kaliteli MLO (havalimanı/PD/hastane), harita cilası
- Gün 10: Anti-cheat + logging (`FiveGuard` ücretsiz katman veya `txAdmin` + Discord log webhook'ları)
- Gün 11: Discord sunucusu (aşağıdaki yapı)
- Gün 12: Performans testi — `resmon 1`, tüm resource'lar 0.05ms altında olmalı boştayken
- Gün 13-14: 10-15 kişilik **kapalı beta**, arkadaş çevresi, bug avı

### 4.3 Oyuncu çekme stratejisi (asıl iş burada)

**Boş sunucu problemini kır.** Kimse 0/64 sunucuya girmez. Çözüm:
- Açılışı **duyurulmuş bir tarih ve saatte** yap, 30+ kişiyi aynı anda içeri sok. "Açılış gecesi etkinliği" — ödüllü drift yarışı, banka soygunu event'i.
- İlk 2 hafta **her akşam 20:00-24:00 sabit event** yap. Oyuncu, ne zaman girerse birini bulacağını bilmeli.

**Görünürlük kanalları (ücretsiz):**
1. **Sunucu listesi optimizasyonu** — `sv_projectName` ve `sv_projectDesc` içine anahtar kelimeler, tag'ler: `sets tags "roleplay, turkce, drift, economy"`. Türkçe oyuncular listeyi "turkish/türkçe" ile filtreler.
2. **TikTok / YouTube Shorts** — FiveM klipleri absürt derecede iyi çalışır. Haftada 3-4 kısa klip, sunucu adı watermark. En ucuz oyuncu edinme kanalı budur.
3. **Küçük yayıncılar** — 20-200 izleyicili Twitch/Kick yayıncılarına **öncelikli whitelist + özel araç** teklif et. Büyük yayıncı peşinde koşma, cevap vermez. 10 küçük yayıncı > 1 büyük.
4. **Discord tanıtım sunucuları** — aşağıdaki bölüm.
5. **Reddit** — r/FiveM (kurallara dikkat), r/FiveMServers
6. **Cfx.re forum** — "Server Bazaar" bölümünde tanıtım postu.

**Tutma (retention) — çekmekten daha önemli:**
- Yeni oyuncuya **ilk 5 dakikada bir hedef** ver: hoş geldin görevi, başlangıç parası, rehber NPC.
- **Haftalık içerik takvimi** duyur. Oyuncular bir sonraki şeyi bekliyorsa geri gelir.
- Discord'da **oyuncu isimleriyle** konuş. Küçük sunucunun tek avantajı budur, kullan.
- Yönetici davranışı sunucuyu tek başına öldürür. Yetkilileri arkadaşlarından değil, olgunluğa göre seç. **Yetkiliye araç/para verme** — sunucuları öldüren #1 sebep budur.

### 4.4 Discord sunucu yapısı

```
📢 duyurular / güncellemeler / bakım
📜 kurallar / rehber / sss
🎫 destek-talebi (ticket) / şikayet / ban-itiraz
💬 genel-sohbet / klip-paylaşımı / ekran-görüntüsü
🎭 rp-ilanı (iş ilanları, çete duyuruları)
🔊 sesli kanallar (5-6 tane, isimlendirilmiş)
🤖 log kanalları (yetkiliye özel: kill, para, item, ban logları)
```
Botlar: `txAdmin Discord bot` (durum + restart bildirimi), ticket botu (Ticket Tool), doğrulama botu.

---

## 5. Discord

> **Uyarı:** Discord davet linkleri sürekli değişir/expire olur. Aşağıda **isimleri ve nasıl bulacağını** veriyorum — linki uydurmuyorum. Discord'un kendi **Discovery** özelliğinden (sol altta pusula ikonu) sunucu adını aratarak resmi olanlara ulaşabilirsin.

### Geliştirme / destek toplulukları (script ve yardım için)
| Topluluk | Nasıl bulunur | Ne işine yarar |
|---|---|---|
| **Cfx.re Official** | forum.cfx.re ana sayfasındaki Discord linki | Resmi FiveM topluluğu, artifact duyuruları |
| **QBCore Framework** | github.com/qbcore-framework README'sindeki davet | Framework desteği, ücretsiz script paylaşımı |
| **Overextended (ox)** | overextended.dev / GitHub org README | ox_inventory & ox_lib desteği |
| **Qbox Project** | github.com/Qbox-project README | Modern framework desteği |
| **txAdmin** | github.com/tabarra/txAdmin README | Panel desteği |
| **Pterodactyl** | pterodactyl.io alt kısmındaki Discord | Panel/Wings sorunları |

Bunlar **script bulma ve öğrenme** için. Buralarda sunucu reklamı yapma — anında ban yersin.

### Oyuncu bulma / tanıtım toplulukları

Bunlar **advertisement** kanalı olan sunucu listeleme toplulukları. Aramak için:

1. **disboard.org** → `fivem` etiketi ile ara. Türkçe için `fivem türkçe`. Buradaki çoğu sunucunun kendi Discord'unda `#server-ads` kanalı var.
2. **top.gg / discords.com / discadia.com** → aynı şekilde `fivem` etiketi.
3. Discord içi arama (Discovery): şu kelimeleri ara:
   - `FiveM Server Advertising`
   - `FiveM Hub`
   - `FiveM Türkiye`
   - `Roleplay Advertising`
   - `GTA RP Community`
4. **r/FiveMServers** ve **r/FiveM** subreddit'lerinin sidebar'ındaki Discord sunucuları — bunlar en canlı İngilizce oyuncu havuzlarıdır.
5. **Türkiye özelinde:** Türk FiveM ekosisteminin merkezi Discord + TikTok'tur, forum kültürü zayıftır. TikTok'ta `#fivemtürkiye` etiketi altındaki hesapların bio'sundaki Discord linkleri en isabetli oyuncu havuzudur. Ayrıca büyük Türk RP sunucularının Discord'larında genelde `#diğer-sunucular` / `#reklam` kanalları bulunur.

**Reklam yaparken:**
- Her yere aynı kopyala-yapıştır metni atma, spam olarak işaretlenirsin.
- Metin şablonu: **tek cümlelik konsept** + 3 madde ayırt edici özellik + 1 görsel/klip + Discord linki. Uzun manifesto kimse okumaz.
- Görsel şart. Görselsiz ilan tıklanmaz.
- Her tanıtım sunucusunun kural kanalını oku; bazıları saatte 1 posta izin verir, ihlal edersen kalıcı ban.

---

## Hızlı Kontrol Listesi

- [ ] Debian 12 + firewall + ayrı `fivem` kullanıcısı
- [ ] CPU governor = performance
- [ ] MariaDB + oxmysql çalışıyor
- [ ] txAdmin panel erişimi (40120) + güçlü şifre
- [ ] FXServer LATEST RECOMMENDED artifact
- [ ] Framework (Qbox/QBCore/ESX) + ox paketi kurulu
- [ ] `resmon 1` boşta toplam < 2.00 ms
- [ ] Asset cache / CDN yapılandırıldı
- [ ] Anti-cheat + Discord logging aktif
- [ ] Otomatik yedek (DB + `resources/` günlük)
- [ ] Discord kuruldu, ticket sistemi var
- [ ] Açılış tarihi duyuruldu, 30+ kişi hazır
- [ ] İlk 2 haftalık event takvimi yazıldı
