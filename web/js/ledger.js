// Desi Calculator AI PRO V2.0 - Udhar/Jama Ledger & Reporting Engine

export function computeCustomerSummary(customer, transactions) {
  let udharDiya = 0;
  let jamaReceived = 0;
  let udharLiya = 0;
  let paymentMade = 0;
  let count = 0;

  for (const tx of transactions) {
    if (tx.customerId !== customer.id) continue;
    count++;
    const amt = Number(tx.amount) || 0;
    if (tx.type === 'UDHAR_DIYA') udharDiya += amt;
    else if (tx.type === 'JAMA_RECEIVED') jamaReceived += amt;
    else if (tx.type === 'UDHAR_LIYA') udharLiya += amt;
    else if (tx.type === 'PAYMENT_MADE') paymentMade += amt;
  }

  const receivable = udharDiya - jamaReceived;
  const payable = udharLiya - paymentMade;
  return {
    ...customer,
    udharDiya,
    jamaReceived,
    udharLiya,
    paymentMade,
    receivable,
    payable,
    netBalance: receivable - payable,
    transactionCount: count
  };
}

export function computeDashboardMetrics(state) {
  const summaries = state.customers.map(c => computeCustomerSummary(c, state.transactions));
  const startOfToday = new Date();
  startOfToday.setHours(0, 0, 0, 0);
  const todayMs = startOfToday.getTime();
  const todayTx = state.transactions.filter(t => Number(t.timestamp) >= todayMs);

  const totalUdhar = summaries.reduce((s, c) => s + c.udharDiya, 0);
  const totalJama = summaries.reduce((s, c) => s + c.jamaReceived, 0);
  const netReceivable = summaries.reduce((s, c) => s + Math.max(0, c.netBalance), 0);
  const netPayable = summaries.reduce((s, c) => s + Math.max(0, -c.netBalance), 0);

  return {
    summaries,
    totalCustomers: summaries.length,
    pendingCount: summaries.filter(c => Math.abs(c.netBalance) > 0.01).length,
    totalUdhar,
    totalJama,
    netRemaining: netReceivable - netPayable,
    todayCount: todayTx.length,
    todayUdhar: todayTx.filter(t => t.type === 'UDHAR_DIYA' || t.type === 'UDHAR_LIYA').reduce((s, t) => s + Number(t.amount), 0),
    todayJama: todayTx.filter(t => t.type === 'JAMA_RECEIVED' || t.type === 'PAYMENT_MADE').reduce((s, t) => s + Number(t.amount), 0)
  };
}

export function findPossibleDuplicate(transactions, customerId, type, amount) {
  const fifteenMinsAgo = Date.now() - 15 * 60 * 1000;
  return transactions.find(t =>
    t.customerId === customerId &&
    t.type === type &&
    Math.abs(Number(t.amount) - Number(amount)) < 0.01 &&
    Number(t.timestamp) >= fifteenMinsAgo
  ) || null;
}

export function buildPeriodReport(transactions, period) {
  const now = new Date();
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);
  if (period === 'weekly') start.setDate(start.getDate() - 6);
  else if (period === 'monthly') start.setDate(start.getDate() - 29);

  const startMs = start.getTime();
  const filtered = transactions.filter(t => Number(t.timestamp) >= startMs);

  let udharDiya = 0, jamaReceived = 0, udharLiya = 0, paymentMade = 0;
  for (const t of filtered) {
    const amt = Number(t.amount) || 0;
    if (t.type === 'UDHAR_DIYA') udharDiya += amt;
    else if (t.type === 'JAMA_RECEIVED') jamaReceived += amt;
    else if (t.type === 'UDHAR_LIYA') udharLiya += amt;
    else if (t.type === 'PAYMENT_MADE') paymentMade += amt;
  }

  return {
    period,
    count: filtered.length,
    udharDiya,
    jamaReceived,
    udharLiya,
    paymentMade,
    netFlow: (jamaReceived - udharDiya) + (udharLiya - paymentMade)
  };
}

export function buildCsvContent(state) {
  const custMap = new Map(state.customers.map(c => [c.id, c]));
  const esc = v => `"${String(v ?? '').replace(/"/g, '""')}"`;
  const lines = ['TransactionID,Date,CustomerCode,CustomerName,Mobile,Type,AmountINR,PartialPayment,Description'];
  for (const tx of state.transactions) {
    const c = custMap.get(tx.customerId);
    lines.push([
      esc(tx.id),
      esc(new Date(Number(tx.timestamp)).toISOString()),
      esc(c?.customerCode || ''),
      esc(c?.name || 'Unknown'),
      esc(c?.mobile || ''),
      esc(tx.type),
      Number(tx.amount).toFixed(2),
      Boolean(tx.isPartialPayment),
      esc(tx.description || '')
    ].join(','));
  }
  return lines.join('\n');
}
