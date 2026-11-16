// Desi Calculator AI PRO V2.0 - Main Web Application Controller
// Developed by Hardik Kumar | © 2026 Hardik Kumar. All Rights Reserved.

import {
  loadAppState, saveAppState, migrateLegacyLocalStorageIntoState,
  mergeBackupObjectNonDestructive, formatInr
} from './storage.js';
import {
  UNITS, evaluateMathExpression, calcGst, calcDiscount, calcProfitLoss
} from './calculator.js';
import {
  computeCustomerSummary, computeDashboardMetrics, findPossibleDuplicate,
  buildPeriodReport, buildCsvContent
} from './ledger.js';
import { runOfflineSmartEngine, runOnlineGeminiRest } from './ai_assistant.js';
import { renderSmsTemplate, launchWebSmsComposer } from './sms.js';

let state = loadAppState();
let selectedCustomerId = null;
let activeCustFilter = 'all';
let activeReportPeriod = 'daily';
let calcExpr = '';
const aiMessages = [];

const I18N = {
  en: {
    navHome: 'Home', navCalc: 'Calculator', navKhata: 'Khata',
    navAi: 'AI Smart', navSms: 'SMS & Reports', navProfile: 'Profile',
    remainingBalance: 'Remaining Balance', totalCustomers: 'Total Customers',
    totalUdhar: 'Total Udhar', totalJama: 'Total Jama', todaysEntries: "Today's Entries",
    quickCalc: 'Quick Calculator', pendingPayments: 'Pending Payments', recentHistory: 'Recent History'
  },
  hi: {
    navHome: 'होम', navCalc: 'कैलकुलेटर', navKhata: 'खाता',
    navAi: 'AI स्मार्ट', navSms: 'SMS/रिपोर्ट', navProfile: 'प्रोफ़ाइल',
    remainingBalance: 'शेष बकाया', totalCustomers: 'कुल ग्राहक',
    totalUdhar: 'कुल उधार', totalJama: 'कुल जमा', todaysEntries: 'आज का लेनदेन',
    quickCalc: 'त्वरित कैलकुलेटर', pendingPayments: 'बकाया भुगतान', recentHistory: 'हालिया इतिहास'
  },
  hinglish: {
    navHome: 'Home', navCalc: 'Calc', navKhata: 'Khata',
    navAi: 'AI Smart', navSms: 'SMS/Report', navProfile: 'Profile',
    remainingBalance: 'Baki Balance', totalCustomers: 'Kul Customers',
    totalUdhar: 'Total Udhar', totalJama: 'Total Jama', todaysEntries: 'Aaj ka Hisab',
    quickCalc: 'Quick Calc', pendingPayments: 'Pending Baki', recentHistory: 'Recent History'
  }
};

function showToast(msg) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.remove('hidden');
  clearTimeout(el._timer);
  el._timer = setTimeout(() => el.classList.add('hidden'), 2600);
}

function persistAndRender() {
  saveAppState(state);
  renderAll();
}

function applyThemeAndLanguage() {
  const s = state.settings;
  document.body.setAttribute('data-theme', s.theme);
  if (s.theme === 'custom') {
    document.body.style.setProperty('--primary', s.customAccent);
    document.body.style.setProperty('--bg', s.customBg);
    document.body.style.setProperty('--bg-grad', s.customBg);
  } else {
    document.body.style.removeProperty('--primary');
    document.body.style.removeProperty('--bg');
    document.body.style.removeProperty('--bg-grad');
  }

  document.querySelectorAll('#theme-preset-chips .chip').forEach(ch => {
    ch.classList.toggle('active', ch.dataset.themeId === s.theme);
  });

  const dict = I18N[s.language] || I18N.en;
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.dataset.i18n;
    if (dict[key]) el.textContent = dict[key];
  });
  document.getElementById('quick-lang-select').value = s.language;
  document.getElementById('prof-language').value = s.language;
  document.getElementById('prof-business-name').value = s.businessName;
  document.getElementById('prof-owner-mobile').value = s.ownerMobile;
  document.getElementById('topbar-business-name').textContent = s.businessName;
  document.getElementById('ai-engine-select').value = s.aiEngineMode;
  document.getElementById('web-gemini-key-input').value = s.geminiApiKey || '';
  if (s.profilePhotoDataUrl) {
    document.getElementById('profile-avatar-img').src = s.profilePhotoDataUrl;
  }
}

function switchTab(tabId) {
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.toggle('active', b.dataset.tab === tabId));
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.toggle('active', p.id === `tab-${tabId}`));
}

