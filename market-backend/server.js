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
 * A player's identity for entitlement purposes: the account UUID, resolved from
 * the session token the mod was issued.
 *
 * <p>Entitlements are keyed by UUID rather than by name on purpose. A purchase
 * has to outlive a config wipe, a reinstall and a new PC, and it must not follow
 * a name to whoever claims it next — names change hands, UUIDs do not.
 */
function playerOf(req) {
  const header = req.get('authorization') || '';
  const token = header.startsWith('Bearer ') ? header.slice(7).trim() : '';
  if (!token) return null;
  const owners = readOwners();
  const session = owners.tokens?.[token];
  if (!session) return null;
  return typeof session === 'string' ? session : session.uuid;
}

function owns(uuid, itemId) {
  if (!uuid) return false;
  const owners = readOwners();
  return (owners.players?.[uuid] || []).includes(itemId);
}

/** Grants an item permanently. Idempotent, so a replayed webhook is harmless. */
function grant(uuid, name, itemId) {
  const owners = readOwners();
  owners.players = owners.players || {};
  owners.names = owners.names || {};
  const owned = new Set(owners.players[uuid] || []);
  owned.add(String(itemId));
  owners.players[uuid] = [...owned];
  if (name) owners.names[uuid] = name;
  store.write(OWNERS_FILE, owners);
}

/** Looks up the UUID behind a Minecraft name, for webhooks that only carry one. */
async function uuidForName(name) {
  const owners = readOwners();
  const known = Object.entries(owners.names || {}).find(
    ([, value]) => String(value).toLowerCase() === String(name).toLowerCase(),
  );
  if (known) return known[0];
  try {
    const res = await fetch(`https://api.mojang.com/users/profiles/minecraft/${encodeURIComponent(name)}`);
    if (!res.ok) return null;
    const profile = await res.json();
    return dashed(profile.id);
  } catch {
    return null;
  }
}

const dashed = (id) =>
  id && id.length === 32
    ? `${id.slice(0, 8)}-${id.slice(8, 12)}-${id.slice(12, 16)}-${id.slice(16, 20)}-${id.slice(20)}`
    : id;

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
app.post('/api/tebex/webhook', express.raw({ type: '*/*' }), async (req, res) => {
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

  const customer = payload?.subject?.customer || payload?.subject || {};
  const name = customer.username?.username || customer.username || customer.name;
  const packages = payload?.subject?.products || payload?.subject?.packages || [];
  if (!name || packages.length === 0) return res.json({ ok: true });

  // Resolve to the UUID so the purchase is bound to the account, not the name
  // it happened to be bought under.
  const uuid = dashed(customer.username?.id || customer.uuid) || (await uuidForName(name));
  if (!uuid) {
    console.warn('[market] could not resolve a UUID for', name, '- purchase not granted');
    return res.status(202).json({ ok: false, reason: 'unresolved player' });
  }
  for (const pkg of packages) {
    const id = pkg.custom?.cosmeticId || pkg.id;
    if (id) grant(uuid, name, id);
  }
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
 * Signs a player in, using the same handshake a Minecraft server uses.
 *
 * <p>The mod tells Mojang it is joining a session id, then posts that id here;
 * this asks Mojang who actually joined it. That is what makes the identity
 * trustworthy: no password reaches this service, and nobody can collect someone
 * else's purchases by typing their name. A token is only a handle to the
 * resulting UUID — it buys nothing, so a leaked one costs downloads and nothing
 * more.
 */
app.post('/api/session', async (req, res) => {
  const username = (req.body?.username || '').trim();
  const serverId = (req.body?.serverId || '').trim();
  if (!username || !serverId) {
    return res.status(400).json({ error: 'username and serverId required' });
  }
  let profile;
  try {
    const url = 'https://sessionserver.mojang.com/session/minecraft/hasJoined'
      + `?username=${encodeURIComponent(username)}&serverId=${encodeURIComponent(serverId)}`;
    const response = await fetch(url);
    if (response.status !== 200) {
      return res.status(401).json({ error: 'session not verified by Mojang' });
    }
    profile = await response.json();
  } catch (err) {
    console.warn('[market] Mojang verification failed', err);
    return res.status(502).json({ error: 'could not reach Mojang' });
  }

  const uuid = dashed(profile.id);
  const token = crypto.randomBytes(24).toString('hex');
  const owners = readOwners();
  owners.tokens = owners.tokens || {};
  owners.names = owners.names || {};
  owners.tokens[token] = { uuid, name: profile.name, issuedAt: Date.now() };
  owners.names[uuid] = profile.name;
  store.write(OWNERS_FILE, owners);
  res.json({ token, uuid, name: profile.name });
});

app.get('/api/health', (_req, res) => res.json({ ok: true }));

app.listen(PORT, () => {
  console.log(`[market] listening on :${PORT}`);
  if (!TEBEX_SECRET) console.warn('[market] set TEBEX_WEBHOOK_SECRET before going live');
  if (!PUBLISH_TOKEN) console.warn('[market] set PUBLISH_TOKEN before going live');
});
