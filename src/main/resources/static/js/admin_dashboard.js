let products = [];
let admins = [];
let pharmacies = [];
let orders = [];

let currentDeleteId = null;
let currentDeleteType = null;
let currentEditId = null;
let selectedImageFiles = [];

function getAuthHeaders() {
    const token = localStorage.getItem('JWT_TOKEN');
    return {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };
}

function getAuthHeadersMultipart() {
    const token = localStorage.getItem('JWT_TOKEN');
    return token ? { 'Authorization': `Bearer ${token}` } : {};
}


document.addEventListener('DOMContentLoaded', () => {
    fetchProductsFromDB();
    fetchAdminsFromDB();
    fetchPharmacies();
    fetchOrders();
    fetchCategories();
});

async function fetchCategories() {
    try {
        const res = await fetch('http://localhost:8080/api/categories');
        if (!res.ok) throw new Error();
        const cats = await res.json();
        const select = document.getElementById('p-category');
        select.innerHTML = '<option value="">Select category</option>';
        cats.forEach(c => {
            select.innerHTML += `<option value="${c.id}">${c.name}</option>`;
        });
    } catch {
        showToast('Failed to load categories!', 'error');
    }
}

async function fetchProductsFromDB() {
    try {
        const response = await fetch('http://localhost:8080/api/products');
        if (!response.ok) throw new Error('Failed to fetch products');
        products = await response.json();
        renderProducts();
        updateDashboardOverview();
    } catch (error) {
        console.error('Error:', error);
        showToast('Failed to load Products!', 'error');
    }
}

async function saveProductToDB(productData) {
    const btn = document.getElementById('prodSaveBtn');
    try {
        btn.disabled = true;
        btn.innerText = 'Saving…';

        const method = currentEditId ? 'PUT' : 'POST';
        const url = currentEditId
            ? `http://localhost:8080/api/products/${currentEditId}`
            : 'http://localhost:8080/api/products';

        const response = await fetch(url, {
            method: method,
            headers: getAuthHeaders(),
            body: JSON.stringify(productData)
        });

        if (!response.ok) {
            const errText = await response.text();
            console.error('Save failed:', response.status, errText);
            throw new Error(`HTTP ${response.status}`);
        }

        const savedProduct = await response.json();
        const productId = savedProduct.id || currentEditId;

        if (selectedImageFiles.length > 0) {
            btn.innerText = 'Uploading images…';
            await uploadProductImages(productId);
        }

        showToast(currentEditId ? 'Product updated!' : 'Product saved!');
        closeModal('productModal');
        clearImagePreviews();
        fetchProductsFromDB();

    } catch (error) {
        console.error('Error saving product:', error);
        showToast('Failed to save product!', 'error');
    } finally {
        btn.disabled = false;
        btn.innerText = 'Save Product';
    }
}

