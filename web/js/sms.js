// Desi Calculator AI PRO V2.0 - Web SMS Composer & Template Renderer
import { formatInr } from './storage.js';

export function renderSmsTemplate(template, customerName, businessName, amount = 0, balance = 0) {
  const dateStr = new Date().toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  return String(template || '')
    .replaceAll('{name}', customerName || 'Customer')
    .replaceAll('{business}', businessName || 'Desi Vyapar Kendra')
    .replaceAll('{amount}', formatInr(amount, false))
    .replaceAll('{balance}', formatInr(Math.abs(balance), false))
    .replaceAll('{date}', dateStr);
}

export function launchWebSmsComposer(mobile, messageBody) {
  const clean = String(mobile || '').trim();
  if (!clean) {
    throw new Error('Customer mobile number is missing.');
  }
  const sep = /iPad|iPhone|iPod/.test(navigator.userAgent) ? '&' : '?';
  const uri = `sms:${encodeURIComponent(clean)}${sep}body=${encodeURIComponent(messageBody)}`;
  window.location.href = uri;
}
