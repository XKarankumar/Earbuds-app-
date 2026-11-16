// Desi Calculator AI PRO V2.0 - Safe Offline Calculator Engine

export const UNITS = [
  { id: 'kg', label: 'Kilogram (kg)', cat: 'weight', factor: 1000 },
  { id: 'g', label: 'Gram (g)', cat: 'weight', factor: 1 },
  { id: 'quintal', label: 'Quintal', cat: 'weight', factor: 100000 },
  { id: 'tola', label: 'Tola (11.664g)', cat: 'weight', factor: 11.6638038 },
  { id: 'm', label: 'Meter (m)', cat: 'length', factor: 1 },
  { id: 'ft', label: 'Foot (ft)', cat: 'length', factor: 0.3048 },
  { id: 'yard', label: 'Yard / Gaj', cat: 'length', factor: 0.9144 },
  { id: 'sqft', label: 'Sq. Feet', cat: 'area', factor: 1 },
  { id: 'sqyd', label: 'Sq. Yard / Gaj', cat: 'area', factor: 9 },
  { id: 'bigha', label: 'Bigha (~27225 sq.ft)', cat: 'area', factor: 27225 },
  { id: 'l', label: 'Litre (L)', cat: 'volume', factor: 1000 },
  { id: 'ml', label: 'Millilitre (mL)', cat: 'volume', factor: 1 }
];

// Safe recursive-descent expression parser (no eval)
export function evaluateMathExpression(raw) {
  const s = String(raw || '').replace(/×/g, '*').replace(/÷/g, '/').replace(/,/g, '').replace(/₹/g, '').trim();
  if (!s) throw new Error('Empty expression');
  let pos = 0;

  const skip = () => { while (pos < s.length && /\s/.test(s[pos])) pos++; };
  const eat = (ch) => { skip(); if (s[pos] === ch) { pos++; return true; } return false; };

  function parseExpr() {
    let x = parseTerm();
    while (true) {
      if (eat('+')) x += parseTerm();
      else if (eat('-')) x -= parseTerm();
      else return x;
    }
  }
  function parseTerm() {
    let x = parseFactor();
    while (true) {
      if (eat('*')) x *= parseFactor();
      else if (eat('/')) {
        const d = parseFactor();
        if (Math.abs(d) < 1e-12) throw new Error('Division by zero');
        x /= d;
      } else return x;
    }
  }
  function parseFactor() {
    skip();
    if (eat('+')) return parseFactor();
    if (eat('-')) return -parseFactor();
    let x;
    if (eat('(')) {
      x = parseExpr();
      if (!eat(')')) throw new Error('Missing )');
    } else {
      const start = pos;
      while (pos < s.length && (/[0-9.]/.test(s[pos]))) pos++;
      if (pos === start) throw new Error('Invalid token');
      x = Number(s.slice(start, pos));
    }
    if (eat('%')) x /= 100;
    return x;
  }

  const val = parseExpr();
  skip();
  if (pos < s.length || !Number.isFinite(val)) throw new Error('Invalid expression');
  return Math.round(val * 1e6) / 1e6;
}

export function calcGst(amount, rate, inclusive) {
  if (inclusive) {
    const base = amount / (1 + rate / 100);
    const gst = amount - base;
    return { base, gst, cgst: gst / 2, sgst: gst / 2, total: amount };
  }
  const gst = amount * (rate / 100);
  return { base: amount, gst, cgst: gst / 2, sgst: gst / 2, total: amount + gst };
}

export function calcDiscount(mrp, d1, d2 = 0) {
  const after1 = mrp * (1 - Math.min(100, Math.max(0, d1)) / 100);
  const finalPrice = after1 * (1 - Math.min(100, Math.max(0, d2)) / 100);
  const saved = mrp - finalPrice;
  return { finalPrice, saved, effectivePct: mrp > 0 ? (saved / mrp) * 100 : 0 };
}

export function calcProfitLoss(cp, sp, qty = 1) {
  const cost = cp * qty;
  const sell = sp * qty;
  const diff = sell - cost;
  return {
    cost, sell, diff,
    isProfit: diff >= 0,
    pctCost: cost > 0 ? (diff / cost) * 100 : 0,
    marginSell: sell > 0 ? (diff / sell) * 100 : 0
  };
}