function renderDashboard() {
  const m = computeDashboardMetrics(state);
  document.getElementById('dash-net-balance').textContent = formatInr(m.netRemaining);
  document.getElementById('dash-total-customers').textContent = String(m.totalCustomers);
  document.getElementById('dash-pending-count').textContent = `${m.pendingCount} pending`;
  document.getElementById('dash-total-udhar').textContent = formatInr(m.totalUdhar);
  document.getElementById('dash-total-jama').textContent = formatInr(m.totalJama);
  document.getElementById('dash-today-count').textContent = `${m.todayCount} tx`;
  document.getElementById('dash-today-flow').textContent = `U: ${formatInr(m.todayUdhar)} | J: ${formatInr(m.todayJama)}`;

  const pendingBox = document.getElementById('dash-pending-list');
  const pending = m.summaries.filter(c => c.netBalance > 0.01).sort((a, b) => b.netBalance - a.netBalance).slice(0, 5);
  pendingBox.innerHTML = pending.length === 0
    ? '<p class="muted">No pending receivables.</p>'
    : pending.map(c => `<div class="list-item clickable" data-open-cust="${c.id}"><span>${escapeHtml(c.name)}</span><strong class="mono" style="color:var(--udhar)">${formatInr(c.netBalance)}</strong></div>`).join('');

  const custById = new Map(state.customers.map(c => [c.id, c]));
  const recent = [...state.transactions].sort((a, b) => b.timestamp - a.timestamp).slice(0, 6);
  document.getElementById('dash-recent-list').innerHTML = recent.length === 0
    ? '<p class="muted">No transactions yet.</p>'
    : recent.map(t => {
        const cn = custById.get(t.customerId)?.name || 'Customer';
        const isU = t.type === 'UDHAR_DIYA' || t.type === 'UDHAR_LIYA';
        return `<div class="list-item"><div><strong>${escapeHtml(cn)}</strong><br><small class="muted">${t.type} • ${new Date(t.timestamp).toLocaleString()}</small></div><strong class="mono" style="color:${isU ? 'var(--udhar)' : 'var(--jama)'}">${formatInr(t.amount)}</strong></div>`;
      }).join('');

  pendingBox.querySelectorAll('[data-open-cust]').forEach(el => {
    el.addEventListener('click', () => {
      selectedCustomerId = el.dataset.openCust;
      switchTab('khata');
      renderKhata();
    });
  });
}

function renderKhata() {
  const listView = document.getElementById('khata-list-view');
  const detailView = document.getElementById('khata-detail-view');
  const m = computeDashboardMetrics(state);

  if (selectedCustomerId) {
    const cust = m.summaries.find(c => c.id === selectedCustomerId);
    if (!cust) {
      selectedCustomerId = null;
      return renderKhata();
    }
    listView.classList.add('hidden');
    detailView.classList.remove('hidden');

    const balColor = cust.netBalance > 0.01 ? 'var(--udhar)' : cust.netBalance < -0.01 ? 'var(--jama)' : 'var(--text)';
    const balLabel = cust.netBalance > 0.01 ? 'Lena Hai (Receivable)' : cust.netBalance < -0.01 ? 'Dena Hai (Payable)' : 'Settled';
    document.getElementById('detail-customer-header').innerHTML = `
      <h3>${escapeHtml(cust.name)} <small class="muted">(${escapeHtml(cust.customerCode)})</small></h3>
      <p class="muted">${escapeHtml(cust.mobile || 'No phone')} ${cust.address ? '• ' + escapeHtml(cust.address) : ''}</p>
      <p class="mono bold" style="color:${balColor};font-size:1.25rem">${balLabel}: ${formatInr(Math.abs(cust.netBalance))}</p>
      <small class="muted">Udhar Diya: ${formatInr(cust.udharDiya)} | Jama Received: ${formatInr(cust.jamaReceived)} | Udhar Liya: ${formatInr(cust.udharLiya)} | Paid Made: ${formatInr(cust.paymentMade)}</small>
    `;

    const txs = state.transactions.filter(t => t.customerId === cust.id).sort((a, b) => b.timestamp - a.timestamp);
    document.getElementById('detail-tx-list').innerHTML = txs.length === 0
      ? '<p class="muted">No ledger entries yet.</p>'
      : txs.map(t => `<div class="list-item">
          <div><strong>${t.type}${t.isPartialPayment ? ' (Partial)' : ''}</strong><br><small class="muted">${new Date(t.timestamp).toLocaleString()}${t.description ? ' • ' + escapeHtml(t.description) : ''}</small></div>
          <div class="inline-row">
            <strong class="mono">${formatInr(t.amount)}</strong>
            <button class="btn btn-outline" data-del-tx="${t.id}">Del</button>
          </div>
        </div>`).join('');

    document.querySelectorAll('[data-del-tx]').forEach(btn => {
      btn.addEventListener('click', () => {
        if (confirm('Delete this transaction?')) {
          state.transactions = state.transactions.filter(t => t.id !== btn.dataset.delTx);
          persistAndRender();
        }
      });
    });
  } else {
    detailView.classList.add('hidden');
    listView.classList.remove('hidden');

    const q = document.getElementById('cust-search-input').value.trim().toLowerCase();
    const filtered = m.summaries.filter(c => {
      const matchQ = !q || c.name.toLowerCase().includes(q) || c.mobile.toLowerCase().includes(q) || c.customerCode.toLowerCase().includes(q);
      const matchF = activeCustFilter === 'all' ? true
        : activeCustFilter === 'lena' ? c.netBalance > 0.01
        : activeCustFilter === 'dena' ? c.netBalance < -0.01
        : Math.abs(c.netBalance) <= 0.01;
      return matchQ && matchF;
    });

    const box = document.getElementById('customers-list-container');
    box.innerHTML = filtered.length === 0
      ? '<div class="card"><p class="muted">No customers found. Click "+ Add Customer" to begin.</p></div>'
      : filtered.map(c => `<div class="list-item clickable" data-pick-cust="${c.id}">
          <div><strong>${escapeHtml(c.name)}</strong><br><small class="muted">${escapeHtml(c.customerCode)} ${c.mobile ? '• ' + escapeHtml(c.mobile) : ''}</small></div>
          <strong class="mono" style="color:${c.netBalance > 0.01 ? 'var(--udhar)' : c.netBalance < -0.01 ? 'var(--jama)' : 'var(--muted)'}">${formatInr(Math.abs(c.netBalance))}</strong>
        </div>`).join('');

    box.querySelectorAll('[data-pick-cust]').forEach(el => {
      el.addEventListener('click', () => {
        selectedCustomerId = el.dataset.pickCust;
        renderKhata();
      });
    });
  }
}

