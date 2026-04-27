const REGISTER_URL = "/api/pharmacies/register";

let currentStep = 1;

function togglePass(inputId, iconElement) {
    const input = document.getElementById(inputId);
    if (input.type === 'password') {
        input.type = 'text';
        iconElement.style.color = 'var(--green-accent)';
    } else {
        input.type = 'password';
        iconElement.style.color = 'var(--text-muted)';
    }
}

function nextStep(step) {
    if (step === 1) {
        const email = document.getElementById('email').value;
        const pass = document.getElementById('password').value;
        const confirmPass = document.getElementById('confirmPassword').value;
        if (!email || !pass) return alert('Email and Password are required!');
        if (pass !== confirmPass) return alert('Passwords do not match!');
    }
    if (step === 2) {
        const name = document.getElementById('pharmacyName').value;
        if (!name) return alert('Pharmacy Name is required!');
    }

    document.getElementById(`section-${step}`).classList.remove('active');
    document.getElementById(`step-${step}`).classList.remove('active');

    currentStep = step + 1;

    document.getElementById(`section-${currentStep}`).classList.add('active');
    document.getElementById(`step-${currentStep}`).classList.add('active');
}

function prevStep(step) {
    document.getElementById(`section-${step}`).classList.remove('active');
    document.getElementById(`step-${step}`).classList.remove('active');

    currentStep = step - 1;

    document.getElementById(`section-${currentStep}`).classList.add('active');
    document.getElementById(`step-${currentStep}`).classList.add('active');
}

function detectLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                document.getElementById('latitude').value = position.coords.latitude;
                document.getElementById('longitude').value = position.coords.longitude;
                alert("Location detected successfully!");
            },
            (error) => {
                alert("Could not get location. Please enter manually.");
            }
        );
    } else {
        alert("Geolocation is not supported by this browser.");
    }
}

function showAlert(message, type) {
    const banner = document.getElementById('alertBanner');
    banner.textContent = message;
    banner.style.display = 'block';

    if (type === 'success') {
        banner.style.backgroundColor = 'rgba(46,204,113,0.1)';
        banner.style.color = '#124a32';
        banner.style.border = '1px solid #2ecc71';
    } else {
        banner.style.backgroundColor = 'rgba(224,85,85,0.08)';
        banner.style.color = '#e05555';
        banner.style.border = '1px solid rgba(224,85,85,0.25)';
    }
}

function submitRegistration() {
    const payload = {
        email: document.getElementById('email').value.trim(),
        password: document.getElementById('password').value.trim(),
        pharmacyName: document.getElementById('pharmacyName').value.trim(),
        contactNumber: document.getElementById('contactNumber').value.trim(),
        role: document.getElementById('role').value,
        isActive: document.getElementById('activeToggle').classList.contains('on'),
        addressLine1: document.getElementById('addressLine1').value.trim(),
        addressLine2: document.getElementById('addressLine2').value.trim(),
        city: document.getElementById('city').value.trim(),
        district: document.getElementById('district').value,
        latitude: parseFloat(document.getElementById('latitude').value) || null,
        longitude: parseFloat(document.getElementById('longitude').value) || null
    };

    const submitBtn = document.querySelector('#section-3 .btn-primary');
    submitBtn.textContent = "Creating Account...";
    submitBtn.disabled = true;

    document.getElementById('alertBanner').style.display = 'none';

    fetch(REGISTER_URL, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
    })
        .then(async response => {
            if (!response.ok) {
                const err = await response.text();
                throw new Error(err || 'Registration Failed');
            }
            return response.text();
        })
        .then(data => {
            showAlert("🎉 Registration Successful! Redirecting to login...", "success");

            document.getElementById('signupForm').style.opacity = '0.5';
            document.getElementById('signupForm').style.pointerEvents = 'none';

            setTimeout(() => {
                window.location.href = '/pharmacy-login';
            }, 2000);
        })
        .catch(error => {
            showAlert("⚠ " + error.message, "error");
            submitBtn.textContent = "Create Account ✦";
            submitBtn.disabled = false;
        });
}