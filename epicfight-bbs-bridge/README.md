# Epic Fight → BBS Bridge (Forge 1.20.1)

BBS (Blockbuster Studio) ile dizi/machinima çekerken **Epic Fight'ın savaş
animasyonlarını** aktörlerine, keyframe'lerle uğraşmadan kullanmanı sağlayan
köprü mod.

## Nasıl çalışır (ve neden diğer modları bozmaz)

Bu mod Epic Fight'ın canlı render'ına **hiç dokunmaz**. Bunun yerine Epic
Fight'ın animasyon verisini (eklem başına keyframe: `AnimationClip →
TransformSheet → Keyframe → JointTransform`) okuyup, BBS'in doğrudan
oynatabileceği **Blockbench / Bedrock `.animation.json`** dosyasına çevirir.

Sonuç: mod sadece veri okur ve dosya yazar. Hiçbir mixin, hiçbir render/kamera
hook'u yoktur. Bu yüzden şunların hiçbirini bozmaz:

- **RealCamera** (birinci şahıs vücut kamerası) — render'a dokunulmuyor.
- **Sinytra Connector + Fabric API** — BBS (Fabric) ve Epic Fight (Forge) kendi
  dünyalarında çalışmaya devam eder; köprü ikisini runtime'da birbirine
  bağlamaz, sadece dosya üretir.
- **EF Plus / Pierced Animations** gibi resource pack'ler — oyun-içi
  `/efbbs exportall` komutu **yüklü** animasyon kaydını okuduğu için bu
  paketlerin animasyonlarını da dışa aktarır.

Epic Fight bu mod için **opsiyonel bağımlılıktır**: kurulu değilse mod yine
yüklenir ve "Epic Fight yok" diye bilgi verir.

## Senin kurulumun

```
Forge 1.20.1
 ├─ Sinytra Connector (beta.46)      ← Fabric modlarını Forge'da çalıştırır
 ├─ Fabric API (0.92.2)              ← Connector'ın gereksinimi
 ├─ BBS mod 2.4 (Fabric)             ← Connector üzerinden
 ├─ Epic Fight 20.14.17 (Forge)
 ├─ RealCamera 0.7.8                 ← etkilenmez
 ├─ EF Plus / Pierced (resource pack)
 └─ efbbs (bu mod, Forge)            ← buraya
