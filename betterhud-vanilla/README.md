# BetterHUD — Tamamen Vanilla HUD

BetterHud **2.1.0-SNAPSHOT-448** (bukkit) icin, tek bir dis kaynak
kullanmayan HUD / scoreboard / oyuncu listesi tasarimi.

## Once bilinmesi gereken iki sey

**1. Bu surumde scoreboard ve tablist modulu yok.**
Jar'in icinde sadece su yoneticiler var: `HudManager`, `LayoutManager`,
`ImageManager`, `TextManager`, `PopupManager`, `CompassManager`,
`BackgroundManager`. `plugins/BetterHud/scoreboard/` veya `tablist/`
diye bir klasor yok — koyarsan okunmaz.

Bu yuzden scoreboard ve oyuncu listesi **HUD paneli olarak** cizildi:
gorsel olarak vanilla sidebar / tab kutusuyla ayni yerde ve ayni
ritimde duruyor, ama ekrana BetterHud tarafindan yaziliyor.

**2. "Hicbir resource olmasin" tam olarak nasil karsilandi.**
BetterHud ekrana yaziyi kendi urettigi paketle basar — bu pluginin
calisma prensibi, kapatilamaz. Karsilanan sey su: **o pakete disaridan
tek bir dosya girmiyor.**

- `images:` hic kullanilmadi → 0 adet PNG
- `heads:` hic kullanilmadi → skin/texture cekilmiyor
- `background:` kullanilmadi → arka plan texture'i yok
- font olarak `merge-default-bitmap: true` + `use-unifont: true` →
  harfler Minecraft'in kendi `ascii.png` / `accented.png` /
  `nonlatin_european.png` ve unifont verisinden geliyor

Yani uretilen pakette custom texture bulunmaz, sadece vanilla
glyph'lerin yeniden paketlenmis hali olur. Can/aclik/zirh bar'lari
sayi olarak gosterildi — bar cizmek PNG isterdi, vanilla kalpler
zaten yerinde duruyor.

## Kurulum

1. Dosyalari kopyala:

   ```
   betterhud-vanilla/texts/vanilla-font.yml    -> plugins/BetterHud/texts/
   betterhud-vanilla/layouts/vanilla-layout.yml -> plugins/BetterHud/layouts/
   betterhud-vanilla/huds/vanilla-hud.yml      -> plugins/BetterHud/huds/
   ```

2. **Varsayilan PNG'li tasarimi kapat.** Jar ile gelen `default/`
   klasoru `health_bar.png`, `hunger_bar.png`, entity kafalari gibi
   custom texture'lar iceriyor. Bunlar durdukca pakete girerler.
   Plugin, adi tire ile baslayan dosyalari atlar
   (`default/huds/-All file prefixed with a hyphen will be skipped.yml`),
   yani en temizi klasoru komple silmek:

   ```
   rm -rf plugins/BetterHud/default
   ```

3. `plugins/BetterHud/config.yml` icinde:

   ```yaml
   default-hud:
     - vanilla_hud          # 'test_hud' idi, degistir
   default-popup: []
   default-compass: []
   load-minecraft-default-textures: false
   remove-default-hotbar: false   # vanilla hotbar dursun
   disable-legacy-offset: true    # varsayilan; ASAGIYI OKU
   ```

   `disable-legacy-offset: true` iken `PixelLocation.hotBarHeight`
   sifir doner, yani layout konumlarina gizli bir kayma eklenmez.
   `false` yaparsan tum layout'lar eski hotbar ofseti kadar kayar ve
   buradaki y degerlerini yeniden ayarlaman gerekir.

4. `/hud reload`

## GUI Scale 1 / 2 / 3 / 4 uyumu

Konumlandirma iki katmanli:

| Katman | Alan | Birim |
|---|---|---|
| Capa | `huds/*.yml` → `gui.x`, `gui.y` | ekranin **yuzdesi**, 0-100 |
| Ince ayar | `huds/*.yml` → `pixel`, `layouts/*.yml` → girdinin `x`/`y`'si | GUI pikseli |

Layout icinde `loc:` diye bir alt-anahtar **yoktur** — `HudLayout`
dogrudan `PixelLocation(yamlObject)` cagirdigi icin `x`/`y` girdinin
kokunde durur (plugin'in kendi `health` / `hunger` layout'larindaki
gibi). Alt bolum acarsan degerler sessizce yok sayilir.

