/**
 * Clinic Appointment System - Authentication & Portal Controller
 */

let selectedPortal = 'PATIENT';

function choosePortal(portal, label) {
    selectedPortal = portal;

    const triggerText = document.getElementById('portalTriggerText');
    const portalInput = document.getElementById('loginPortalInput');
    if (triggerText) triggerText.textContent = label || portal;
    if (portalInput) portalInput.value = portal;

    // Update active highlight on dropdown items
    ['PATIENT', 'DOCTOR', 'ADMIN'].forEach(r => {
        const item = document.getElementById(`portalItem-${r}`);
        if (item) {
            if (r === portal) {
                item.classList.add('active');
                item.setAttribute('aria-selected', 'true');
            } else {
                item.classList.remove('active');
                item.setAttribute('aria-selected', 'false');
            }
        }
    });

    const menu = document.getElementById('portalDropdownMenu');
    const trigger = document.getElementById('portalDropdownTrigger');
    if (menu) menu.classList.remove('show');
    if (trigger) {
        trigger.classList.remove('open');
        trigger.setAttribute('aria-expanded', 'false');
    }
}

// Backward-compatible alias
function selectPortal(portal, label) {
    choosePortal(portal, label);
}

async function handleLogin(event) {
    event.preventDefault();

    const emailInput = document.getElementById('loginEmail');
    const passwordInput = document.getElementById('loginPassword');
    const submitBtn = document.getElementById('loginSubmitBtn');
    const portalInput = document.getElementById('loginPortalInput');

    const email = emailInput ? emailInput.value.trim() : '';
    const password = passwordInput ? passwordInput.value.trim() : '';
    const portal = (portalInput && portalInput.value) ? portalInput.value : selectedPortal;

    if (!email || !password) {
        showAlert('Please enter both email address and password.', 'error');
        return;
    }

    try {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Signing In...';

        const response = await apiRequest('/auth/login', 'POST', {
            email,
            password,
            portal: portal
        });

        if (response.success) {
            setCurrentUser(response);
            showAlert('Login successful! Redirecting...', 'success');

            setTimeout(() => {
                if (response.role === 'PATIENT') {
                    window.location.href = '/pages/patient-dashboard.html';
                } else if (response.role === 'DOCTOR') {
                    window.location.href = '/pages/doctor-dashboard.html';
                } else if (response.role === 'ADMIN') {
                    window.location.href = '/pages/admin-dashboard.html';
                } else if (response.role === 'HOSPITAL') {
                    window.location.href = '/pages/hospital-dashboard.html';
                } else {
                    window.location.href = '/pages/index.html';
                }
            }, 600);
        }
    } catch (error) {
        showAlert(error.message || 'Login failed. Please verify credentials or portal selection.', 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Sign In';
    }
}
