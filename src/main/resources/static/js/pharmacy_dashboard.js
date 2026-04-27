import { initializeApp } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-app.js";
import { getDatabase, ref, onValue, update, push } from "https://www.gstatic.com/firebasejs/10.8.1/firebase-database.js";

const firebaseConfig = {
    apiKey: "AIzaSyDElz6FYf5Kr9g1jYHKWNy2Zdp09OdF1_0",
    authDomain: "carelink-41b88.firebaseapp.com",
    databaseURL: "https://carelink-41b88-default-rtdb.asia-southeast1.firebasedatabase.app",
    projectId: "carelink-41b88",
    storageBucket: "carelink-41b88.firebasestorage.app",
    messagingSenderId: "651778516076",
    appId: "1:651778516076:web:fe6cb4cfc7ca03622fe449",
    measurementId: "G-MD85QBX5L5"
};

const app = initializeApp(firebaseConfig);
const db = getDatabase(app);

const PHARMACY_ID = localStorage.getItem('USER_ID');

const tableBody = document.getElementById('overviewOrdersTableBody');
const modalBg = document.getElementById('modalBg');
const modalBody = document.getElementById('modalBody');
const mOrderId = document.getElementById('mOrderId');
const mStatus = document.getElementById('mStatus');

let activeChatOrderId = null;
let chatUnsubscribe = null;

function fetchLiveOrders() {
    const ordersRef = ref(db, 'Orders');
    onValue(ordersRef, (snapshot) => {
        const data = snapshot.val();
        tableBody.innerHTML = '';
        if (data) {
            const ordersArray = Object.keys(data).map(orderId => {
                return {
                    id: orderId,
                    ...data[orderId]
                };
            });

            ordersArray.sort((a, b) => b.timestamp - a.timestamp);

            ordersArray.forEach(order => {
                if (String(order.pharmacyId) === String(PHARMACY_ID) && order.status !== "DELIVERED") {
                    renderTableRow(order.id, order);
                }
            });
        }
    });
}

function calcTotal(order) {
    let totalAmount = 0;
    if (order.orderItems)
        order.orderItems
            .filter(i => i.status !== 'OUT_OF_STOCK')
            .forEach(i => totalAmount += (i.product.price * i.quantity));
    if (order.prescriptions)
        order.prescriptions
            .filter(p => p.status !== 'REJECTED')
            .forEach(p => totalAmount += (Number(p.price) || 0));
    return totalAmount;
}

function renderTableRow(orderId, order) {
    const tr = document.createElement('tr');
    const date = new Date(order.timestamp);
    const dateString = date.toLocaleDateString() + " " + date.toLocaleTimeString();

    let medsCount = 0;
    if (order.orderItems) medsCount += order.orderItems.length;
    if (order.prescriptions) medsCount += order.prescriptions.length;

    let totalAmount = calcTotal(order);
    let deliveryCharge = 450.00;
    let grandTotal = totalAmount + deliveryCharge;

    tr.innerHTML = `
        <td class="order-id">#ORD-${orderId}</td>
        <td>
            <div class="patient-cell">
                <div class="p-avatar">👤</div>
                <div><div class="p-name">${order.customerName || 'Customer ' + order.customerId}</div></div>
            </div>
        </td>
        <td>${order.paymentMethod || 'N/A'}</td>
        <td style="font-size: 11px;">${dateString}</td>
        <td><span class="badge ${order.status.toLowerCase()}">${order.status}</span></td>
        <td>${order.customerMobile}</td>
        <td class="amount">Rs. ${grandTotal.toFixed(2)}</td>
        <td class="action-btns" style="display: flex; gap: 8px;"></td>
    `;

    const viewBtn = document.createElement('button');
    viewBtn.className = 'btn-sm primary';
    viewBtn.textContent = 'View';
    viewBtn.onclick = (e) => { e.stopPropagation(); openOrderModal(orderId, order); };

    const chatBtn = document.createElement('button');
    chatBtn.className = 'btn-sm ghost';
    chatBtn.innerHTML = '💬 Chat';
    chatBtn.style.color = '#3b9edd';
    chatBtn.style.borderColor = '#3b9edd';
    chatBtn.onclick = (e) => { e.stopPropagation(); openChatWindow(orderId, order.customerId); };

    const actionTd = tr.querySelector('.action-btns');
    actionTd.appendChild(viewBtn);
    actionTd.appendChild(chatBtn);

    tr.onclick = () => openOrderModal(orderId, order);
    tableBody.appendChild(tr);
}