function renderCalculatorHistory() {
  const box = document.getElementById('calc-history-list');
  box.innerHTML = state.calcHistory.length === 0
    ? '<p class="muted">No calculations saved yet.</p>'
    : state.calcHistory.slice(0, 20).map(h => `<div class="list-item"><span>${escapeHtml(h.category)} • ${escapeHtml(h.expr)}</span><strong class="mono">${escapeHtml(h.result)}</strong></div>`).join('');
}

function renderAiChat() {
  const feed = document.getElementById('ai-chat-feed');
  if (aiMessages.length === 0) {
    feed.innerHTML = '<p class="muted">Ask anything in Hindi or English. Financial entries always require your confirmation before saving.</p>';
    return;
  }
  feed.innerHTML = aiMessages.map((m, idx) => {
    const pendingHtml = m.pendingAction ? `<div class="pending-box">
      <strong>Confirm Transaction:</strong> ${escapeHtml(m.pendingAction.customerName)} • ${m.pendingAction.transactionType} • ${formatInr(m.pendingAction.amount)}<br>
      ${m.confirmed ? '<span style="color:var(--jama)">✓ Saved to Khata</span>' : `<button class="btn btn-primary" data-confirm-ai="${idx}">Confirm &amp; Save</button>`}
    </div>` : '';
    return `<div class="chat-bubble ${m.role === 'user' ? 'user' : ''}">
      ${m.engine ? `<small class="muted">${escapeHtml(m.engine)}</small><br>` : ''}
      <div>${escapeHtml(m.text).replace(/\n/g, '<br>')}</div>
      ${pendingHtml}
    </div>`;
  }).join('');

  feed.querySelectorAll('[data-confirm-ai]').forEach(btn => {
    btn.addEventListener('click', () => {
      const msg = aiMessages[Number(btn.dataset.confirmAi)];
      if (!msg?.pendingAction || msg.confirmed) return;
      const act = msg.pendingAction;
      let custId = act.matchedCustomerId;
      if (!custId) {
        custId = `cust_${Date.now()}`;
        state.customers.push({
          id: custId,
          customerCode: `CUST-${1001 + state.customers.length}`,
          name: act.customerName,
          mobile: '',
          address: '',
          notes: 'Created via AI',
          createdAt: Date.now()
        });
      }
      state.transactions.push({
        id: `tx_${Date.now()}`,
        customerId: custId,
        type: act.transactionType,
        amount: act.amount,
        description: act.description,
        isPartialPayment: false,
        timestamp: Date.now()
      });
      msg.confirmed = true;
      showToast('AI Transaction Confirmed & Saved');
      persistAndRender();
    });
  });
  feed.scrollTop = feed.scrollHeight;
}

