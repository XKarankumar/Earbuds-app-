// Desi Calculator AI PRO V2.0 - Bilingual Offline Smart Engine & Online Gemini REST Client
import { formatInr } from './storage.js';
import { computeDashboardMetrics, findPossibleDuplicate, buildPeriodReport } from './ledger.js';
import { evaluateMathExpression, calcGst, calcDiscount } from './calculator.js';

export function runOfflineSmartEngine(query, state) {
  const raw = String(query || '').trim();
  const lower = raw.toLowerCase();
  const metrics = computeDashboardMetrics(state);

  // 1. Transaction intent detection
  const amtMatch = lower.match(/(\d+(?:\.\d{1,2})?)/);
  if (amtMatch) {
    const amount = Number(amtMatch[1]);
    let txType = null;
    if (/udhar liya|borrowed|liya udhar/.test(lower)) txType = 'UDHAR_LIYA';
    else if (/payment made|chukaya|wapas diye|paid back/.test(lower)) txType = 'PAYMENT_MADE';
    else if (/jama|mila|mile|received|wapas aaye|paid me/.test(lower)) txType = 'JAMA_RECEIVED';
    else if (/udhar|diye|diya|credit|gave|baki likh/.test(lower)) txType = 'UDHAR_DIYA';

    if (txType && amount > 0) {
      const matched = metrics.summaries.find(c => lower.includes(c.name.toLowerCase().trim()));
      const candidateName = matched?.name || extractCandidateName(raw);
      if (!candidateName) {
        return { engine: 'Offline Smart Engine', reply: 'Kripya customer ka naam likhein (e.g. "Rahul ko 500 rupaye udhar diye").' };
      }
      const dup = matched ? findPossibleDuplicate(state.transactions, matched.id, txType, amount) : null;
      return {
        engine: 'Offline Smart Engine',
        reply: `Samjha: ${candidateName} ke liye ${formatInr(amount)} (${txType}).${dup ? ' ⚠️ Warning: Similar entry recorded in last 15 mins!' : ''} Save karne ke liye Confirm dabayein.`,
        pendingAction: {
          customerName: candidateName,
          matchedCustomerId: matched?.id || null,
          transactionType: txType,
          amount,
          description: `AI Entry: ${raw}`,
          isPossibleDuplicate: Boolean(dup)
        }
      };
    }
  }

  // 2. Today's summary
  if (/(aaj|today)/.test(lower) && /(hisab|total|summary|report|batao)/.test(lower)) {
    return {
      engine: 'Offline Smart Engine',
      reply: `Aaj ka Total Hisab:\n• Transactions: ${metrics.todayCount}\n• Aaj Udhar: ${formatInr(metrics.todayUdhar)}\n• Aaj Jama: ${formatInr(metrics.todayJama)}\n• Total Remaining Balance: ${formatInr(metrics.netRemaining)}`
    };
  }

  // 3. Highest due customer
  if (/(sabse zyada|highest|top baki|zyada baki|kis customer)/.test(lower)) {
    const top = metrics.summaries.filter(c => c.netBalance > 0.01).sort((a, b) => b.netBalance - a.netBalance)[0];
    return {
      engine: 'Offline Smart Engine',
      reply: top
        ? `Sabse zyada baki ${top.name} ka hai: ${formatInr(top.netBalance)} (Udhar: ${formatInr(top.udharDiya)}, Jama: ${formatInr(top.jamaReceived)}).`
        : 'Abhi kisi customer par koi baki nahi hai.'
    };
  }

  // 4. Monthly / Weekly report
  if (/(mahine|month|pichhle|weekly|hafte|report)/.test(lower)) {
    const period = /(week|hafte)/.test(lower) ? 'weekly' : 'monthly';
    const rep = buildPeriodReport(state.transactions, period);
    return {
      engine: 'Offline Smart Engine',
      reply: `${period.toUpperCase()} Report: ${rep.count} entries | Udhar Diya: ${formatInr(rep.udharDiya)} | Jama Received: ${formatInr(rep.jamaReceived)} | Net Flow: ${formatInr(rep.netFlow)}`
    };
  }

  // 5. GST / Discount / Math
  if (lower.includes('gst')) {
    const nums = (lower.match(/\d+(?:\.\d+)?/g) || []).map(Number);
    if (nums.length >= 2) {
      const rate = nums.find(n => n <= 40) ?? nums[0];
      const amt = nums.find(n => n !== rate) ?? nums[1];
      const g = calcGst(amt, rate, /inclusive|included/.test(lower));
      return { engine: 'Offline Smart Engine', reply: `GST (${rate}%): Base ${formatInr(g.base)}, CGST ${formatInr(g.cgst)} + SGST ${formatInr(g.sgst)}, Total ${formatInr(g.total)}` };
    }
  }
  if (/discount|chhoot|off/.test(lower)) {
    const nums = (lower.match(/\d+(?:\.\d+)?/g) || []).map(Number);
    if (nums.length >= 2) {
      const pct = nums.find(n => n <= 99) ?? nums[0];
      const mrp = nums.find(n => n !== pct) ?? nums[1];
      const d = calcDiscount(mrp, pct);
      return { engine: 'Offline Smart Engine', reply: `Discount (${pct}% on ${formatInr(mrp)}): Saved ${formatInr(d.saved)}, Final Price ${formatInr(d.finalPrice)}` };
    }
  }
  try {
    const expr = raw.replace(/[^0-9+\-*/().%]/g, '').trim();
    if (/[+\-*/]/.test(expr) && /\d/.test(expr)) {
      return { engine: 'Offline Smart Engine', reply: `${expr} = ${evaluateMathExpression(expr)}` };
    }
  } catch (_) {}

  return {
    engine: 'Offline Smart Engine',
    reply: 'Try: "Rahul ko 500 rupaye udhar diye", "Rahul se 200 rupaye jama mile", "Aaj ka total hisab batao", "Kis customer ka sabse zyada baki hai?", or "Pichhle mahine ki report dikhao".'
  };
}

