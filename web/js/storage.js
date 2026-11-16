// Desi Calculator AI PRO V2.0 - LocalStorage Persistence & Legacy Migration
// Developed by Hardik Kumar | © 2026 Hardik Kumar. All Rights Reserved.

const STORAGE_KEY = 'desi_calculator_ai_pro_v2';
const LEGACY_KEYS = ['hisab_customers', 'customers', 'udhar_jama_entries', 'transactions', 'hisabData', 'custom_hisab_data'];

export const DEFAULT_STATE = {
  formatVersion: 2,
  settings: {
    theme: 'premium_dark',
    customAccent: '#f59e0b',
    customBg: '#0b1324',
    language: 'en',
    businessName: 'Desi Vyapar Kendra',
    ownerMobile: '',
    profilePhotoDataUrl: '',
    aiEngineMode: 'auto',
    geminiApiKey: '',
    templates: {
      REMINDER: 'Namaste {name}, aapka {business} par kul baki hisab ₹{balance} hai. Kripya samay par jama karein.',
      UDHAR_CONFIRM: 'Namaste {name}, {business} khata mein ₹{amount} Udhar darj hua hai. Kul baki: ₹{balance}.',
      JAMA_CONFIRM: 'Namaste {name}, {business} khata mein ₹{amount} Jama prapt hua. Shesh baki: ₹{balance}.',
      BALANCE_SUMMARY: 'Hisab Summary ({business}): {name}, aapka balance ₹{balance} hai. Date: {date}.'
    }
  },
  customers: [],
  transactions: [],
  calcHistory: [],
  smsLogs: [],
  migratedLegacySources: []
};

export function loadAppState() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      const fresh = structuredClone(DEFAULT_STATE);
      migrateLegacyLocalStorageIntoState(fresh);
      saveAppState(fresh);
      return fresh;
    }
    const parsed = JSON.parse(raw);
    const merged = {
      ...structuredClone(DEFAULT_STATE),
      ...parsed,
      settings: {
        ...DEFAULT_STATE.settings,
        ...(parsed.settings || {}),
        templates: { ...DEFAULT_STATE.settings.templates, ...(parsed.settings?.templates || {}) }
      }
    };
    return merged;
  } catch (e) {
    console.error('Failed to load state, preserving safe defaults:', e);
    return structuredClone(DEFAULT_STATE);
  }
}

export function saveAppState(state) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

export function normalizeTxType(raw) {
  const s = String(raw || '').toLowerCase().trim();
  if (s === 'udhar_liya' || s === 'taken' || s === 'borrowed') return 'UDHAR_LIYA';
  if (s === 'payment_made' || s === 'paid' || s === 'payment') return 'PAYMENT_MADE';
  if (s.includes('jama') || s === 'received' || s === 'credit') return 'JAMA_RECEIVED';
  return 'UDHAR_DIYA';
}

export function migrateLegacyLocalStorageIntoState(state) {
  let importedCustomers = 0;
  let importedTx = 0;
  for (const key of LEGACY_KEYS) {
    if (state.migratedLegacySources.includes(key)) continue;
    const raw = localStorage.getItem(key);
    if (!raw) continue;
    try {
      const parsed = JSON.parse(raw);
      const res = mergeBackupObjectNonDestructive(state, parsed);
      importedCustomers += res.customersImported;
      importedTx += res.transactionsImported;
      state.migratedLegacySources.push(key);
    } catch (_) {
      // Ignore unrelated non-JSON key
    }
  }
  return { importedCustomers, importedTx };
}

export function mergeBackupObjectNonDestructive(state, payload) {
  let customersImported = 0;
  let customersSkipped = 0;
  let transactionsImported = 0;
  let transactionsSkipped = 0;

  const incomingCustomers = Array.isArray(payload)
    ? payload
    : (payload.customers || payload.hisab_customers || payload.accounts || []);
  const incomingTx = payload.transactions || payload.udhar_jama_entries || payload.entries || payload.hisabData || [];

  const legacyToId = new Map();
  const nameToId = new Map();
  state.customers.forEach(c => {
    if (c.legacyId) legacyToId.set(String(c.legacyId), c.id);
    nameToId.set(c.name.trim().toLowerCase(), c.id);
  });

  incomingCustomers.forEach((rawC, idx) => {
    const name = String(rawC.name || rawC.customerName || '').trim();
    if (!name) return;
    const legacyId = String(rawC.id || rawC.legacyId || rawC.customerId || `leg_c_${idx}`);
    const existingId = legacyToId.get(legacyId) || nameToId.get(name.toLowerCase());
    if (existingId) {
      legacyToId.set(legacyId, existingId);
      customersSkipped++;
    } else {
      const newId = `cust_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`;
      state.customers.push({
        id: newId,
        customerCode: rawC.customerCode || `CUST-${1001 + state.customers.length}`,
        name,
        mobile: String(rawC.mobile || rawC.phone || '').trim(),
        address: String(rawC.address || '').trim(),
        notes: String(rawC.notes || rawC.note || '').trim(),
        photoDataUrl: rawC.photoDataUrl || '',
        legacyId,
        createdAt: Number(rawC.createdAt) || Date.now()
      });
      legacyToId.set(legacyId, newId);
      nameToId.set(name.toLowerCase(), newId);
      customersImported++;
    }

    const nested = rawC.transactions || rawC.entries || [];
    nested.forEach((nt, j) => {
      incomingTx.push({ ...nt, customerId: legacyId, customerName: name, id: nt.id || `leg_nt_${idx}_${j}` });
    });
  });

  const existingTxLegacyIds = new Set(state.transactions.map(t => String(t.legacyId || t.id)));

  incomingTx.forEach((rawT, idx) => {
    const amount = Number(rawT.amount);
    if (!Number.isFinite(amount) || amount <= 0) return;
    const legacyTxId = String(rawT.id || rawT.legacyId || `leg_t_${idx}_${amount}`);
    if (existingTxLegacyIds.has(legacyTxId)) {
      transactionsSkipped++;
      return;
    }
    const custName = String(rawT.customerName || rawT.name || '').trim();
    let targetCustId = legacyToId.get(String(rawT.customerId || rawT.customerLegacyId || ''))
      || nameToId.get(custName.toLowerCase());

    if (!targetCustId && custName) {
      targetCustId = `cust_${Date.now()}_${idx}`;
      state.customers.push({
        id: targetCustId,
        customerCode: `CUST-${1001 + state.customers.length}`,
        name: custName,
        mobile: '',
        address: '',
        notes: 'Migrated',
        photoDataUrl: '',
        legacyId: targetCustId,
        createdAt: Date.now()
      });
      nameToId.set(custName.toLowerCase(), targetCustId);
      customersImported++;
    }
    if (!targetCustId) return;

    state.transactions.push({
      id: `tx_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
      customerId: targetCustId,
      type: normalizeTxType(rawT.type || rawT.kind),
      amount,
      description: String(rawT.description || rawT.note || '').trim(),
      isPartialPayment: Boolean(rawT.isPartialPayment),
      timestamp: Number(rawT.timestamp) || Date.now(),
      legacyId: legacyTxId
    });
    existingTxLegacyIds.add(legacyTxId);
    transactionsImported++;
  });

  return { customersImported, customersSkipped, transactionsImported, transactionsSkipped };
}

export function formatInr(amount, withSymbol = true) {
  const num = Number(amount) || 0;
  const absFormatted = Math.abs(num).toLocaleString('en-IN', { maximumFractionDigits: 2 });
  const sign = num < -0.004 ? '-' : '';
  return withSymbol ? `${sign}₹${absFormatted}` : `${sign}${absFormatted}`;
}
