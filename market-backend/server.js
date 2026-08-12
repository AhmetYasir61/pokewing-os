/**
 * PokeFace cosmetic market — catalog, entitlements and publishing.
 *
 * What this service is responsible for:
 *   GET  /api/cosmetic.json   the catalog the mod reads (the baked-in URL)
 *   GET  /api/download/:id    the pack itself, only for someone who owns it
 *   POST /api/publish         a creator uploading a model with an asking price
 *   POST /api/tebex/webhook   Tebex telling us a payment completed
 *
 * What it is deliberately NOT responsible for: taking money. Checkout happens on
 * Tebex's own pages, so no card detail ever reaches this process, its logs or
 * its disk. Ownership arrives here as a webhook after the fact, which is the
 * only part of a payment this service needs to know about.
 *
 * Storage is JSON files under ./data. That is enough for a catalog of cosmetics
 * and keeps the service to one dependency; swap `store` for a database when the
 * listing count makes that worth doing.
 */

import express from 'express';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const DATA_DIR = path.join(__dirname, 'data');
const PACK_DIR = path.join(DATA_DIR, 'packs');
const CATALOG_FILE = path.join(DATA_DIR, 'catalog.json');
const OWNERS_FILE = path.join(DATA_DIR, 'entitlements.json');

const PORT = process.env.PORT || 8080;
/** Shared secret Tebex signs its webhooks with. Required in production. */
const TEBEX_SECRET = process.env.TEBEX_WEBHOOK_SECRET || '';
/** Token a creator must present to publish. Required in production. */
const PUBLISH_TOKEN = process.env.PUBLISH_TOKEN || '';
const PUBLIC_URL = process.env.PUBLIC_URL || 'https://pokewing.com';
const MAX_PACK_BYTES = 25 * 1024 * 1024;

fs.mkdirSync(PACK_DIR, { recursive: true });

const store = {
  read(file, fallback) {
    try {
      return JSON.parse(fs.readFileSync(file, 'utf8'));
    } catch {
      return fallback;
    }
  },
  write(file, value) {
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(value, null, 2));
  },
};

const readCatalog = () => store.read(CATALOG_FILE, []);
const readOwners = () => store.read(OWNERS_FILE, {});

/**
 * A player's identity for entitlement purposes. The mod sends the store session
 * token it was given; anything unrecognised is simply "not signed in", which is
 * a valid state — the catalog is public, only downloads are not.
 */
function playerOf(req) {
  const header = req.get('authorization') || '';
  const token = header.startsWith('Bearer ') ? header.slice(7).trim() : '';
  if (!token) return null;
  const owners = readOwners();
  return owners.tokens?.[token] || null;
}

function owns(player, itemId) {
  if (!player) return false;
  const owners = readOwners();
  return (owners.players?.[player] || []).includes(itemId);
}

const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: '1mb' }));

/**
 * The catalog. Public, so the market lists and prices without a sign-in; a
 * download URL is attached only to the items the caller actually owns, which is
 * what turns the mod's button from "Buy" into "Install".
 */
app.get('/api/cosmetic.json', (req, res) => {
  const player = playerOf(req);
  const items = readCatalog()
    .filter((item) => item.published !== false)
    .map((item) => ({
      id: item.id,
      name: item.name,
      author: item.author || '',
      description: item.description || '',
      price: item.price,
      priceLabel: item.priceLabel || formatPrice(item.price, item.currency),
      checkoutUrl: item.checkoutUrl || '',
      fileName: item.fileName,
      // Withheld until it is paid for; the download route checks again anyway.
      downloadUrl: owns(player, item.id) ? `${PUBLIC_URL}/api/download/${item.id}` : '',
    }));
  res.json(items);
});

function formatPrice(amount, currency) {
  try {
    return new Intl.NumberFormat('tr-TR', {
      style: 'currency',
      currency: currency || 'TRY',
    }).format(amount);
  } catch {
    return `${amount} ${currency || ''}`.trim();
  }
}

/** The pack itself. Checked here too, so a guessed URL is not a free cosmetic. */
app.get('/api/download/:id', (req, res) => {
  const player = playerOf(req);
  const item = readCatalog().find((entry) => entry.id === req.params.id);
  if (!item) return res.status(404).json({ error: 'unknown item' });
  if (!owns(player, item.id)) return res.status(402).json({ error: 'not purchased' });

  const file = path.join(PACK_DIR, `${item.id}.zip`);
  if (!fs.existsSync(file)) return res.status(404).json({ error: 'pack missing' });
  res.type('application/zip').sendFile(file);
});

