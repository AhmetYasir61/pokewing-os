# SpotifyWorldMusic

Belirli dünyalarda ve bölgelerde (spawn, hub, nether…) bir Spotify bağlantısını
**döngüde** çalan Paper/Spigot eklentisi. Minecraft **1.21.1**, Java 21.

```
/swm player   -> tarayıcı oynatıcı adresini al
/swm link     -> Spotify hesabını bağla (API modu)
/swm status   -> durum
/swm reload   -> config.yml'i yeniden yükle (yetkili)
```

## Önce şunu bilmek gerekiyor

Minecraft istemcisi Spotify sesini oyunun içinde çalamaz — Spotify'ın lisansı
ve istemci API'si buna izin vermiyor. "Oyuna Spotify koydum" diyen her çözüm,
sesi ya bir tarayıcıdan ya da oyuncunun kendi Spotify uygulamasından çalar.
Bu eklenti de ikisini birden yapar ve **hangi parçanın ne zaman çalacağına
sunucu karar verir**:

| Mod | Nasıl çalar | Kimde çalışır |
|-----|-------------|---------------|
| `WEB` | Oyuncu `/swm player` ile aldığı adresi tarayıcıda açık tutar; sayfa gömülü Spotify oynatıcısını sunucunun söylediği parçaya göre yönetir ve döngüde tutar | Herkes. Tarayıcıda Spotify oturumu açık olanlar parçayı **tam**, olmayanlar ~30 sn önizleme duyar |
| `API` | Sunucu, Spotify Web API üzerinden oyuncunun **kendi** Spotify uygulamasında (telefon/masaüstü) parçayı başlatır, `repeat` modunu açar | `/swm link` ile hesabını bağlamış **Spotify Premium** kullanıcıları |
| `BOTH` | Hesabını bağlayan API ile, diğerleri tarayıcı oynatıcı ile dinler | Varsayılan |

## Kurulum

1. `gradle build` → `build/libs/SpotifyWorldMusic-1.0.0.jar` dosyasını
   sunucunun `plugins/` klasörüne at, sunucuyu başlat.
2. `plugins/SpotifyWorldMusic/config.yml` içinde bölgeleri düzenle.
3. `web.public-url` alanına oyuncuların erişebileceği adresi yaz
   (ör. `http://oyun.sunucum.net:8787`) ve o portu firewall'da aç.
4. `/swm reload`.

Sadece tarayıcı modunu kullanacaksan Spotify Developer hesabı gerekmez —
adım 5'i atla.

5. **API modu için:** https://developer.spotify.com/dashboard adresinden bir
   uygulama oluştur, `client-id` / `client-secret` alanlarını doldur ve
   `redirect-uri`'yi **hem config'e hem Spotify panelindeki "Redirect URIs"
   listesine aynı şekilde** yaz. Oyuncular `/swm link` yazıp açılan sayfada
   izin verir.

   > ⚠️ **Spotify redirect URI kuralı:** yalnızca `https://` adresleri veya açık
   > loopback adresi (`http://127.0.0.1:PORT`, `http://[::1]:PORT`) kabul edilir.
   > Düz `http://` ile bir IP/alan adı — örneğin `http://1.2.3.4:8787/callback` —
   > Spotify panelinde **kaydedilemez**, `localhost` da kabul edilmez. Yani API
   > modu için `/callback` yolunu HTTPS ile dışarı açman gerekir; pratikte bir
   > alan adı + Nginx/Caddy ters vekil:
   > `https://muzik.sunucum.net/callback` → `127.0.0.1:8787/callback`.
   > Eklenti bu kuralı ihlal eden bir değeri açılışta ve `/swm status`'ta uyarır.

## Sorun giderme

`/swm status` (yetkiliyseysen) modu, bölge sayısını, web dinleyicisinin çalışıp
çalışmadığını, public URL'i ve API modunun **neden** kapalı olduğunu tek ekranda
gösterir. Aynı bilgiler sunucu açılışında ve `/swm reload` sonrasında konsola da
yazılır.