export async function runOnlineGeminiRest(query, state, apiKey) {
  const metrics = computeDashboardMetrics(state);
  const topList = metrics.summaries.slice(0, 15).map(c =>
    `${c.name} (Net: ${formatInr(c.netBalance)}, Udhar: ${formatInr(c.udharDiya)}, Jama: ${formatInr(c.jamaReceived)})`
  ).join('\n');

  const sysPrompt = `You are Desi Calculator AI PRO V2.0 bilingual (Hindi, Hinglish, English) assistant.
Return valid JSON ONLY with keys: replyText (string), intent ("PROPOSE_TRANSACTION"|"QUERY_ANSWER"), customerName (string|null), transactionType ("UDHAR_DIYA"|"JAMA_RECEIVED"|"UDHAR_LIYA"|"PAYMENT_MADE"|"NONE"), amount (number), description (string).
Never claim a transaction is already saved; always ask user confirmation.
LEDGER: Customers=${metrics.totalCustomers}, Remaining=${formatInr(metrics.netRemaining)}, Today=${metrics.todayCount} entries.\n${topList}\nUSER: ${query}`;

  const resp = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${encodeURIComponent(apiKey)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      contents: [{ parts: [{ text: sysPrompt }] }],
      generationConfig: { responseMimeType: 'application/json', temperature: 0.2 }
    })
  });
  if (!resp.ok) throw new Error(`Gemini API HTTP ${resp.status}`);
  const data = await resp.json();
  const txt = data?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!txt) throw new Error('Empty Gemini response');
  const parsed = JSON.parse(txt);

  let pendingAction = null;
  if (parsed.intent === 'PROPOSE_TRANSACTION' && parsed.customerName && parsed.amount > 0 && parsed.transactionType !== 'NONE') {
    const matched = metrics.summaries.find(c => c.name.toLowerCase() === String(parsed.customerName).toLowerCase().trim());
    const dup = matched ? findPossibleDuplicate(state.transactions, matched.id, parsed.transactionType, parsed.amount) : null;
    pendingAction = {
      customerName: matched?.name || parsed.customerName,
      matchedCustomerId: matched?.id || null,
      transactionType: parsed.transactionType,
      amount: Number(parsed.amount),
      description: parsed.description || `AI Entry: ${query}`,
      isPossibleDuplicate: Boolean(dup)
    };
  }
  return { engine: 'Online Gemini AI', reply: parsed.replyText || 'Done.', pendingAction };
}

function extractCandidateName(raw) {
  return raw
    .replace(/\d+(?:\.\d+)?/g, ' ')
    .replace(/\b(ko|se|ka|ki|ke|for|from|to|rs|inr|rupaye|rupees|udhar|jama|diye|diya|liya|mila|mile|received|gave|credit|debit|payment|made|add|record)\b/gi, ' ')
    .replace(/[^\p{L}\s]/gu, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .split(' ')
    .filter(w => w.length >= 2)
    .slice(0, 3)
    .map(w => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}