/**
 * A creator publishing a model. The mod posts the zip as the body with the name,
 * price and currency in headers. Listings arrive unpublished: a human decides
 * what goes on sale, which is the only sane default for user-uploaded content.
 */
app.post(
  '/api/publish',
  express.raw({ type: 'application/zip', limit: MAX_PACK_BYTES }),
  (req, res) => {
    const token = (req.get('authorization') || '').replace('Bearer ', '').trim();
    if (PUBLISH_TOKEN && token !== PUBLISH_TOKEN) {
      return res.status(401).json({ error: 'publishing requires a token' });
    }
    const name = (req.get('x-pokeface-name') || '').trim();
    const price = Number(req.get('x-pokeface-price') || '0');
    const currency = (req.get('x-pokeface-currency') || 'TRY').trim();
    if (!name || !Number.isFinite(price) || price < 0) {
      return res.status(400).json({ error: 'name and a valid price are required' });
    }
    if (!req.body?.length) return res.status(400).json({ error: 'empty upload' });

    const id = slug(name) + '-' + crypto.randomBytes(3).toString('hex');
    fs.writeFileSync(path.join(PACK_DIR, `${id}.zip`), req.body);

    const catalog = readCatalog();
    catalog.push({
      id,
      name,
      author: req.get('x-pokeface-author') || '',
      description: '',
      price,
      currency,
      fileName: `${slug(name)}.zip`,
      checkoutUrl: '',
      published: false,
      submittedAt: new Date().toISOString(),
    });
    store.write(CATALOG_FILE, catalog);
    res.status(201).json({ id, status: 'submitted for review' });
  },
);

const slug = (value) =>
  value
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .slice(0, 40) || 'cosmetic';

/**
 * Tebex webhook: a payment completed, so grant the packages it covered.
 *
 * The signature is verified before anything is granted — an unauthenticated
 * "they paid, honest" endpoint would hand out every cosmetic in the catalog.
 */
app.post('/api/tebex/webhook', express.raw({ type: '*/*' }), (req, res) => {
  const raw = req.body?.toString('utf8') || '';
  if (!verifyTebex(req, raw)) return res.status(401).json({ error: 'bad signature' });

  let payload;
  try {
    payload = JSON.parse(raw);
  } catch {
    return res.status(400).json({ error: 'bad json' });
  }

  // Tebex sends a validation ping when the endpoint is first saved.
  if (payload.type === 'validation.webhook') {
    return res.json({ id: payload.id });
  }

  const player = payload?.subject?.customer?.username_id
    || payload?.subject?.customer?.username
    || payload?.subject?.username;
  const packages = payload?.subject?.products || payload?.subject?.packages || [];
  if (!player || packages.length === 0) return res.json({ ok: true });

  const owners = readOwners();
  owners.players = owners.players || {};
  const owned = new Set(owners.players[player] || []);
  for (const pkg of packages) {
    const id = pkg.custom?.cosmeticId || pkg.id;
    if (id) owned.add(String(id));
  }
  owners.players[player] = [...owned];
  store.write(OWNERS_FILE, owners);
  res.json({ ok: true });
});

function verifyTebex(req, raw) {
  if (!TEBEX_SECRET) {
    // Refusing outright beats silently trusting anyone who finds the URL.
    console.warn('[market] TEBEX_WEBHOOK_SECRET is unset; rejecting webhooks');
    return false;
  }
  const signature = req.get('x-signature') || '';
  const digest = crypto.createHash('sha256').update(raw).digest('hex');
  const expected = crypto.createHmac('sha256', TEBEX_SECRET).update(digest).digest('hex');
  const a = Buffer.from(signature);
  const b = Buffer.from(expected);
  return a.length === b.length && crypto.timingSafeEqual(a, b);
}

/**
 * Issues the session token the mod stores. A token is just a handle to a player
 * name here — it buys nothing and pays for nothing, so a leaked one costs the
 * holder their download entitlements and nothing else.
 */
app.post('/api/session', (req, res) => {
  const player = (req.body?.player || '').trim();
  if (!player) return res.status(400).json({ error: 'player required' });
  const token = crypto.randomBytes(24).toString('hex');
  const owners = readOwners();
  owners.tokens = owners.tokens || {};
  owners.tokens[token] = player;
  store.write(OWNERS_FILE, owners);
  res.json({ token });
});

app.get('/api/health', (_req, res) => res.json({ ok: true }));

app.listen(PORT, () => {
  console.log(`[market] listening on :${PORT}`);
  if (!TEBEX_SECRET) console.warn('[market] set TEBEX_WEBHOOK_SECRET before going live');
  if (!PUBLISH_TOKEN) console.warn('[market] set PUBLISH_TOKEN before going live');
});