"Spotify hesap bağlama şu an kapalı" mesajının tipik sebepleri: `config.yml`
kaydedilmeden `/swm reload` çalıştırılması, `mode: WEB` bırakılmış olması ya da
üç Spotify alanından birinin boş olması — `/swm status` hangisi olduğunu söyler.

## Bölge tanımı

```yaml
zones:
  spawn:
    enabled: true
    display-name: "&aSpawn Teması"
    world: world
    region:
      enabled: true          # false -> dünyanın tamamı
      center: { x: 0, y: 64, z: 0 }
      radius: 120
      ignore-y: true         # yükseklik farkını yok say (silindir)
    url: "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"
    loop: true
    shuffle: false
    volume: 55               # yalnızca API modunda uygulanır
    priority: 10             # üst üste binen bölgelerde yüksek olan kazanır
```

`url` alanına şunların hepsi yazılabilir:
`https://open.spotify.com/playlist/<id>`, `https://open.spotify.com/intl-tr/track/<id>`,
`spotify:album:<id>`, ayrıca `artist`, `episode`, `show`.

Üst üste binen bölgelerde önce yüksek `priority`, eşitlikte `region` tanımlı
olan kazanır — yani "tüm dünyada X, spawn çevresinde Y" ayrı ayarlama
gerektirmeden çalışır.

## Döngü (loop)

- **Tarayıcı modunda:** sayfa parçanın bittiğini yakalar; tek parçaysa başa
  sarıp devam eder, playlist/albümse listeyi baştan yükler.
- **API modunda:** tek parça için `repeat=track`, playlist/albüm için
  `repeat=context` ayarlanır — çalma listesi kendiliğinden döner.

## Aynı bölgedekiler aynı yeri duysun (`web.sync-position`)

Sunucu her bölge için bir "başlangıç saati" tutar; bölgeye sonradan giren bir
oyuncunun tarayıcısı parçanın o anki yerine atlar. Bu yalnızca **tek parça**
(`track`/`episode`) için uygulanır: playlist/albümde parça sınırlarını sunucu
bilemediği için baştan başlatılır. Kapatmak için `web.sync-position: false`.

## Komutlar ve yetkiler

| Komut | Ne yapar | Yetki |
|-------|----------|-------|
| `/swm player` | Oyuncuya özel tarayıcı oynatıcı adresini verir | `swm.use` (varsayılan: herkes) |
| `/swm link` | Spotify hesabını bağlar (10 dk geçerli tek kullanımlık bağlantı) | `swm.use` |
| `/swm unlink` | Bağlantıyı kaldırır | `swm.use` |
| `/swm status` | Bulunduğu bölgeyi, parçayı ve bağlantı durumunu gösterir | `swm.use` |
| `/swm resync` | Müziği yeniden başlatır | `swm.use` |
| `/swm zones` | Tanımlı bölgeleri listeler | `swm.admin` (varsayılan: op) |
| `/swm reload` | `config.yml`'i yeniden yükler | `swm.admin` |

## Dosyalar

| Dosya | İçerik |
|-------|--------|
| `config.yml` | Mod, web sunucusu, Spotify uygulaması ve bölgeler |
| `links.yml` | Oyuncu UUID → Spotify refresh token. **Gizli tutulmalı** |
| `data.yml` | Tarayıcı oynatıcı adreslerini imzalayan sunucu anahtarı |
| `web/player.html` | Tarayıcı sayfası. Buradaki kopya düzenlenirse jar yerine o kullanılır |

## Bilinen sınırlar

- Tarayıcı sekmesi kapalıyken ses çalmaz; oyuncunun sekmeyi açık tutması gerekir.
- Tarayıcılar kullanıcı etkileşimi olmadan ses başlatmaz, bu yüzden sayfada bir
  kez "Müziği başlat" düğmesine basmak gerekir.
- API modu Spotify Premium ister ve oyuncunun Spotify uygulamasının açık
  (aktif cihaz) olmasını bekler; değilse eklenti oyuncuyu uyarır.
- `volume` yalnızca API modunda uygulanır — gömülü oynatıcı ses seviyesini
  dışarıdan ayarlamaya izin vermez.
- Folia desteklenmiyor.
