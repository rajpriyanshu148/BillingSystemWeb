/* ============================================================
   BillingSystem Pro — Main Application JavaScript
   ============================================================ */
'use strict';

// ── State ─────────────────────────────────────────────────────
const state = {
  currentPage: 'dashboard',
  products: [],
  customers: [],
  cart: [],
  selectedProduct: null,
  appInfo: {},
};

// ── Init ──────────────────────────────────────────────────────
window.addEventListener('DOMContentLoaded', async () => {
  await loadAppInfo();
  navigate('dashboard');
  setupProductSearch();
  setupSidebar();
});

async function loadAppInfo() {
  try {
    const info = await api('/api/info');
    state.appInfo = info;
    document.getElementById('userName').textContent = info.username;
    document.getElementById('userRole').textContent = info.isAdmin ? 'Administrator' : 'Staff';
    document.getElementById('userAvatar').textContent = info.username.charAt(0).toUpperCase();
    document.getElementById('companyName').textContent = info.company;
  } catch (e) { /* ignore */ }
}

// ── Navigation ────────────────────────────────────────────────
function navigate(page) {
  document.querySelectorAll('.page').forEach(el => el.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
  document.getElementById('page-' + page).classList.add('active');
  const navEl = document.getElementById('nav-' + page);
  if (navEl) navEl.classList.add('active');
  state.currentPage = page;

  const titles = { dashboard: 'Dashboard', billing: 'New Bill', products: 'Products', customers: 'Customers', reports: 'Reports' };
  document.getElementById('pageTitle').textContent = titles[page] || page;

  if (page === 'dashboard') loadDashboard();
  if (page === 'billing') loadBillingPage();
  if (page === 'products') loadProducts();
  if (page === 'customers') loadCustomers();
  if (page === 'reports') loadReports();

  // Close sidebar on mobile
  document.getElementById('sidebar').classList.remove('open');
}

document.querySelectorAll('.nav-item[data-page]').forEach(el => {
  el.addEventListener('click', e => { e.preventDefault(); navigate(el.dataset.page); });
});

function setupSidebar() {
  document.getElementById('hamburger').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('open');
  });
}

// ── Dashboard ─────────────────────────────────────────────────
async function loadDashboard() {
  try {
    const data = await api('/api/dashboard');
    renderKPIs(data);
    renderRecentOrders(data.recentOrders);
  } catch (e) { toast('Failed to load dashboard', 'error'); }
}

function renderKPIs(data) {
  const grid = document.getElementById('kpiGrid');
  grid.innerHTML = `
    <div class="kpi-card">
      <div class="kpi-icon">💰</div>
      <div class="kpi-value">${fmt(data.todayRevenue)}</div>
      <div class="kpi-label">Today's Revenue</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-icon">🧾</div>
      <div class="kpi-value">${data.todayBillCount}</div>
      <div class="kpi-label">Today's Bills</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-icon">📦</div>
      <div class="kpi-value">${data.productCount}</div>
      <div class="kpi-label">Active Products</div>
    </div>
    <div class="kpi-card">
      <div class="kpi-icon">👥</div>
      <div class="kpi-value">${data.customerCount}</div>
      <div class="kpi-label">Total Customers</div>
    </div>`;
}

function renderRecentOrders(orders) {
  const tbody = document.getElementById('recentOrdersTbody');
  if (!orders || orders.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty-cell">No bills yet — create your first invoice!</td></tr>';
    return;
  }
  tbody.innerHTML = orders.map(o => `
    <tr>
      <td><strong>${o.invoiceNumber}</strong></td>
      <td>${o.customerName}</td>
      <td><strong>${fmt(o.totalAmount)}</strong></td>
      <td>${paymentBadge(o.paymentMethod)}</td>
      <td>${statusBadge(o.paymentStatus)}</td>
      <td>${fmtDate(o.createdAt)}</td>
    </tr>`).join('');
}