function renderReportsAndSms() {
  const rep = buildPeriodReport(state.transactions, activeReportPeriod);
  document.getElementById('report-summary-box').innerHTML = `
    <div class="result-box mono">
      <strong>${activeReportPeriod.toUpperCase()} REPORT</strong> (${rep.count} entries)<br>
      Udhar Diya: ${formatInr(rep.udharDiya)} | Jama Received: ${formatInr(rep.jamaReceived)}<br>
      Udhar Liya: ${formatInr(rep.udharLiya)} | Payment Made: ${formatInr(rep.paymentMade)}<br>
      Net Flow: ${formatInr(rep.netFlow)}
    </div>`;

  const sel = document.getElementById('sms-customer-select');
  const prevVal = sel.value;
  sel.innerHTML = state.customers.map(c => `<option value="${c.id}">${escapeHtml(c.name)} (${escapeHtml(c.mobile || 'No phone')})</option>`).join('');
  if (prevVal) sel.value = prevVal;
  updateSmsPreviewBox();

  const logBox = document.getElementById('sms-log-list');
  logBox.innerHTML = state.smsLogs.length === 0
    ? '<p class="muted">No SMS composer logs yet.</p>'
    : state.smsLogs.slice(0, 20).map(l => `<div class="list-item"><div><strong>${escapeHtml(l.customerName)}</strong><br><small class="muted">${escapeHtml(l.body)}</small></div><small class="mono">${escapeHtml(l.status)}</small></div>`).join('');
}

function updateSmsPreviewBox() {
  const custId = document.getElementById('sms-customer-select').value;
  const tplKey = document.getElementById('sms-template-select').value;
  const cust = state.customers.find(c => c.id === custId);
  const summary = cust ? computeCustomerSummary(cust, state.transactions) : null;
  const tpl = state.settings.templates[tplKey] || state.settings.templates.REMINDER;
  document.getElementById('sms-message-box').value = renderSmsTemplate(
    tpl,
    summary?.name || 'Customer',
    state.settings.businessName,
    Math.abs(summary?.netBalance || 0),
    summary?.netBalance || 0
  );
}

function renderAll() {
  applyThemeAndLanguage();
  renderDashboard();
  renderKhata();
  renderCalculatorHistory();
  renderAiChat();
  renderReportsAndSms();
}

function openModal(html, onBind) {
  const backdrop = document.getElementById('modal-backdrop');
  const card = document.getElementById('modal-card');
  card.innerHTML = html;
  backdrop.classList.remove('hidden');
  onBind(() => backdrop.classList.add('hidden'));
}