async function deleteProductFromDB(id) {
    try {
        const response = await fetch(`http://localhost:8080/api/products/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error();
        showToast('Product deleted!');
        fetchProductsFromDB();
    } catch {
        showToast('Failed to delete product!', 'error');
    }
}

function renderProducts(searchTxt = '') {
    const tbody = document.getElementById('productsTbody');
    tbody.innerHTML = '';

    let filtered = products.filter(p =>
        p.name.toLowerCase().includes(searchTxt.toLowerCase())
    );

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;padding:20px;color:var(--text-3);">No products found</td></tr>`;
        return;
    }

    filtered.forEach(p => {
        let catName = p.category ? p.category.name : 'Unknown';
        tbody.innerHTML += `<tr>
            <td style="font-weight:500; color:var(--blue);">${p.name}</td>
            <td style="font-size:13px; color:var(--text-2);">${catName}</td>
            <td style="font-weight:600;">Rs. ${p.price.toFixed(2)}</td>
            <td>
                <div class="action-btns">
                    <button class="btn-icon" onclick="editProduct(${p.id})" title="Edit">✏️</button>
                    <button class="btn-icon" style="color:var(--red);" onclick="confirmDelete(${p.id}, 'product', '${p.name}')" title="Delete">🗑️</button>
                </div>
            </td>
        </tr>`;
    });
}

function openProductModal() {
    currentEditId = null;
    document.getElementById('prodModalTitle').innerText = 'Add Product';
    document.getElementById('p-name').value = '';
    document.getElementById('p-category').value = '';
    document.getElementById('p-price').value = '';
    document.getElementById('p-desc').value = '';
    clearImagePreviews();
    fetchCategories();
    document.getElementById('productModal').classList.add('show');
}

function editProduct(id) {
    currentEditId = id;
    let p = products.find(x => x.id === id);
    document.getElementById('prodModalTitle').innerText = 'Edit Product';
    document.getElementById('p-name').value = p.name;
    document.getElementById('p-price').value = p.price;
    document.getElementById('p-desc').value = p.description || '';
    clearImagePreviews();
    fetchCategories().then(() => {
        document.getElementById('p-category').value = p.category ? p.category.id : '';
    });
    document.getElementById('productModal').classList.add('show');
}

function saveProduct() {
    let name = document.getElementById('p-name').value.trim();
    let price = document.getElementById('p-price').value;
    let catId = document.getElementById('p-category').value;
    let desc = document.getElementById('p-desc').value.trim();

    if (!name || !price || !catId) {
        showToast('Please fill Name, Category, and Price!', 'error');
        return;
    }
    if (parseFloat(price) <= 0) {
        showToast('Price must be greater than 0!', 'error');
        return;
    }

    saveProductToDB({
        name,
        price: parseFloat(price),
        description: desc,
        category: { id: parseInt(catId) }
    });
}

function searchProducts(val) { renderProducts(val); }
function filterProducts(type, btn) {
    document.querySelectorAll('#page-products .ftab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    renderProducts();
}

function handleImageSelect(input) {
    Array.from(input.files).forEach(file => {
        if (!selectedImageFiles.find(f => f.name === file.name)) {
            selectedImageFiles.push(file);
        }
    });
    renderImagePreviews();
    input.value = '';
}

function renderImagePreviews() {
    const grid = document.getElementById('imgPreviewGrid');
    if (!grid) return;
    grid.innerHTML = '';
    selectedImageFiles.forEach((file, index) => {
        const reader = new FileReader();
        reader.onload = (e) => {
            const item = document.createElement('div');
            item.className = 'img-preview-item';
            item.innerHTML = `
                <img src="${e.target.result}" alt="preview"/>
                <button class="img-preview-remove" onclick="removeImagePreview(${index})">✕</button>
            `;
            grid.appendChild(item);
        };
        reader.readAsDataURL(file);
    });
}

function removeImagePreview(index) {
    selectedImageFiles.splice(index, 1);
    renderImagePreviews();
}

function clearImagePreviews() {
    selectedImageFiles = [];
    const grid = document.getElementById('imgPreviewGrid');
    if (grid) grid.innerHTML = '';
}

async function uploadProductImages(productId) {
    const formData = new FormData();
    selectedImageFiles.forEach(file => formData.append('files', file));

    const res = await fetch(`http://localhost:8080/api/products/${productId}/images`, {
        method: 'POST',
        headers: getAuthHeadersMultipart(),
        body: formData
    });

    if (!res.ok) {
        const errText = await res.text();
        console.error('Image upload failed:', res.status, errText);
        throw new Error(`Image upload HTTP ${res.status}`);
    }
}

async function fetchAdminsFromDB() {
    try {
        const response = await fetch('http://localhost:8080/api/admin/users/admins', {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error();

        const data = await response.json();
        admins = data.map(admin => ({
            id: admin.id,
            name: admin.pharmacyName,
            email: admin.email,
            role: admin.role,
            added: admin.createdAt ? new Date(admin.createdAt).toLocaleDateString() : 'N/A',
            status: admin.isActive ? 'active' : 'inactive'
        }));

        renderAdmins();
        updateDashboardOverview();
    } catch {
        showToast('Failed to load Admins!', 'error');
    }
}

async function saveAdminToDB(adminData) {
    try {
        const method = currentEditId ? 'PUT' : 'POST';
        const url = currentEditId
            ? `http://localhost:8080/api/admin/users/admins/${currentEditId}`
            : 'http://localhost:8080/api/admin/users/admins';

        const response = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(adminData)
        });

        if (!response.ok) throw new Error();

        showToast(currentEditId ? 'Admin updated!' : 'Admin created!');
        closeModal('adminModal');
        fetchAdminsFromDB();
    } catch {
        showToast('Failed to save Admin!', 'error');
    }
}

async function deleteAdminFromDB(id) {
    try {
        const response = await fetch(`http://localhost:8080/api/admin/users/admins/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error();
        showToast('Admin deleted!');
        fetchAdminsFromDB();
    } catch {
        showToast('Failed to delete Admin!', 'error');
    }
}

function renderAdmins(searchTxt = '') {
    const tbody = document.getElementById('adminsTbody');
    if (!tbody) return;
    tbody.innerHTML = '';

    let filtered = admins.filter(a =>
        a.name.toLowerCase().includes(searchTxt.toLowerCase()) ||
        a.email.toLowerCase().includes(searchTxt.toLowerCase())
    );

    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:20px;color:var(--text-3);">No admins found</td></tr>`;
        return;
    }

    filtered.forEach(a => {
        let badge = a.status === 'active'
            ? '<span class="status-badge status-active">Active</span>'
            : '<span class="status-badge status-inactive">Inactive</span>';
        tbody.innerHTML += `<tr>
            <td style="font-weight:500;">${a.name}</td>
            <td style="font-size:13px; color:var(--text-2);">${a.email}</td>
            <td style="font-size:13px; font-weight:600;">${a.role}</td>
            <td style="font-size:13px; color:var(--text-2);">${a.added}</td>
            <td>${badge}</td>
            <td>
                <div class="action-btns">
                    <button class="btn-icon" onclick="editAdmin(${a.id})" title="Edit">✏️</button>
                    <button class="btn-icon" style="color:var(--red);" onclick="confirmDelete(${a.id}, 'admin', '${a.name}')" title="Delete">🗑️</button>
                </div>
            </td>
        </tr>`;
    });
}

function searchAdmins(val) { renderAdmins(val); }

function filterAdmins(type, btn) {
    document.querySelectorAll('#page-admins .ftab').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    const filtered = type === 'all' ? admins : admins.filter(a => a.role && a.role.toLowerCase() === type.toLowerCase());
    const tbody = document.getElementById('adminsTbody');
    tbody.innerHTML = '';
    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;padding:20px;color:var(--text-3);">No admins found</td></tr>`;
        return;
    }
    filtered.forEach(a => {
        let badge = a.status === 'active'
            ? '<span class="status-badge status-active">Active</span>'
            : '<span class="status-badge status-inactive">Inactive</span>';
        tbody.innerHTML += `<tr>
            <td style="font-weight:500;">${a.name}</td>
            <td style="font-size:13px; color:var(--text-2);">${a.email}</td>
            <td style="font-size:13px; font-weight:600;">${a.role}</td>
            <td style="font-size:13px; color:var(--text-2);">${a.added}</td>
            <td>${badge}</td>
            <td>
                <div class="action-btns">
                    <button class="btn-icon" onclick="editAdmin(${a.id})" title="Edit">✏️</button>
                    <button class="btn-icon" style="color:var(--red);" onclick="confirmDelete(${a.id}, 'admin', '${a.name}')" title="Delete">🗑️</button>
                </div>
            </td>
        </tr>`;
    });
}

function openAdminModal() {
    currentEditId = null;
    document.getElementById('adminModalTitle').innerText = 'Add Admin';
    document.getElementById('a-name').value = '';
    document.getElementById('a-email').value = '';
    document.getElementById('a-role').value = 'admin';
    document.getElementById('a-status').value = 'active';
    document.getElementById('a-password').value = '';
    document.getElementById('a-pw-field').style.display = 'block';
    document.getElementById('adminModal').classList.add('show');
}

function editAdmin(id) {
    currentEditId = id;
    let a = admins.find(x => x.id === id);
    document.getElementById('adminModalTitle').innerText = 'Edit Admin';
    document.getElementById('a-name').value = a.name;
    document.getElementById('a-email').value = a.email;
    document.getElementById('a-role').value = a.role ? a.role.toLowerCase() : 'admin';
    document.getElementById('a-status').value = a.status;
    document.getElementById('a-pw-field').style.display = 'none';
    document.getElementById('adminModal').classList.add('show');
}

function saveAdmin() {
    let name = document.getElementById('a-name').value.trim();
    let email = document.getElementById('a-email').value.trim();
    let role = document.getElementById('a-role').value;
    let status = document.getElementById('a-status').value;
    let password = document.getElementById('a-password').value;

    if (!name || !email) { showToast('Please fill Name and Email!', 'error'); return; }
    if (!currentEditId && !password) { showToast('Password is required!', 'error'); return; }
    if (!currentEditId && password.length < 8) { showToast('Password must be 8+ characters!', 'error'); return; }

    let adminData = { pharmacyName: name, email, role: role.toUpperCase(), isActive: status === 'active' };
    if (password) adminData.password = password;
    saveAdminToDB(adminData);
}

async function fetchPharmacies() {
    try {
        const res = await fetch('http://localhost:8080/api/pharmacies');
        if (!res.ok) throw new Error();
        pharmacies = await res.json();
        renderPharmacies();
        updateDashboardOverview();
    } catch {
        showToast('Failed to load Pharmacies!', 'error');
    }
}

function renderPharmacies() {
    const tbody = document.getElementById('pharmacyTbody');
    tbody.innerHTML = '';
    const filtered = pharmacies.filter(p => p.role && p.role.toUpperCase() === 'PHARMACY');
    if (filtered.length === 0) {
        tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;padding:20px;color:var(--text-3);">No pharmacies found</td></tr>`;
        return;
    }
    filtered.forEach(p => {
        tbody.innerHTML += `<tr>
            <td>${p.pharmacyName}</td>
            <td>${p.email}</td>
            <td>${p.city || '-'}</td>
            <td><span class="status-badge ${p.isActive ? 'status-active' : 'status-inactive'}">${p.isActive ? 'Active' : 'Inactive'}</span></td>
        </tr>`;
    });
}

async function fetchOrders() {
    try {
        const res = await fetch('http://localhost:8080/api/orders');
        if (!res.ok) throw new Error();
        orders = await res.json();
        renderOrders();
        updateDashboardOverview();
    } catch {
        showToast('Failed to load Orders!', 'error');
    }
}

function renderOrders() {
    const tbody = document.getElementById('ordersTbody');
    tbody.innerHTML = '';
    if (orders.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center;padding:20px;color:var(--text-3);">No orders found</td></tr>`;
        return;
    }
    orders.forEach(o => {
        let badgeClass = o.status === 'PENDING' ? 'status-inactive' : 'status-active';
        let customerName = o.customer?.name || o.customerName || 'Unknown';
        tbody.innerHTML += `<tr>
            <td style="font-weight:500;">${o.trackingId || 'N/A'}</td>
            <td style="color:var(--blue); font-weight:500;">${customerName}</td>
            <td>${o.pharmacy?.pharmacyName || '-'}</td>
            <td style="font-weight:600;">Rs. ${o.totalAmount?.toFixed(2) || '0.00'}</td>
            <td><span class="status-badge ${badgeClass}">${o.status || 'N/A'}</span></td>
        </tr>`;
    });
}

function updateDashboardOverview() {
    document.getElementById('ov-prod').innerText = products.length;
    document.getElementById('productsBadge').innerText = products.length;
    document.getElementById('ov-admin').innerText = admins.length;
    document.getElementById('adminsBadge').innerText = admins.length;
    document.getElementById('ov-pharm').innerText = pharmacies.filter(p => p.role && p.role.toUpperCase() === 'PHARMACY').length;
    document.getElementById('ov-orders').innerText = orders.length;

    const tbody = document.getElementById('overviewProductTable');
    let html = `<table style="width:100%;border-collapse:collapse;">
        <thead><tr style="text-align:left;border-bottom:1px solid var(--border);background:var(--bg);color:var(--text-2);font-size:12px;text-transform:uppercase;">
            <th style="padding:12px 18px;">Tracking ID</th>
            <th style="padding:12px 18px;">Customer</th>
            <th style="padding:12px 18px;">Pharmacy</th>
            <th style="padding:12px 18px;">Total</th>
            <th style="padding:12px 18px;">Status</th>
        </tr></thead><tbody>`;

    if (orders.length === 0) {
        html += `<tr><td colspan="5" style="text-align:center;padding:20px;color:var(--text-3);">No recent orders</td></tr>`;
    } else {
        orders.slice(0, 4).forEach(o => {
            let badgeClass = o.status === 'PENDING' ? 'status-inactive' : 'status-active';
            let customerName = o.customer?.name || o.customerName || 'Unknown';
            html += `<tr style="border-bottom:1px solid var(--border);font-size:14px;">
                <td style="padding:12px 18px;font-weight:500;">${o.trackingId || 'N/A'}</td>
                <td style="padding:12px 18px;color:var(--blue);font-weight:500;">${customerName}</td>
                <td style="padding:12px 18px;">${o.pharmacy?.pharmacyName || '-'}</td>
                <td style="padding:12px 18px;font-weight:600;">Rs. ${o.totalAmount?.toFixed(2) || '0.00'}</td>
                <td style="padding:12px 18px;"><span class="status-badge ${badgeClass}">${o.status || 'N/A'}</span></td>
            </tr>`;
        });
    }
    html += `</tbody></table>`;
    tbody.innerHTML = html;
}

function confirmDelete(id, type, name) {
    currentDeleteId = id;
    currentDeleteType = type;
    document.getElementById('deleteName').innerText = `"${name}"`;
    document.getElementById('deleteModal').classList.add('show');
}

document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
    if (currentDeleteType === 'product') deleteProductFromDB(currentDeleteId);
    else if (currentDeleteType === 'admin') deleteAdminFromDB(currentDeleteId);
    closeModal('deleteModal');
});