// ── Billing ───────────────────────────────────────────────────
async function loadBillingPage() {
  try {
    const [products, customers] = await Promise.all([api('/api/products'), api('/api/customers')]);
    state.products = products;
    state.customers = customers;
    // Populate customer dropdown
    const sel = document.getElementById('billCustomer');
    sel.innerHTML = customers.map(c => `<option value="${c.id}">${c.name}${c.phone ? ' ('+c.phone+')' : ''}</option>`).join('');
  } catch (e) { toast('Failed to load billing data', 'error'); }
}

function setupProductSearch() {
  const input = document.getElementById('productSearch');
  const list = document.getElementById('autocompleteList');

  input.addEventListener('input', () => {
    const kw = input.value.toLowerCase().trim();
    if (!kw) { list.classList.remove('open'); return; }
    const matches = state.products.filter(p => p.name.toLowerCase().includes(kw)).slice(0, 8);
    if (!matches.length) { list.classList.remove('open'); return; }
    list.innerHTML = matches.map(p => `
      <div class="autocomplete-item" onclick="selectProduct(${p.id})">
        <span>${p.name} <small style="color:var(--muted);font-size:11px">(${p.unit})</small></span>
        <span class="item-price">${fmt(p.price)} + ${p.gstPercent}% GST</span>
      </div>`).join('');
    list.classList.add('open');
  });

  document.addEventListener('click', e => {
    if (!input.contains(e.target) && !list.contains(e.target)) list.classList.remove('open');
  });
}

function selectProduct(id) {
  const p = state.products.find(x => x.id === id);
  if (!p) return;
  state.selectedProduct = p;
  document.getElementById('productSearch').value = p.name;
  document.getElementById('autocompleteList').classList.remove('open');
  document.getElementById('billQty').focus();
}

function addToCart() {
  const p = state.selectedProduct;
  if (!p) { toast('Please select a product first', 'error'); return; }
  const qty = parseInt(document.getElementById('billQty').value) || 1;
  if (qty <= 0) { toast('Quantity must be at least 1', 'error'); return; }
  if (qty > p.quantity) { toast(`Insufficient stock. Available: ${p.quantity}`, 'error'); return; }

  const existing = state.cart.find(i => i.productId === p.id);
  if (existing) {
    existing.quantity += qty;
    existing.recalculate();
  } else {
    const item = {
      productId: p.id, productName: p.name, quantity: qty,
      unitPrice: parseFloat(p.price), gstPercent: parseFloat(p.gstPercent),
      gstAmount: 0, lineTotal: 0,
      recalculate() {
        const base = this.unitPrice * this.quantity;
        this.gstAmount = +(base * this.gstPercent / 100).toFixed(2);
        this.lineTotal = +(base + this.gstAmount).toFixed(2);
      }
    };
    item.recalculate();
    state.cart.push(item);
  }
  state.selectedProduct = null;
  document.getElementById('productSearch').value = '';
  document.getElementById('billQty').value = 1;
  renderCart();
  updateSummary();
}

function removeFromCart(productId) {
  state.cart = state.cart.filter(i => i.productId !== productId);
  renderCart();
  updateSummary();
}

function clearCart() {
  state.cart = [];
  document.getElementById('billDiscount').value = 0;
  renderCart();
  updateSummary();
}

function renderCart() {
  const tbody = document.getElementById('cartTbody');
  if (!state.cart.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="empty-cell">Cart is empty — add products above</td></tr>';
    return;
  }
  tbody.innerHTML = state.cart.map(i => `
    <tr>
      <td><strong>${i.productName}</strong></td>
      <td>${i.quantity}</td>
      <td>${fmtN(i.unitPrice)}</td>
      <td>${i.gstPercent}%</td>
      <td>${fmtN(i.gstAmount)}</td>
      <td><strong>${fmtN(i.lineTotal)}</strong></td>
      <td><button class="btn btn-danger btn-sm" onclick="removeFromCart(${i.productId})">✕</button></td>
    </tr>`).join('');
}