function initCalculatorControls() {
  const keys = ['AC', '(', ')', '÷', '7', '8', '9', '×', '4', '5', '6', '-', '1', '2', '3', '+', '%', '0', '.', '='];
  const pad = document.getElementById('calc-keypad');
  pad.innerHTML = keys.map(k => `<button class="calc-key ${k === '=' ? 'eq' : ''}" data-key="${k}">${k}</button>`).join('');
  pad.addEventListener('click', e => {
    const k = e.target.dataset.key;
    if (!k) return;
    if (k === 'AC') {
      calcExpr = '';
      document.getElementById('calc-expr-view').textContent = '0';
      document.getElementById('calc-res-view').textContent = '0';
    } else if (k === '=') {
      try {
        const res = evaluateMathExpression(calcExpr);
        state.calcHistory.unshift({ category: 'BASIC', expr: calcExpr, result: String(res) });
        calcExpr = String(res);
        document.getElementById('calc-res-view').textContent = calcExpr;
        persistAndRender();
      } catch (err) {
        document.getElementById('calc-res-view').textContent = err.message;
      }
    } else {
      calcExpr += k;
      document.getElementById('calc-expr-view').textContent = calcExpr;
      try {
        document.getElementById('calc-res-view').textContent = String(evaluateMathExpression(calcExpr));
      } catch (_) {}
    }
  });

  document.getElementById('calc-mode-chips').addEventListener('click', e => {
    const mode = e.target.dataset.calcMode;
    if (!mode) return;
    document.querySelectorAll('#calc-mode-chips .chip').forEach(c => c.classList.toggle('active', c.dataset.calcMode === mode));
    document.querySelectorAll('.calc-subpanel').forEach(p => p.classList.toggle('active', p.id === `calc-panel-${mode}`));
  });

  // GST, Discount, Percentage, Profit/Loss, Units live bindings
  const recalcAllSpec = () => {
    const g = calcGst(Number(document.getElementById('gst-amount').value) || 0, Number(document.getElementById('gst-rate').value) || 0, document.getElementById('gst-inclusive').checked);
    document.getElementById('gst-result-box').innerHTML = `Base: ${formatInr(g.base)}<br>CGST: ${formatInr(g.cgst)} | SGST: ${formatInr(g.sgst)}<br><strong>Total: ${formatInr(g.total)}</strong>`;

    const d = calcDiscount(Number(document.getElementById('disc-mrp').value) || 0, Number(document.getElementById('disc-p1').value) || 0, Number(document.getElementById('disc-p2').value) || 0);
    document.getElementById('disc-result-box').innerHTML = `Saved: ${formatInr(d.saved)} (${d.effectivePct.toFixed(1)}%)<br><strong>Final Price: ${formatInr(d.finalPrice)}</strong>`;

    const pr = Number(document.getElementById('pct-rate').value) || 0;
    const pt = Number(document.getElementById('pct-total').value) || 0;
    document.getElementById('pct-result-box').innerHTML = `<strong>${pr}% of ${pt} = ${(pr * pt / 100).toFixed(2)}</strong>`;

    const pl = calcProfitLoss(Number(document.getElementById('pl-cp').value) || 0, Number(document.getElementById('pl-sp').value) || 0, Math.max(1, Number(document.getElementById('pl-qty').value) || 1));
    document.getElementById('pl-result-box').innerHTML = `<strong>${pl.isProfit ? 'Profit' : 'Loss'}: ${formatInr(Math.abs(pl.diff))} (${Math.abs(pl.pctCost).toFixed(1)}%)</strong>`;

    const uFrom = UNITS.find(u => u.id === document.getElementById('unit-from').value) || UNITS[0];
    const uTo = UNITS.find(u => u.id === document.getElementById('unit-to').value) || UNITS[1];
    const uVal = Number(document.getElementById('unit-val').value) || 0;
    const conv = uFrom.cat === uTo.cat ? (uVal * uFrom.factor) / uTo.factor : NaN;
    document.getElementById('unit-result-box').innerHTML = Number.isFinite(conv)
      ? `<strong>${conv.toFixed(4)} ${uTo.id}</strong>`
      : 'Select units from the same category (Weight, Length, Area, Volume)';
  };

  const uFromSel = document.getElementById('unit-from');
  const uToSel = document.getElementById('unit-to');
  const opts = UNITS.map(u => `<option value="${u.id}">${u.label}</option>`).join('');
  uFromSel.innerHTML = opts;
  uToSel.innerHTML = opts;
  uToSel.selectedIndex = 1;

  ['gst-amount', 'gst-rate', 'gst-inclusive', 'disc-mrp', 'disc-p1', 'disc-p2', 'pct-rate', 'pct-total', 'pl-cp', 'pl-sp', 'pl-qty', 'unit-val', 'unit-from', 'unit-to']
    .forEach(id => document.getElementById(id).addEventListener('input', recalcAllSpec));
  recalcAllSpec();

  document.getElementById('save-gst-btn').addEventListener('click', () => {
    const g = calcGst(Number(document.getElementById('gst-amount').value) || 0, Number(document.getElementById('gst-rate').value) || 0, document.getElementById('gst-inclusive').checked);
    state.calcHistory.unshift({ category: 'GST', expr: `₹${document.getElementById('gst-amount').value} @ ${document.getElementById('gst-rate').value}%`, result: formatInr(g.total) });
    persistAndRender(); showToast('GST saved');
  });
  document.getElementById('save-disc-btn').addEventListener('click', () => {
    const d = calcDiscount(Number(document.getElementById('disc-mrp').value) || 0, Number(document.getElementById('disc-p1').value) || 0, Number(document.getElementById('disc-p2').value) || 0);
    state.calcHistory.unshift({ category: 'DISCOUNT', expr: `₹${document.getElementById('disc-mrp').value} - ${document.getElementById('disc-p1').value}%`, result: formatInr(d.finalPrice) });
    persistAndRender(); showToast('Discount saved');
  });
  document.getElementById('save-pct-btn').addEventListener('click', () => {
    const r = (Number(document.getElementById('pct-rate').value) || 0) * (Number(document.getElementById('pct-total').value) || 0) / 100;
    state.calcHistory.unshift({ category: 'PERCENT', expr: `${document.getElementById('pct-rate').value}% of ${document.getElementById('pct-total').value}`, result: String(r) });
    persistAndRender(); showToast('Percentage saved');
  });
  document.getElementById('save-pl-btn').addEventListener('click', () => {
    const pl = calcProfitLoss(Number(document.getElementById('pl-cp').value) || 0, Number(document.getElementById('pl-sp').value) || 0, Math.max(1, Number(document.getElementById('pl-qty').value) || 1));
    state.calcHistory.unshift({ category: 'PROFIT/LOSS', expr: `CP ${document.getElementById('pl-cp').value} -> SP ${document.getElementById('pl-sp').value}`, result: formatInr(pl.diff) });
    persistAndRender(); showToast('Profit/Loss saved');
  });
  document.getElementById('save-unit-btn').addEventListener('click', () => {
    state.calcHistory.unshift({ category: 'UNIT', expr: `${document.getElementById('unit-val').value} ${uFromSel.value} -> ${uToSel.value}`, result: document.getElementById('unit-result-box').textContent });
    persistAndRender(); showToast('Unit conversion saved');
  });
  document.getElementById('clear-calc-history-btn').addEventListener('click', () => {
    state.calcHistory = []; persistAndRender();
  });
}