```

## Canlı kayıt (mocap) — "sen dövüş, gerisi otomatik"

Bu, senin ana iş akışın. Epic Fight ile canlı oynarken (tek başına VEYA online
arkadaşlarınla) sahnedeki herkesin pozunu ve hareketini kare kare kaydeder,
sonra her karakter için ayrı bir BBS animasyonu üretir. Keyframe yok, rig
uğraşı yok.

1. Oyunda **K** tuşuna bas (veya `/efbbs rec start`) — kayıt başlar.
2. Dövüş: kılıç savur, saldır, koş, zıpla. Yakınındaki (varsayılan 24 blok)
   **tüm** Epic Fight karakterleri (sen + arkadaşların + moblar) kaydedilir.
3. Tekrar **K** (veya `/efbbs rec stop`) — durur. Her karakter için bir
   `rec_<isim>_N.animation.json` dosyası `config/efbbs/exported/` içine yazılır.
4. BBS'te her dosyayı bir aktöre ver, zaman çizgisinde diz, epik sahneni kur.

Notlar:
- **Online çalışır**: kayıt tamamen client tarafında olur, sunucuya mod
  gerekmez. Arkadaşlarının Epic Fight animasyonlarını da yakalar.
- **Konum + dönüş dahil**: karakterin dünyada nasıl hareket ettiyse (root
  motion) ve gövde dönüşü, kök kemiğe işlenir — koşarak saldırı gibi sahneler
  korunur. Ters yöne giderse `retarget.json` içindeki `rootXSign/rootZSign`.
- **Çoklu karakter**: hepsini aynı anda tek geçişte yakalayabilirsin; ya da her
  rolü ayrı ayrı oynayıp kaydını alıp BBS'te birleştirebilirsin.
- Kayıt hızı: client tick hızı (20/sn). BBS'te akıcı görünür.

## Kullanım (hazır Epic Fight animasyonlarını dışa aktarma)

### A) Oyun içinden (önerilen — resource pack animasyonlarını da yakalar)

```
/efbbs list                 # tüm Epic Fight animasyon anahtarlarını listeler
/efbbs export epicfight:biped/combat/sword_auto1
/efbbs exportall            # hepsini birden dışa aktarır
/efbbs reloadconfig         # retarget.json'u yeniden yükler
```

Çıktılar: `config/efbbs/exported/*.animation.json`

### B) Oyun açmadan (çevrimdışı toplu dönüştürme)

Tamamen bağımsız çalışır — **sadece `efbbs.jar` yeter**, Minecraft/Forge/Gson
gerekmez. Köşeli parantez `[...]` "isteğe bağlı" demek; yazma. Örnek (tek satır):

```
java -cp efbbs-forge-1.20.1-0.2.0.jar com.pokewing.efbbs.OfflineConverter epic-fight-20.14.17-mc1.20.1-forge.jar out-klasoru
```

Kendi kemik eşleştirmenle çalıştırmak istersen sona `retarget.json` yolunu ekle:

```
java -cp efbbs-forge-1.20.1-0.2.0.jar com.pokewing.efbbs.OfflineConverter epic-fight-20.14.17-mc1.20.1-forge.jar out-klasoru retarget.json
```

Epic Fight jar'ının içindeki
`assets/epicfight/animmodels/animations/**/*.json` animasyonlarını (biped combat
dahil ~350+ animasyon) okuyup bedrock formatında yazar. Bazı özel/insansı-olmayan
(vex vb.) animasyonlar farklı formatta olduğu için atlanır.

### BBS'te oynatma

1. Aktörün için **rigli** bir model kullan (`.bbmodel` veya glTF). Statik `.obj`
   modellerin kemiği olmadığı için animasyon oynatılamaz — `.obj`'yi hareketsiz
   prop'lar için kullan.
2. Üretilen `.animation.json` dosyasını modelinin yanına koy (BBS'in geo model
   klasörü) ya da BBS'in animasyon içe aktarma akışından yükle.
3. Aktöre animasyonu seç ve oynat. Keyframe elle girmene gerek yok.

## Kemik eşleştirme (retargeting)

İlk çalıştırmada `config/efbbs/retarget.json` oluşur. Sol taraf Epic Fight'ın
biped eklem isimleri (jar'dan doğrulandı: `Root, Torso, Chest, Head,
Shoulder_R/L, Arm_R/L, Elbow_R/L, Hand_R/L, Thigh_R/L, Leg_R/L, Knee_R/L,
Tool_R/L`), sağ taraf **senin modelinin kemik isimleri**. Sağ tarafı kendi
riginin isimlerine göre düzenle.

Ek ayarlar:

| Alan | İşlev |
|------|-------|
| `translationScale` | Epic Fight birimi → model birimi (varsayılan 16 = blok→piksel) |
| `rotXSign / rotYSign / rotZSign` | Eksen ters dönüyorsa işaret çevirir (`-1`) |
| `rotationOnly` | `true` → sadece rotasyon aktarılır (humanoidler için genelde daha temiz) |
| `decimals` | Çıktı ondalık hassasiyeti |

## Dürüst sınırlama (okunması önemli)

Epic Fight'ın eklem **dinlenme yönelimi** (rest orientation) Minecraft/Blockbench
kemik uzayından farklıdır. Bu yüzden ham dönüşümde kemiklerde sabit bir açı
kayması görülebilir (ör. Root'ta ~90°). En temiz sonucu, Epic Fight'ın
iskeletiyle **aynı kemik yönelimine sahip** bir modelde alırsın; farklı riglerde
`rot*Sign` ve `rotationOnly` ile ince ayar gerekebilir. Bu v0.1 için beklenen
davranıştır — otomatik rest-pozu kalibrasyonu ilerideki sürüme bırakılmıştır.

## Derleme

```
cd epicfight-bbs-bridge
./gradlew build      # jar -> build/libs/efbbs-forge-1.20.1-0.1.0.jar
```

Epic Fight erişimi **reflection** ile yapıldığı için Epic Fight jar'ına derleme
zamanında hiç ihtiyaç yoktur — `libs/` klasörüne bir şey koymana gerek yok. Mod
Epic Fight kurulu olmadan da derlenip yüklenir.