Capa degeri uretilen shader'a birebir su sekilde giriyor:

```glsl
xGui = ui.x * <gui.x> / 100.0;
yGui = ui.y * <gui.y> / 100.0;
pos.x += xGui;
pos.y += yGui;
```

`ui`, GUI uzayindaki ekran boyutu — yani deger cozunurluge degil orana
bagli. `gui.y: 0` ust kenar, `gui.y: 100` alt kenar.

**Y yonu: `+y` asagi, `-y` yukari.** Plugin'in kendi ornegindeki gibi:
`gui.y: 100` + `pixel.y: -80` = alt kenardan 80 px yukari. Layout
icindeki satirlar da bu yuzden 0, 10, 22, 32... diye **artarak** gider.

Piksel ofset GUI birimi cinsinden oldugu icin scale ile birlikte buyur.
Yuzde capa + GUI pikseli birlikte, panelin her scale'de ayni oransal
noktada ve yaziyla ayni oranda kalmasini saglar.

Ayrica Scale 4'un dar efektif cozunurlugu (≈640x360) icin:

- hicbir panel ekranin ~%25'inden genis degil
- scoreboard dikeyde ortalandi (11 satir ≈ 110 px), ust/alt tasmaz
- kenar bosluklari 4 px birakildi
- font `scale` degeri elle degistirilmedi — vanilla yazi boyutu

`scale:` degerini buyutursen bu garanti bozulur, dokunma.

## Kullanilan placeholder'lar

BetterHud'un kendi placeholder'lari `[koseli parantez]` icinde,
eklenti disi olanlar `%yuzde%` icindedir.

### Dahili (ek plugin gerekmez)

`[name]` `[world]` `[level]` `[exp]` `[health]` `[max_health]`
`[food]` `[air]` `[max_air]` `[armor]` `[absorption]` `[gamemode]`
`[health_percentage]` `[hotbar_slot]` `[dead]` `[burning]` `[frozen]`

### Vault (ekonomi)

`[money]` — Vault + bir ekonomi plugini gerekir. Sunucuda Vault yoksa
`layouts/vanilla-layout.yml` icindeki `vanilla_economy` layout'unu ve
scoreboard'un 7. satirini sil.

### PlaceholderAPI — `[papi:xxx]`

**Yuzde isaretiyle yazma.** BetterHud ham `%xxx%` metnini hic islemez,
oldugu gibi ekrana basar. PAPI placeholder'lari `papi` adiyla string
container'a `requiredArgsLength(1)` ile kayitlidir; yani tek argumanli
ve koseli parantez icinde `:` sonrasina yazilir:

```
%vault_eco_balance_formatted%   ->  [papi:vault_eco_balance_formatted]
%server_online%                 ->  [papi:server_online]
```

| Kullanilan | Ne verir | Gereken expansion |
|---|---|---|
| `[papi:vault_eco_balance_formatted]` | bakiye | Vault |
| `[papi:server_online]` | gercek online oyuncu sayisi | Server |
| `[papi:server_max_players]` | slot | Server |
| `[papi:player_ping]` | ping | Player |
| `[papi:playerlist_players_N]` | listedeki N. oyuncu | PlayerList |

```
/papi ecloud download Vault
/papi ecloud download Server
/papi ecloud download Player
/papi ecloud download PlayerList
/papi reload
```

Ekonomi icin Vault'un dahili `[money]` placeholder'i da vardir ama bu
sunucuda deger dondurmedi, o yuzden her yerde PAPI surumu kullanildi.

## Yazi boyu — `scale: 8`

`texts/vanilla-font.yml` icindeki `scale: 8` satirini silme, yoksa yazi
**tam 2 kati** buyur.

Plugin'in kendi `font.yml`'i `scale: 16` kullanir ama varsayilan font
yolunda `8.0 / scale` oraninda bir kucultme uygular: 16 px rasterize
edilip 8 px cizilir. `texts/` altinda kendi tanimladigin fontlarda bu
bolme **yoktur** — `scale` kac ise o kadar piksel cizilir. Varsayilan
16 oldugu icin, degeri yazmazsan 16 px yazi alirsin.

`scale: 8` -> 8 px rasterize, 8 px cizim = vanilla yazi boyu.

Font scale'ini degistirirsen `layouts/` icindeki 10 px'lik satir
araliklarini da ayni oranda olceklemen gerekir.