async function handleAiSubmit(prompt) {
  const clean = String(prompt || '').trim();
  if (!clean) return;
  aiMessages.push({ role: 'user', text: clean });
  renderAiChat();

  const mode = state.settings.aiEngineMode;
  const apiKey = state.settings.geminiApiKey;
  let out;
  if (mode === 'online' || (mode === 'auto' && apiKey)) {
    try {
      if (!apiKey) throw new Error('No Gemini API key entered in Profile.');
      out = await runOnlineGeminiRest(clean, state, apiKey);
    } catch (err) {
      if (mode === 'online') {
        out = { engine: 'Online Gemini AI Error', reply: `${err.message}. Switch to Offline Smart Engine or add your key in Profile.` };
      } else {
        out = runOfflineSmartEngine(clean, state);
      }
    }
  } else {
    out = runOfflineSmartEngine(clean, state);
  }
  aiMessages.push({ role: 'assistant', text: out.reply, engine: out.engine, pendingAction: out.pendingAction || null, confirmed: false });
  renderAiChat();
}

function downloadBlob(filename, content, mime) {
  const blob = new Blob([content], { type: mime });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
  URL.revokeObjectURL(a.href);
}

function escapeHtml(str) {
  return String(str || '').replace(/[&<>"']/g, s => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[s]));
}

window.addEventListener('DOMContentLoaded', () => {
  // Splash animation
  setTimeout(() => document.getElementById('splash-overlay').classList.add('fade-out'), 800);

  // Navigation
  document.getElementById('nav-list').addEventListener('click', e => {
    const btn = e.target.closest('.nav-btn');
    if (btn) switchTab(btn.dataset.tab);
  });
  document.getElementById('dash-open-ai-btn').addEventListener('click', () => switchTab('ai'));
  document.getElementById('dash-open-calc-btn').addEventListener('click', () => switchTab('calculator'));

  // Quick calculator
  document.getElementById('quick-calc-input').addEventListener('input', e => {
    try {
      document.getElementById('quick-calc-output').textContent = e.target.value.trim() ? `= ${evaluateMathExpression(e.target.value)}` : '= 0';
    } catch (_) {
      document.getElementById('quick-calc-output').textContent = '...';
    }
  });

  initCalculatorControls();

  // Customer search & filter
  document.getElementById('cust-search-input').addEventListener('input', renderKhata);
  document.getElementById('cust-filter-chips').addEventListener('click', e => {
    if (!e.target.dataset.custFilter) return;
    activeCustFilter = e.target.dataset.custFilter;
    document.querySelectorAll('#cust-filter-chips .chip').forEach(c => c.classList.toggle('active', c.dataset.custFilter === activeCustFilter));
    renderKhata();
  });

  document.getElementById('open-add-customer-btn').addEventListener('click', () => {
    openModal(`<h3>Add Customer</h3>
      <input id="m-cname" placeholder="Customer Name *" />
      <input id="m-cmobile" placeholder="Mobile Number" />
      <input id="m-caddr" placeholder="Address" />
      <input id="m-cnotes" placeholder="Notes" />
      <div class="inline-row"><button class="btn btn-primary" id="m-csave">Save</button><button class="btn btn-outline" id="m-ccancel">Cancel</button></div>`, close => {
      document.getElementById('m-ccancel').onclick = close;
      document.getElementById('m-csave').onclick = () => {
        const name = document.getElementById('m-cname').value.trim();
        if (!name) return;
        state.customers.push({
          id: `cust_${Date.now()}`,
          customerCode: `CUST-${1001 + state.customers.length}`,
          name,
          mobile: document.getElementById('m-cmobile').value.trim(),
          address: document.getElementById('m-caddr').value.trim(),
          notes: document.getElementById('m-cnotes').value.trim(),
          createdAt: Date.now()
        });
        close();
        persistAndRender();
        showToast('Customer added');
      };
    });
  });

  document.getElementById('back-to-customers-btn').addEventListener('click', () => { selectedCustomerId = null; renderKhata(); });
  document.getElementById('delete-customer-btn').addEventListener('click', () => {
    if (confirm('Delete this customer and all their ledger entries?')) {
      state.transactions = state.transactions.filter(t => t.customerId !== selectedCustomerId);
      state.customers = state.customers.filter(c => c.id !== selectedCustomerId);
      selectedCustomerId = null;
      persistAndRender();
      showToast('Customer deleted');
    }
  });
  document.getElementById('edit-customer-btn').addEventListener('click', () => {
    const cust = state.customers.find(c => c.id === selectedCustomerId);
    if (!cust) return;
    openModal(`<h3>Edit Customer</h3>
      <input id="m-ename" value="${escapeHtml(cust.name)}" />
      <input id="m-emobile" value="${escapeHtml(cust.mobile)}" />
      <input id="m-eaddr" value="${escapeHtml(cust.address)}" />
      <div class="inline-row"><button class="btn btn-primary" id="m-esave">Update</button><button class="btn btn-outline" id="m-ecancel">Cancel</button></div>`, close => {
      document.getElementById('m-ecancel').onclick = close;
      document.getElementById('m-esave').onclick = () => {
        cust.name = document.getElementById('m-ename').value.trim() || cust.name;
        cust.mobile = document.getElementById('m-emobile').value.trim();
        cust.address = document.getElementById('m-eaddr').value.trim();
        close();
        persistAndRender();
        showToast('Customer updated');
      };
    });
  });

  document.querySelectorAll('[data-tx-type]').forEach(btn => {
    btn.addEventListener('click', () => {
      const txType = btn.dataset.txType;
      openModal(`<h3>${txType.replace('_', ' ')}</h3>
        <input id="m-tx-amt" type="number" placeholder="Amount ₹ *" />
        <input id="m-tx-desc" placeholder="Description / Note" />
        <label><input type="checkbox" id="m-tx-partial" /> Partial Payment</label>
        <div class="inline-row"><button class="btn btn-primary" id="m-tx-save">Save Entry</button><button class="btn btn-outline" id="m-tx-cancel">Cancel</button></div>`, close => {
        document.getElementById('m-tx-cancel').onclick = close;
        document.getElementById('m-tx-save').onclick = () => {
          const amt = Number(document.getElementById('m-tx-amt').value);
          if (!Number.isFinite(amt) || amt <= 0) return showToast('Enter valid positive amount');
          const dup = findPossibleDuplicate(state.transactions, selectedCustomerId, txType, amt);
          if (dup && !confirm('Duplicate entry detected within last 15 minutes. Save anyway?')) return;
          state.transactions.push({
            id: `tx_${Date.now()}`,
            customerId: selectedCustomerId,
            type: txType,
            amount: amt,
            description: document.getElementById('m-tx-desc').value.trim(),
            isPartialPayment: document.getElementById('m-tx-partial').checked,
            timestamp: Date.now()
          });
          close();
          persistAndRender();
          showToast('Transaction saved');
        };
      });
    });
  });

  document.getElementById('detail-send-sms-btn').addEventListener('click', () => {
    const sel = document.getElementById('sms-customer-select');
    sel.value = selectedCustomerId;
    switchTab('sms-reports');
    updateSmsPreviewBox();
  });

  // AI bindings
  document.getElementById('ai-send-btn').addEventListener('click', () => {
    const inp = document.getElementById('ai-prompt-input');
    handleAiSubmit(inp.value);
    inp.value = '';
  });
  document.getElementById('ai-prompt-input').addEventListener('keydown', e => {
    if (e.key === 'Enter') {
      e.preventDefault();
      document.getElementById('ai-send-btn').click();
    }
  });
  document.getElementById('calc-nl-btn').addEventListener('click', () => {
    const inp = document.getElementById('calc-nl-input');
    switchTab('ai');
    handleAiSubmit(inp.value);
    inp.value = '';
  });
  document.getElementById('calc-nl-input').addEventListener('keydown', e => {
    if (e.key === 'Enter') {
      e.preventDefault();
      document.getElementById('calc-nl-btn').click();
    }
  });
  document.getElementById('ai-quick-chips').addEventListener('click', e => {
    if (e.target.dataset.aiPrompt) handleAiSubmit(e.target.dataset.aiPrompt);
  });
  document.getElementById('ai-engine-select').addEventListener('change', e => {
    state.settings.aiEngineMode = e.target.value;
    persistAndRender();
  });

  // Reports & SMS
  document.getElementById('report-period-chips').addEventListener('click', e => {
    if (!e.target.dataset.reportPeriod) return;
    activeReportPeriod = e.target.dataset.reportPeriod;
    document.querySelectorAll('#report-period-chips .chip').forEach(c => c.classList.toggle('active', c.dataset.reportPeriod === activeReportPeriod));
    renderReportsAndSms();
  });
  document.getElementById('export-csv-btn').addEventListener('click', () => {
    downloadBlob('desi_calculator_ledger.csv', buildCsvContent(state), 'text/csv;charset=utf-8');
    showToast('CSV exported');
  });
  document.getElementById('export-json-btn').addEventListener('click', () => {
    downloadBlob('desi_calculator_backup_v2.json', JSON.stringify(state, null, 2), 'application/json');
    showToast('JSON backup exported');
  });
  document.getElementById('sms-customer-select').addEventListener('change', updateSmsPreviewBox);
  document.getElementById('sms-template-select').addEventListener('change', updateSmsPreviewBox);
  document.getElementById('open-sms-composer-btn').addEventListener('click', () => {
    const cust = state.customers.find(c => c.id === document.getElementById('sms-customer-select').value);
    const body = document.getElementById('sms-message-box').value;
    try {
      launchWebSmsComposer(cust?.mobile || '', body);
      state.smsLogs.unshift({ customerName: cust?.name || 'Customer', body, status: 'COMPOSER_OPENED', timestamp: Date.now() });
      persistAndRender();
    } catch (err) {
      showToast(err.message);
    }
  });
  document.getElementById('save-sms-templates-btn').addEventListener('click', () => {
    const key = document.getElementById('sms-template-select').value;
    state.settings.templates[key] = document.getElementById('sms-message-box').value;
    persistAndRender();
    showToast('SMS template saved');
  });

  // Profile, Themes, Language & Migration
  document.getElementById('quick-lang-select').addEventListener('change', e => {
    state.settings.language = e.target.value; persistAndRender();
  });
  document.getElementById('save-profile-btn').addEventListener('click', () => {
    state.settings.businessName = document.getElementById('prof-business-name').value.trim() || 'Desi Vyapar Kendra';
    state.settings.ownerMobile = document.getElementById('prof-owner-mobile').value.trim();
    state.settings.language = document.getElementById('prof-language').value;
    persistAndRender(); showToast('Profile saved');
  });
  document.getElementById('profile-photo-input').addEventListener('change', e => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => { state.settings.profilePhotoDataUrl = String(reader.result); persistAndRender(); };
    reader.readAsDataURL(file);
  });
  document.getElementById('theme-preset-chips').addEventListener('click', e => {
    if (!e.target.dataset.themeId) return;
    state.settings.theme = e.target.dataset.themeId;
    persistAndRender();
  });
  document.getElementById('apply-custom-theme-btn').addEventListener('click', () => {
    state.settings.theme = 'custom';
    state.settings.customAccent = document.getElementById('custom-accent-picker').value;
    state.settings.customBg = document.getElementById('custom-bg-picker').value;
    persistAndRender(); showToast('Custom theme applied');
  });
  document.getElementById('save-web-gemini-key-btn').addEventListener('click', () => {
    state.settings.geminiApiKey = document.getElementById('web-gemini-key-input').value.trim();
    persistAndRender(); showToast('API key saved locally in browser');
  });
  document.getElementById('import-json-input').addEventListener('change', e => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try {
        const res = mergeBackupObjectNonDestructive(state, JSON.parse(String(reader.result)));
        document.getElementById('migration-status-text').textContent =
          `Imported +${res.customersImported} customers, +${res.transactionsImported} transactions (preserved ${res.customersSkipped} existing customers & ${res.transactionsSkipped} entries).`;
        persistAndRender();
        showToast('Backup migrated safely');
      } catch (err) {
        showToast('Invalid JSON file');
      }
    };
    reader.readAsText(file);
  });
  document.getElementById('scan-legacy-ls-btn').addEventListener('click', () => {
    const res = migrateLegacyLocalStorageIntoState(state);
    document.getElementById('migration-status-text').textContent =
      `Legacy scan complete: +${res.importedCustomers} customers, +${res.importedTx} entries merged.`;
    persistAndRender();
  });

  renderAll();
});
