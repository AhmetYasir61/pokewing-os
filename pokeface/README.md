# PokeFace — Epic Fight yüz & göz reaksiyonları (Forge 1.20.1)

Karakterinin **yüz ifadelerini, göz ve ağız hareketlerini** canlandıran Epic
Fight eklentisi. Yüz üç kaynaktan sırayla beslenir:

1. **Webcam / yüz takibi** — bir tracker veri gönderiyorsa gerçek yüz ifaden.
2. **Reaksiyonlar** — kamera yoksa veya menüden seçilmediyse: hasar alınca acı,
   Epic Fight savaş modunda / yakında düşman varken sinirlenme, canın azken
   yorgunluk, `R` çarkından elle seçtiğin ifadeler.
3. **Rastgele idle animasyonu** — hiçbiri yoksa göz kırpma, bakış kaymaları ve
   yavaş ifade sürüklenmesi. Yüz asla donmuş kalmaz.

Ağız ayrıca **Simple Voice Chat** mikrofonuna göre oynar (lip-sync), hem senin
hem karşı oyuncuların ağzı konuşurken hareket eder.

## Yüz nereye çizilir

Yüz **kafa kemiğine bağlıdır**, dünya üzerinde tahmini bir noktaya değil:

- Vanilla render'da kafa `ModelPart`'ının üzerine geçilir
  (`head.translateAndRotate`), böylece kafa nereye dönerse yüz de oraya döner.
- Epic Fight kurulu olduğunda EF'in **animasyonlu Head joint'i** kullanılır.
  Kullanılan dönüşüm, EF'in kafaya miğfer takarken kendi kullandığı
  (`PatchedHeadLayer`) dönüşümün aynısıdır:
  `modelMatrix × (scale(-1,-1,1) · headJointMatrix)`. Yani yüz EF'in savaş
  animasyonlarıyla birlikte hareket eder.
- Yüz, kafa küpünün ön yüzeyinin (−4 px) hemen dışına çizilir; başka bir mod
  kafaya katman ekleyip yüzü içeride bırakırsa `frontOffset` ile dışarı alınır.

## Neden hiçbir modu bozmaz

- **Mixin yok, renderer değiştirme yok.** Yüz, normal entity buffer'ına çizilen
  düz quad'lardan ibaret.
- **Epic Fight opsiyonel** ve sadece **reflection** ile okunur (battle mode
  sorgusu). EF kurulu değilse vanilla event'lerine düşer.
- Epic Fight kendi patched renderer'ını kullandığında yüz, Forge'un public
  `RenderPlayerEvent.Post` hook'undan çizilir; EF kurulu değilse normal bir
  `RenderLayer` kullanılır. İkisi aynı anda çizmez.
- **RealCamera / Sinytra Connector / BBS** etkilenmez.

## Kurulum

```
Forge 1.20.1 (47.x)
 ├─ Epic Fight 20.14.17   (opsiyonel, önerilen)
 ├─ Simple Voice Chat 2.6.22 (opsiyonel — ağız lip-sync için)
 └─ pokeface (bu mod)
```

## Paketindeki modlarla durum

Reflection isimleri **senin gönderdiğin jar'lara bakılarak** doğrulandı:

| Mod | Durum |
|-----|-------|
| Epic Fight 20.14.17 | `EpicFightCapabilities.getPlayerPatch` + `PlayerPatch.getPlayerMode()` üzerinden okunur (BATTLE/MINING). Çakışma yok. |
| Simple Voice Chat 2.6.22 | `ClientManager.getClient().getTalkCache()` → `isTalking(UUID)` + `getPlayerAudioLevel(UUID)`. Gerçek ses seviyesiyle lip-sync. |
| Voiceless Survival 2.0.2 | SVC eklentisi, aynı TalkCache'i besler; çakışma yok. |
| Tijon's Epic Arsenal 1.0.2 | EF silah/animasyon eklentisi; biz EF animasyonlarına dokunmuyoruz, çakışma yok. |
| Armourer's Workshop 2.1.4 | Kafaya kendi skin katmanını çizebilir. Yüz kafanın önüne çizildiği için üst üste binerse `config/pokeface-client.toml` içindeki **`frontOffset`** değerini (0.001 → 0.01 gibi) artır. AW oyuncu render'ını tamamen iptal ederse yüz çizilmez. |

Hiçbirine mixin atılmadığı, hiçbirinin renderer'ı değiştirilmediği için mod
kaldırıldığında/eklendiğinde bir şey bozulmaz.

## Kullanım

| Tuş | İş |
|-----|----|
| `R` | Reaction çarkı (8 ifade) |
| `F8` | Yüz menüsü |
| — | Kamerayı aç/kapat (varsayılan atanmamış) |

Menüye ayrıca mod listesindeki **Configure** düğmesinden de girilir.

### Menüde neler var

- **Tasarım (style):** Default, Anime, Toon, Sharp, Pixel — beş ayrı çizim tipi.
- **Göz X / Y / aralık / boyut** ve **ağız X / Y / boyut**: kendi skin'inin kafa
  dokusuna göre gözü ve ağzı piksel piksel oturtursun.
- **Renkler:** çizgi/kaş, göz, ağız içi ve diş rengi ayrı ayrı seçilir.
- **Skin kafasını kullan:** yüz, skin'in kendi kafa pikselleri üstüne çizilir.
- **Yüz kamerası** ve **Voice chat ağzı** anahtarları.
- **Piksel editörü:** kafanın ön yüzündeki 8x8 alanı elle boyarsın. Asıl amacı,
  skin'inin kendi gözlerini ten rengiyle kapatıp üst üste iki çift göz
  görünmesini engellemek — "Skin gözlerini kapat" düğmesi bunu tek tıkla yapar.
  Fırça / silgi / doldur, geri al, RGB kaydırıcıları ve ten rengi hazır
  paleti var; sağ tık her zaman siler, tekerlek grid'i büyütür. Şeffaf bırakılan
  pikseller dama tahtası olarak görünür, yani "burada skin görünsün" demektir.
- Solda canlı önizleme: **tekerlek ile yakınlaştır/uzaklaştır, sürükleyerek
  döndür** (arayüz ölçeğinden bağımsız). Altta hangi kaynağın aktif olduğu
  (kamera / reaksiyon / idle) ve Epic Fight + voice chat durumu yazar.

Ayarlar `config/pokeface/face-profile.json` dosyasına yazılır ve seni gören
oyunculara otomatik gönderilir, yani herkes aynı yüzü görür.

## Yüz kamerası kurulumu

Minecraft'ın JVM'i kameraya doğrudan erişemez, bu yüzden mod dışarıdaki bir
tracker'dan **UDP** ile blendshape verisi dinler (OpenSeeFace protokolü —
OpenSeeFace, VSeeFace ve uyumlu araçlar bunu konuşur).

```
python facetracker.py -c 0 -I 127.0.0.1 -P 11573
```

`config/pokeface-client.toml` içinden adres/port değiştirilebilir. Paket gelmezse
(kamera kapalı, tracker yok, seçilmemiş) mod **otomatik olarak** reaksiyon ve
idle animasyonlarına düşer — ayrıca bir şey yapman gerekmez.

## Doku

`assets/pokeface/textures/face/expressions.png` 8 ifade × 5 stil'lik 16x16
tile'lardan oluşur; üst yarı göz, alt yarı ağız sprite'ı. Renk kod tarafında
verildiği için doku beyaz maske olarak çizilir. Yeniden üretmek için:

```
python3 tools/gen_atlas.py
```

Kendi çizimini koyacaksan aynı düzeni koruman yeterli; kod değişikliği gerekmez.
