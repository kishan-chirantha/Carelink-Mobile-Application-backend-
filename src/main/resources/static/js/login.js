const BASE_URL = "/api/auth";

function handleSignin() {

    const emailInput = document.getElementById('email');
    const passwordInput = document.getElementById('password');
    const email = emailInput.value.trim();
    const password = passwordInput.value.trim();

    const errorBanner = document.getElementById('errorBanner');
    const errorMsg = document.getElementById('errorMsg');
    const loginBtn = document.getElementById('loginBtn');
    const btnLabel = loginBtn.querySelector('.btn-label');
    const spinner = document.getElementById('spinner');

    if (!email || !password) {
        showError("Please enter both email and password.");
        emailInput.classList.add('error');
        passwordInput.classList.add('error');
        return;
    }

    emailInput.classList.remove('error');
    passwordInput.classList.remove('error');
    errorBanner.classList.remove('show');

    loginBtn.classList.add('loading');
    loginBtn.disabled = true;
    btnLabel.textContent = "Authenticating...";

    fetch(`${BASE_URL}/pharmacy-login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email: email, password: password })
    })
        .then(async response => {
            if (!response.ok) {
                const errText = await response.text();
                throw new Error(errText || 'Login failed');
            }
            return response.json();
        })
        .then(data => {
            localStorage.clear();
            localStorage.setItem('JWT_TOKEN', data.token);
            localStorage.setItem('USER_ID', data.customerId);
            localStorage.setItem('USER_ROLE', data.role);
            localStorage.setItem('IS_LOGGED_IN', 'true');


            console.log(localStorage.getItem("USER_ID"));

            setTimeout(() => {
                if (data.role === 'ADMIN') {
                    window.location.href = '/admin-dashboard';
                } else if (data.role === 'PHARMACY') {
                    window.location.href = '/pharmacy-dashboard';
                } else {
                    showError("Access Denied: Only Staff members can login here.");
                    resetButton();
                }
            }, 1500);
        })
        .catch(error => {
            showError(error.message || "Invalid email or password!");
            resetButton();
        });

    function showError(message) {
        errorMsg.textContent = message;
        errorBanner.classList.add('show');
    }

    function resetButton() {
        loginBtn.classList.remove('loading');
        loginBtn.disabled = false;
        btnLabel.textContent = "Sign In";
    }
}

function togglePass() {
    const passInput = document.getElementById('password');
    const eyeIcon = document.getElementById('eyeIcon');
    if (passInput.type === 'password') {
        passInput.type = 'text';
        eyeIcon.style.color = 'var(--green-accent)';
    } else {
        passInput.type = 'password';
        eyeIcon.style.color = 'var(--text-muted)';
    }
}

function showForgot(e) {
    e.preventDefault();
    document.getElementById('modalBg').style.display = 'flex';
}
function closeModal() {
    document.getElementById('modalBg').style.display = 'none';
}
function sendReset() {
    document.getElementById('resetSuccess').style.display = 'block';
}