function updateSummary() {
  const subtotal = state.cart.reduce((s, i) => s + i.unitPrice * i.quantity, 0);
  const gst = state.cart.reduce((s, i) => s + i.gstAmount, 0);
  const discount = parseFloat(document.getElementById('billDiscount').value) || 0;
  const total = Math.max(0, subtotal + gst - discount);
  document.getElementById('sumSubtotal').textContent = fmtN(subtotal);
  document.getElementById('sumGst').textContent = fmtN(gst);
  document.getElementById('sumTotal').textContent = fmtN(total);
}

async function saveBill() {
  if (!state.cart.length) { toast('Cart is empty!', 'error'); return; }
  const btn = document.getElementById('saveBillBtn');
  btn.disabled = true; btn.textContent = 'Saving…';
  try {
    const discount = parseFloat(document.getElementById('billDiscount').value) || 0;
    const req = {
      customerId: parseInt(document.getElementById('billCustomer').value),
      paymentMethod: document.getElementById('billPayment').value,
      discountAmount: discount,
      notes: document.getElementById('billNotes').value,
      items: state.cart.map(i => ({ productId: i.productId, quantity: i.quantity }))
    };
    const result = await api('/api/bills', { method: 'POST', body: req });
    toast(`Invoice ${result.invoiceNumber} saved! Total: ${fmt(result.totalAmount)}`, 'success');
    // Show invoice
    const order = await api('/api/bills/' + result.orderId);
    showInvoice(order);
    clearCart();
  } catch (e) {
    toast(e.message || 'Failed to save bill', 'error');
  } finally {
    btn.disabled = false; btn.textContent = '💾 Save & Generate Invoice';
  }
}

function showInvoice(order) {
  const info = state.appInfo;
  const itemsHtml = order.items.map(i => `
    <tr>
      <td>${i.productName}</td>
      <td style="text-align:right">${i.quantity}</td>
      <td style="text-align:right">₹${fmtN2(i.unitPrice)}</td>
      <td style="text-align:right">${i.gstPercent}%</td>
      <td style="text-align:right">₹${fmtN2(i.gstAmount)}</td>
      <td style="text-align:right"><strong>₹${fmtN2(i.lineTotal)}</strong></td>
    </tr>`).join('');

  document.getElementById('invoicePrint').innerHTML = `
    <div style="font-family:Arial,sans-serif;color:#1a1a2e">
      <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px">
        <div>
          <h2 style="font-size:22px;font-weight:800;margin:0">${info.company || 'BillingSystem Pro'}</h2>
          <div style="font-size:12px;color:#64748b;margin-top:4px;line-height:1.6">
            ${info.address || ''}<br/>Phone: ${info.phone || ''}<br/>GSTIN: ${info.gstin || ''}
          </div>
        </div>
        <div style="text-align:right">
          <div style="font-size:24px;font-weight:800;color:#4f46e5">TAX INVOICE</div>
          <div style="font-size:12px;color:#64748b;margin-top:4px">
            <strong>Invoice:</strong> ${order.invoiceNumber}<br/>
            <strong>Date:</strong> ${fmtDate(order.createdAt)}<br/>
            <strong>By:</strong> ${order.createdBy}
          </div>
        </div>
      </div>
      <div style="background:#f8fafc;border-radius:8px;padding:12px 16px;margin-bottom:16px;font-size:12px;color:#475569">
        <strong style="font-size:13px;color:#1a1a2e">${order.customerName}</strong><br/>
        ${order.customerPhone ? 'Phone: ' + order.customerPhone : ''}
        ${order.customerAddress ? '<br/>' + order.customerAddress : ''}
        ${order.customerGstin ? '<br/>GSTIN: ' + order.customerGstin : ''}
      </div>
      <table style="width:100%;border-collapse:collapse;font-size:12px;margin-bottom:16px">
        <thead>
          <tr style="background:#1a1a2e;color:#fff">
            <th style="padding:8px;text-align:left">Product</th>
            <th style="padding:8px;text-align:right">Qty</th>
            <th style="padding:8px;text-align:right">Rate</th>
            <th style="padding:8px;text-align:right">GST%</th>
            <th style="padding:8px;text-align:right">GST ₹</th>
            <th style="padding:8px;text-align:right">Amount</th>
          </tr>
        </thead>
        <tbody>
          ${itemsHtml}
        </tbody>
      </table>
      <div style="display:flex;justify-content:flex-end">
        <div style="min-width:220px;font-size:13px">
          <div style="display:flex;justify-content:space-between;padding:5px 0;border-bottom:1px solid #e2e8f0">
            <span style="color:#64748b">Subtotal</span><span>₹${fmtN2(order.subtotal)}</span>
          </div>
          <div style="display:flex;justify-content:space-between;padding:5px 0;border-bottom:1px solid #e2e8f0">
            <span style="color:#64748b">GST</span><span>₹${fmtN2(order.totalGst)}</span>
          </div>
          ${+order.discountAmount > 0 ? `<div style="display:flex;justify-content:space-between;padding:5px 0;border-bottom:1px solid #e2e8f0">
            <span style="color:#64748b">Discount</span><span>-₹${fmtN2(order.discountAmount)}</span>
          </div>` : ''}
          <div style="display:flex;justify-content:space-between;padding:8px 0;font-size:16px;font-weight:800">
            <span>TOTAL</span><span style="color:#4f46e5">₹${fmtN2(order.totalAmount)}</span>
          </div>
        </div>
      </div>
      <div style="margin-top:16px;padding:10px 14px;background:#f0f9ff;border-radius:6px;font-size:12px;color:#0369a1">
        Payment: <strong>${order.paymentMethod}</strong> | Status: <strong>${order.paymentStatus}</strong>
        ${order.notes ? '<br/>Notes: ' + order.notes : ''}
      </div>
      <div style="margin-top:20px;text-align:center;font-size:11px;color:#94a3b8">Thank you for your business! 🙏</div>
    </div>`;
  document.getElementById('invoiceModal').classList.remove('hidden');
}