window.openChatWindow = function(orderId, customerId) {
    activeChatOrderId = orderId;
    document.getElementById('chatTitle').textContent = `Order #ORD-${orderId}`;
    document.getElementById('chatSub').textContent = `Customer ID: ${customerId}`;
    document.getElementById('chatModalBg').classList.add('open');

    const chatBody = document.getElementById('chatBody');
    chatBody.innerHTML = '';
    const chatRef = ref(db, `Chats/Order_${orderId}`);
    if(chatUnsubscribe) chatUnsubscribe();

    chatUnsubscribe = onValue(chatRef, (snapshot) => {
        const data = snapshot.val();
        chatBody.innerHTML = '';
        if(data) {
            const messages = Object.keys(data).map(key => data[key]).sort((a, b) => a.timestamp - b.timestamp);
            messages.forEach(msg => {
                const isSentByMe = msg.senderId === `Pharmacy_${PHARMACY_ID}`;
                const div = document.createElement('div');
                div.className = `chat-bubble ${isSentByMe ? 'sent' : 'received'}`;
                const time = new Date(msg.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
                div.innerHTML = `${msg.message} <span class="chat-time">${time}</span>`;
                chatBody.appendChild(div);
            });
            chatBody.scrollTop = chatBody.scrollHeight;
        } else {
            chatBody.innerHTML = `<div style="text-align:center; color:gray; font-size:12px; margin-top:20px;">No messages yet. Send a message to start!</div>`;
        }
    });
}

window.closeChat = function() { document.getElementById('chatModalBg').classList.remove('open'); }

window.sendMessage = function() {
    const input = document.getElementById('chatInput');
    const text = input.value.trim();
    if(!text || !activeChatOrderId) return;
    const newMessage = { message: text, senderId: `Pharmacy_${PHARMACY_ID}`, timestamp: Date.now() };
    const chatRef = ref(db, `Chats/Order_${activeChatOrderId}`);
    push(chatRef, newMessage).then(() => { input.value = ''; }).catch(err => { alert("Error: " + err.message); });
}

window.openOrderModal = function(orderId, order) {
    mOrderId.textContent = `Order #ORD-${orderId}`;
    mStatus.textContent = order.status;
    modalBg.classList.add('open');
    let html = ``;

    if (order.orderItems && order.orderItems.length > 0) {
        html += `<div style="margin-bottom: 20px;"><h4 style="margin-bottom: 10px; color: var(--green-deep);">Order Items</h4>`;
        order.orderItems.forEach((item, index) => {
            html += `
            <div style="display: flex; justify-content: space-between; align-items: center; background: #fff; padding: 10px; border: 1px solid var(--cream-dark); border-radius: 8px; margin-bottom: 8px;">
                <div><strong>${item.product.name}</strong> <br><span style="font-size: 11px; color: var(--text-muted);">Qty: ${item.quantity} | Rs. ${item.product.price}</span></div>
                <select onchange="updateFirebaseNode('Orders/${orderId}/orderItems/${index}', 'status', this.value)" style="padding: 5px; border-radius: 5px; border: 1px solid #ccc; font-size: 12px; outline: none;">
                    <option value="REVIEWING" ${item.status === 'REVIEWING' ? 'selected' : ''}>REVIEWING</option>
                    <option value="IN_STOCK" ${item.status === 'IN_STOCK' ? 'selected' : ''}>IN STOCK</option>
                    <option value="OUT_OF_STOCK" ${item.status === 'OUT_OF_STOCK' ? 'selected' : ''}>OUT OF STOCK</option>
                </select>
            </div>`;
        });
        html += `</div>`;
    }

    if (order.prescriptions && order.prescriptions.length > 0) {
        html += `<div style="margin-bottom: 20px;"><h4 style="margin-bottom: 10px; color: var(--green-deep);">Prescriptions</h4>`;
        order.prescriptions.forEach((pres, index) => {
            html += `
            <div style="display: flex; gap: 15px; background: #fff; padding: 10px; border: 1px solid var(--cream-dark); border-radius: 8px; margin-bottom: 8px;">
                <img src="${pres.imageUrl}" style="width: 80px; height: 80px; object-fit: cover; border-radius: 8px; cursor: pointer; border: 1px solid #ccc;" onclick="window.open('${pres.imageUrl}', '_blank')"/>
                <div style="flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 8px;">
                    <div style="display: flex; align-items: center; justify-content: space-between;">
                        <label style="font-size: 11px; font-weight: bold;">Price (Rs):</label>
                        <input type="number" value="${pres.price || ''}" placeholder="0.00" onblur="updatePrescriptionField('${orderId}', ${index}, ${pres.id}, 'price', this.value)" style="width: 100px; padding: 5px; border-radius: 5px; border: 1px solid #ccc; font-size: 12px; outline: none;">
                    </div>
                    <div style="display: flex; align-items: center; justify-content: space-between;">
                        <label style="font-size: 11px; font-weight: bold;">Status:</label>
                        <select onchange="updatePrescriptionField('${orderId}', ${index}, ${pres.id}, 'status', this.value)" style="width: 100px; padding: 5px; border-radius: 5px; border: 1px solid #ccc; font-size: 12px; outline: none;">
                            <option value="REVIEWING" ${pres.status === 'REVIEWING' ? 'selected' : ''}>REVIEWING</option>
                            <option value="APPROVED" ${pres.status === 'APPROVED' ? 'selected' : ''}>APPROVED</option>
                            <option value="REJECTED" ${pres.status === 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                        </select>
                    </div>
                </div>
            </div>`;
        });
        html += `</div>`;
    }

    let totalAmount = calcTotal(order);
    let deliveryCharge = 450.00;
    let grandTotal = totalAmount + deliveryCharge;

    html += `
        <div style="margin-top: 15px; padding: 15px; background: #fafaf7; border-radius: 8px; border: 1px solid var(--cream-dark);">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <span style="font-size: 13px; color: var(--text-muted);">Subtotal (Items + Prescriptions):</span>
                <span style="font-size: 13px; font-weight: 500;">Rs. ${totalAmount.toFixed(2)}</span>
            </div>
            
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                <span style="font-size: 13px; color: var(--text-muted);">Delivery Charge:</span>
                <span style="font-size: 13px; font-weight: bold; color: var(--text-dark);">Rs. ${deliveryCharge.toFixed(2)}</span>
            </div>
            
            <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px; padding-top: 10px; border-top: 1px dashed #ccc;">
                <span style="font-size: 15px; font-weight: bold; color: var(--green-deep);">Grand Total:</span>
                <span style="font-size: 16px; font-weight: bold; color: #1a9e55;">Rs. ${grandTotal.toFixed(2)}</span>
            </div>
        </div>
    `;

    html += `
        <div style="margin-top: 20px; padding-top: 15px; border-top: 2px dashed var(--cream-dark);">
            <div style="display: flex; justify-content: space-between; align-items: center;">
                <h4 style="color: var(--text-dark);">Overall Order Status</h4>
                <select onchange="updateFirebaseNode('Orders/${orderId}', 'status', this.value)" style="padding: 8px; border-radius: 5px; border: 2px solid var(--green-accent); font-weight: bold; outline: none;">
                    <option value="REVIEWING" ${order.status === 'REVIEWING' ? 'selected' : ''}>REVIEWING</option>
                    <option value="PROCESSING" ${order.status === 'PROCESSING' ? 'selected' : ''}>PROCESSING</option>
                    <option value="APPROVED" ${order.status === 'APPROVED' ? 'selected' : ''}>APPROVED</option>
                    <option value="REJECTED" ${order.status === 'REJECTED' ? 'selected' : ''}>REJECTED</option>
                    <option value="CANCELLED" ${order.status === 'CANCELLED' ? 'selected' : ''}>CANCELLED</option>
                    <option value="OUT_FOR_DELIVERY" ${order.status === 'OUT_FOR_DELIVERY' ? 'selected' : ''}>OUT FOR DELIVERY</option>
                    <option value="DELIVERED" ${order.status === 'DELIVERED' ? 'selected' : ''}>DELIVERED</option>
                </select>
            </div>
        </div>
    `;

    modalBody.innerHTML = html;

    const mActionBtn = document.getElementById('mActionBtn');
    if (mActionBtn) {
        mActionBtn.onclick = function() {
            updateFirebaseNode(`Orders/${orderId}`, 'status', 'PROCESSING');
            closeModal();
        };
    }
}

window.closeModal = function() { modalBg.classList.remove('open'); }

window.updatePrescriptionField = function(orderId, index, presId, field, value) {
    if (!value) return;

    let finalValue = value;
    if (field === 'price') {
        finalValue = Number(value);
        if (isNaN(finalValue) || finalValue < 0) return;
    }

    updateFirebaseNode(`Orders/${orderId}/prescriptions/${index}`, field, finalValue);

    const token = localStorage.getItem('JWT_TOKEN');
    if (presId && token) {
        fetch(`/api/prescriptions/${presId}/${field}?${field}=${encodeURIComponent(finalValue)}`, {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + token
            }
        })
            .then(response => {
                if (response.ok) {
                    console.log(`MySQL Updated: Prescription ${presId} -> ${field}: ${finalValue}`);
                } else {
                    console.error("Failed to update MySQL Database");
                }
            })
            .catch(err => console.error("Error reaching Spring Boot:", err));
    }
}

