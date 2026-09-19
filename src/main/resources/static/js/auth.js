/**
 * Auth JavaScript logic: Login and Registration
 */

async function handleLogin(event) {
    event.preventDefault();

    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value.trim();
    const submitBtn = document.getElementById('loginSubmitBtn');

    if (!email || !password) {
        showAlert('Please enter both email and password.', 'error');
        return;
    }

    try {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Logging in...';

        const response = await apiRequest('/auth/login', 'POST', { email, password });

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
                } else {
                    window.location.href = '/pages/index.html';
                }
            }, 800);
        }
    } catch (error) {
        showAlert(error.message || 'Login failed. Please check credentials.', 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Sign In';
    }
}

async function handleRegister(event) {
    event.preventDefault();

    const name = document.getElementById('regName').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value.trim();
    const phone = document.getElementById('regPhone').value.trim();
    const dob = document.getElementById('regDob').value;
    const gender = document.getElementById('regGender').value;
    const address = document.getElementById('regAddress').value.trim();
    const submitBtn = document.getElementById('regSubmitBtn');

    if (!name || !email || !password || !phone) {
        showAlert('Please fill in all mandatory fields.', 'error');
        return;
    }

    try {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Creating Account...';

        const response = await apiRequest('/auth/register', 'POST', {
            name,
            email,
            password,
            phone,
            dob: dob || null,
            gender: gender || null,
            address: address || null
        });

        if (response.success) {
            showAlert('Registration successful! Redirecting to login...', 'success');
            setTimeout(() => {
                window.location.href = '/pages/login.html';
            }, 1000);
        }
    } catch (error) {
        showAlert(error.message || 'Registration failed.', 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Create Account';
    }
}