function printInvoice() {
  const html = document.getElementById('invoicePrint').innerHTML;
  const w = window.open('', '_blank');
  w.document.write(`<html><head><title>Invoice</title><style>body{font-family:Arial,sans-serif;padding:32px}table{width:100%;border-collapse:collapse}th,td{padding:8px;border:1px solid #e2e8f0}</style></head><body>${html}</body></html>`);
  w.document.close();
  w.print();
}

// ── Products ──────────────────────────────────────────────────
let allProducts = [];

async function loadProducts() {
  try {
    allProducts = await api('/api/products/all');
    renderProducts(allProducts);
  } catch (e) { toast('Failed to load products', 'error'); }
}

function renderProducts(list) {
  const tbody = document.getElementById('productsTbody');
  if (!list.length) { tbody.innerHTML = '<tr><td colspan="7" class="empty-cell">No products found</td></tr>'; return; }
  tbody.innerHTML = list.map(p => `
    <tr>
      <td><strong>${p.name}</strong>${p.description ? '<br/><small style="color:var(--muted)">'+p.description+'</small>' : ''}</td>
      <td>${fmt(p.price)}</td>
      <td class="${p.quantity < 5 ? 'text-danger' : ''}">${p.quantity}</td>
      <td>${p.gstPercent}%</td>
      <td>${p.unit}</td>
      <td>${p.active ? '<span class="badge badge-success">Active</span>' : '<span class="badge badge-danger">Inactive</span>'}</td>
      <td style="display:flex;gap:6px">
        <button class="btn btn-ghost btn-sm" onclick="openProductModal(${p.id})">✏️ Edit</button>
        <button class="btn btn-danger btn-sm" onclick="deleteProduct(${p.id})">🗑️</button>
      </td>
    </tr>`).join('');
}

function filterProducts(kw) {
  const filtered = allProducts.filter(p => p.name.toLowerCase().includes(kw.toLowerCase()));
  renderProducts(filtered);
}

