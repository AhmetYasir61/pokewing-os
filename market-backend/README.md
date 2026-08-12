# PokeWing kozmetik market — arka uç

PokeFace modunun içine gömülü adres: `https://pokewing.com/api/cosmetic.json`.
Kullanıcı hiçbir şey yazmıyor; bu servis o adresi karşılıyor.

## Uçlar

| Uç | İş |
|----|----|
| `GET /api/cosmetic.json` | Katalog. Herkese açık; **satın alınmış** ürünlere `downloadUrl` eklenir. |
| `GET /api/download/:id` | Paketin kendisi. Sahiplik burada tekrar kontrol edilir. |
| `POST /api/publish` | Üretici model yükler (zip gövde + isim/fiyat başlıkları). |
| `POST /api/tebex/webhook` | Tebex "ödeme tamamlandı" der; sahiplik burada verilir. |
| `POST /api/session` | Mojang oturum doğrulamasıyla oyuncuyu tanır, token üretir. |
| `GET /api/health` | Ayakta mı. |

## Kimlik ve kalıcı sahiplik

Satın alınan kozmetik **hesaba** bağlanır, kayıt dosyasına veya isme değil.
Sahiplik `entitlements.json` içinde **UUID** ile tutulur; oyuncu config'ini
silse, oyunu yeniden kursa, başka bilgisayara geçse de ürünleri geri gelir.
İsimle tutulmaz çünkü isimler el değiştirir.

Kimlik, bir Minecraft sunucusunun kullandığı el sıkışmanın aynısıyla doğrulanır:
mod Mojang'a "şu oturuma katılıyorum" der, bu servis Mojang'a "o oturuma kim
katıldı" diye sorar. Böylece buraya hiçbir parola gelmez ve kimse başkasının
adını yazarak onun satın aldıklarını toplayamaz. Token yalnızca UUID'ye bir
tutamaçtır — hiçbir şey satın alamaz, sızması indirme hakkından fazlasına mal
olmaz.

## Ödeme

Para bu servisten **geçmez**. Ödeme Tebex'in kendi sayfasında yapılır; buraya
sadece "şu oyuncu şu paketi aldı" webhook'u gelir. Dolayısıyla hiçbir kart
bilgisi bu sürecin belleğine, loguna veya diskine düşmez — mod tarafındaki
tasarımla aynı sınır.

Webhook imzası doğrulanmadan hiçbir sahiplik verilmez: imzasız bir uç, katalogdaki
her kozmetiği bedava dağıtmak demektir. `TEBEX_WEBHOOK_SECRET` boşsa istekler
reddedilir.

## Çalıştırma

```bash
cd market-backend
npm install
TEBEX_WEBHOOK_SECRET=... PUBLISH_TOKEN=... PUBLIC_URL=https://pokewing.com npm start
```

Nginx arkasında `/api/` yolunu bu servise ver. Veri `data/` altında JSON:
`catalog.json` (listeler), `entitlements.json` (kim neye sahip, oturum token'ları),
`packs/<id>.zip` (dosyalar). Başlangıç için `data/catalog.example.json` dosyasını
`data/catalog.json` olarak kopyala.

## Yayınlanan modeller

`POST /api/publish` ile gelen her model **`published: false`** olarak düşer;
satışa çıkmadan önce bir insan onaylar. Kullanıcı yüklemesi için tek makul
varsayılan budur. Onaylarken `catalog.json` içinde `published: true` yap ve
Tebex paketinin `checkoutUrl` adresini gir.