window.updateFirebaseNode = function(dbPath, key, newValue) {
    const nodeRef = ref(db, dbPath);
    const updates = {};
    updates[key] = newValue;

    update(nodeRef, updates).then(() => {
        console.log(`Updated Firebase: ${dbPath} -> ${key}: ${newValue}`);

        if (key === 'status' && dbPath.split('/').length === 2) {
            const orderId = dbPath.split('/')[1];
            notifyFCM(orderId, newValue);
        }

    }).catch((error) => {
        alert("Error updating Firebase: " + error.message);
    });
}

function notifyFCM(orderId, newStatus) {
    const token = localStorage.getItem('JWT_TOKEN');

    fetch(`/api/orders/${orderId}/status?newStatus=${newStatus}`, {
        method: 'PUT',
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json'
        }
    })
        .then(async response => {
            if (response.ok) {
                console.log("Spring Boot updated & Push Notification Sent!");
            } else {
                const err = await response.text();
                console.error("Failed to notify Spring Boot:", err);
            }
        })
        .catch(err => console.error("Error reaching Spring Boot:", err));
}

window.onload = () => { fetchLiveOrders(); };

window.setPage = function(pageId, element) {
    document.querySelectorAll('.sb-item').forEach(el => el.classList.remove('active'));
    element.classList.add('active');
    document.querySelectorAll('.page').forEach(el => el.classList.remove('active'));
    document.getElementById('page-' + pageId).classList.add('active');
    document.getElementById('topbarTitle').textContent = element.querySelector('.sb-label').textContent;
}

window.logout = function() {
    localStorage.removeItem('JWT_TOKEN');
    localStorage.removeItem('USER_ID');
    localStorage.removeItem('USER_ROLE');
    localStorage.removeItem('IS_LOGGED_IN');
    window.location.href = '/pharmacy-login';
}