function openProductModal(id) {
  document.getElementById('productModalTitle').textContent = id ? 'Edit Product' : 'Add Product';
  document.getElementById('productId').value = id || '';
  if (id) {
    const p = allProducts.find(x => x.id === id);
    if (!p) return;
    document.getElementById('pName').value = p.name;
    document.getElementById('pDesc').value = p.description || '';
    document.getElementById('pPrice').value = p.price;
    document.getElementById('pQty').value = p.quantity;
    document.getElementById('pGst').value = p.gstPercent;
    document.getElementById('pUnit').value = p.unit;
  } else {
    document.getElementById('productForm').reset();
    document.getElementById('pGst').value = '18';
    document.getElementById('pUnit').value = 'PCS';
  }
  document.getElementById('productModal').classList.remove('hidden');
}

async function saveProduct(e) {
  e.preventDefault();
  const id = document.getElementById('productId').value;
  const data = {
    name: document.getElementById('pName').value,
    description: document.getElementById('pDesc').value,
    price: parseFloat(document.getElementById('pPrice').value),
    quantity: parseInt(document.getElementById('pQty').value),
    gstPercent: parseFloat(document.getElementById('pGst').value),
    unit: document.getElementById('pUnit').value,
    active: true,
  };
  try {
    if (id) { await api('/api/products/' + id, { method: 'PUT', body: data }); toast('Product updated!', 'success'); }
    else { await api('/api/products', { method: 'POST', body: data }); toast('Product added!', 'success'); }
    closeModal('productModal');
    loadProducts();
  } catch (ex) { toast(ex.message || 'Failed to save', 'error'); }
}

async function deleteProduct(id) {
  if (!confirm('Deactivate this product? It will no longer appear in billing.')) return;
  try {
    await api('/api/products/' + id, { method: 'DELETE' });
    toast('Product deactivated', 'success');
    loadProducts();
  } catch (ex) { toast('Failed to delete', 'error'); }
}

// ── Customers ─────────────────────────────────────────────────
let allCustomers = [];

async function loadCustomers() {
  try {
    allCustomers = await api('/api/customers');
    renderCustomers(allCustomers);
  } catch (e) { toast('Failed to load customers', 'error'); }
}

function renderCustomers(list) {
  const tbody = document.getElementById('customersTbody');
  if (!list.length) { tbody.innerHTML = '<tr><td colspan="5" class="empty-cell">No customers yet</td></tr>'; return; }
  tbody.innerHTML = list.map(c => `
    <tr>
      <td><strong>${c.name}</strong></td>
      <td>${c.phone || '—'}</td>
      <td>${c.email || '—'}</td>
      <td>${c.gstin || '—'}</td>
      <td style="display:flex;gap:6px">
        <button class="btn btn-ghost btn-sm" onclick="openCustomerModal(${c.id})">✏️ Edit</button>
        <button class="btn btn-danger btn-sm" onclick="deleteCustomer(${c.id})">🗑️</button>
      </td>
    </tr>`).join('');
}

function filterCustomers(kw) {
  const filtered = allCustomers.filter(c => c.name.toLowerCase().includes(kw.toLowerCase()) ||
    (c.phone && c.phone.includes(kw)));
  renderCustomers(filtered);
}

function openCustomerModal(id) {
  document.getElementById('customerModalTitle').textContent = id ? 'Edit Customer' : 'Add Customer';
  document.getElementById('customerId').value = id || '';
  if (id) {
    const c = allCustomers.find(x => x.id === id);
    if (!c) return;
    document.getElementById('cName').value = c.name;
    document.getElementById('cPhone').value = c.phone || '';
    document.getElementById('cEmail').value = c.email || '';
    document.getElementById('cAddress').value = c.address || '';
    document.getElementById('cGstin').value = c.gstin || '';
  } else {
    document.getElementById('customerForm').reset();
  }
  document.getElementById('customerModal').classList.remove('hidden');
}

async function saveCustomer(e) {
  e.preventDefault();
  const id = document.getElementById('customerId').value;
  const data = {
    name: document.getElementById('cName').value,
    phone: document.getElementById('cPhone').value,
    email: document.getElementById('cEmail').value,
    address: document.getElementById('cAddress').value,
    gstin: document.getElementById('cGstin').value,
  };
  try {
    if (id) { await api('/api/customers/' + id, { method: 'PUT', body: data }); toast('Customer updated!', 'success'); }
    else { await api('/api/customers', { method: 'POST', body: data }); toast('Customer added!', 'success'); }
    closeModal('customerModal');
    loadCustomers();
  } catch (ex) { toast(ex.message || 'Failed to save', 'error'); }
}