function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('show');
}

function showToast(msg, type = 'success') {
    const toast = document.getElementById('toast');
    const toastIco = document.getElementById('toastIco');
    const toastBar = document.getElementById('toastBar');
    document.getElementById('toastMsg').innerText = msg;
    if (type === 'error') { toast.classList.add('error'); toastIco.innerText = '✕'; }
    else { toast.classList.remove('error'); toastIco.innerText = '✓'; }
    toast.classList.remove('show');
    toastBar.style.width = '100%';
    void toast.offsetWidth;
    toast.classList.add('show');
    toastBar.style.width = '0%';
    setTimeout(() => { toast.classList.remove('show'); }, 3000);
}

function setPage(pageId, element) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.sb-item').forEach(i => i.classList.remove('active'));
    document.getElementById('page-' + pageId).classList.add('active');
    if (element) element.classList.add('active');
    const titleMap = { overview: 'Dashboard Overview', products: 'Product Catalog', admins: 'System Administrators', pharmacies: 'Pharmacies', orders: 'Orders', settings: 'Settings' };
    document.getElementById('topbarTitle').innerText = titleMap[pageId] || 'CareLink';
}

function handleGlobalSearch(val) {}

function logout() {
    localStorage.removeItem('JWT_TOKEN');
    showToast('Logged out!');
    setTimeout(() => { window.location.href = '/pharmacy-login'; }, 1000);
}