async function deleteCustomer(id) {
  if (!confirm('Delete this customer?')) return;
  try {
    await api('/api/customers/' + id, { method: 'DELETE' });
    toast('Customer deleted', 'success');
    loadCustomers();
  } catch (ex) { toast(ex.message || 'Cannot delete (has orders)', 'error'); }
}

// ── Reports ───────────────────────────────────────────────────
async function loadReports() {
  try {
    const [daily, monthly] = await Promise.all([api('/api/reports/daily'), api('/api/reports/monthly')]);
    renderDailyReport(daily);
    renderMonthlyReport(monthly);
  } catch (e) { toast('Failed to load reports', 'error'); }
}

function renderDailyReport(rows) {
  const tbody = document.getElementById('dailyTbody');
  if (!rows.length) { tbody.innerHTML = '<tr><td colspan="4" class="empty-cell">No data yet</td></tr>'; return; }
  tbody.innerHTML = rows.map(r => `
    <tr>
      <td>${r.date}</td>
      <td>${r.billCount}</td>
      <td><strong>${fmt(r.revenue)}</strong></td>
      <td>${fmt(r.gst)}</td>
    </tr>`).join('');
}

function renderMonthlyReport(rows) {
  const months = ['','Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  const tbody = document.getElementById('monthlyTbody');
  if (!rows.length) { tbody.innerHTML = '<tr><td colspan="5" class="empty-cell">No data yet</td></tr>'; return; }
  tbody.innerHTML = rows.map(r => `
    <tr>
      <td>${months[r.month] || r.month}</td>
      <td>${r.year}</td>
      <td>${r.billCount}</td>
      <td><strong>${fmt(r.revenue)}</strong></td>
      <td>${fmt(r.gst)}</td>
    </tr>`).join('');
}

function switchTab(tab, el) {
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
  document.getElementById('dailyReportSection').classList.toggle('hidden', tab !== 'daily');
  document.getElementById('monthlyReportSection').classList.toggle('hidden', tab !== 'monthly');
}

// ── Helpers ───────────────────────────────────────────────────
function closeModal(id) { document.getElementById(id).classList.add('hidden'); }

function fmt(n) { return '₹' + parseFloat(n || 0).toLocaleString('en-IN', { minimumFractionDigits: 2 }); }
function fmtN(n) { return '₹' + parseFloat(n || 0).toFixed(2); }
function fmtN2(n) { return parseFloat(n || 0).toFixed(2); }
function fmtDate(d) { if (!d) return '—'; return new Date(d).toLocaleDateString('en-IN', { day:'2-digit', month:'short', year:'numeric', hour:'2-digit', minute:'2-digit' }); }

function paymentBadge(m) {
  const icons = { CASH: '💵', CARD: '💳', UPI: '📱', ONLINE: '🌐' };
  return `<span class="badge badge-info">${icons[m] || ''} ${m}</span>`;
}
function statusBadge(s) {
  const map = { PAID: 'badge-success', PENDING: 'badge-warning', CANCELLED: 'badge-danger' };
  return `<span class="badge ${map[s] || 'badge-neutral'}">${s}</span>`;
}

function toast(msg, type = 'info') {
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.textContent = msg;
  document.getElementById('toastContainer').appendChild(el);
  setTimeout(() => el.remove(), 4000);
}

// ── API Client ────────────────────────────────────────────────
async function api(url, options = {}) {
  const opts = {
    method: options.method || 'GET',
    headers: { 'Content-Type': 'application/json' },
  };
  if (options.body) opts.body = JSON.stringify(options.body);
  const res = await fetch(url, opts);
  if (res.status === 401 || res.status === 403) { location.href = '/login'; return; }
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
  return data;
}
