/**
 * Clinic Appointment System - API Utility & Global Session Manager
 */

const API_BASE = '/api';

// Session Storage Helpers
function getCurrentUser() {
    const userJson = localStorage.getItem('clinic_user');
    return userJson ? JSON.parse(userJson) : null;
}

function setCurrentUser(user) {
    localStorage.setItem('clinic_user', JSON.stringify(user));
}

function clearCurrentUser() {
    localStorage.removeItem('clinic_user');
}

// Centralized Fetch Wrapper
async function apiRequest(endpoint, method = 'GET', body = null) {
    const headers = {
        'Content-Type': 'application/json'
    };

    const options = {
        method: method,
        headers: headers
    };

    if (body) {
        options.body = JSON.stringify(body);
    }

    try {
        const response = await fetch(`${API_BASE}${endpoint}`, options);
        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(data.message || `Request failed with status ${response.status}`);
        }

        return data;
    } catch (error) {
        console.error(`API Error [${method} ${endpoint}]:`, error);
        throw error;
    }
}

// Global Notification Alert Toast
function showAlert(message, type = 'success') {
    let alertBox = document.getElementById('globalAlert');
    if (!alertBox) {
        alertBox = document.createElement('div');
        alertBox.id = 'globalAlert';
        alertBox.style.position = 'fixed';
        alertBox.style.top = '20px';
        alertBox.style.right = '20px';
        alertBox.style.zIndex = '9999';
        alertBox.style.padding = '12px 24px';
        alertBox.style.borderRadius = '8px';
        alertBox.style.fontWeight = '600';
        alertBox.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
        alertBox.style.transition = 'all 0.3s ease';
        document.body.appendChild(alertBox);
    }

    if (type === 'success') {
        alertBox.style.backgroundColor = '#10b981';
        alertBox.style.color = '#ffffff';
    } else if (type === 'error') {
        alertBox.style.backgroundColor = '#ef4444';
        alertBox.style.color = '#ffffff';
    } else {
        alertBox.style.backgroundColor = '#0284c7';
        alertBox.style.color = '#ffffff';
    }

    alertBox.textContent = message;
    alertBox.style.display = 'block';

    setTimeout(() => {
        alertBox.style.display = 'none';
    }, 4000);
}

// Render dynamic Navbar based on User Role
function renderNavbar() {
    const user = getCurrentUser();
    const navContainer = document.getElementById('navLinks');
    if (!navContainer) return;

    let emergencyLink = '<li><a href="/pages/emergency.html" class="nav-sos-btn">🚨 Emergency SOS</a></li>';
    if (user) {
        if (user.role === 'DOCTOR') {
            emergencyLink = '<li><a href="/pages/doctor-dashboard.html#doctorEmergencyCard" class="nav-sos-btn">🚨 Emergency Cases</a></li>';
        } else if (user.role === 'ADMIN') {
            emergencyLink = ''; // Phase 7: Remove Emergency SOS action from Admin portal
        } else if (user.role === 'HOSPITAL') {
            emergencyLink = '<li><a href="/pages/hospital-dashboard.html" class="nav-sos-btn">🚨 Hospital Dispatch</a></li>';
        }
    }

    // Phase 7: If on home page and logged in as ADMIN, replace Book Appointment & Emergency SOS with Admin actions
    if (user && user.role === 'ADMIN') {
        const heroBtnGroup = document.getElementById('heroActionGroup') || document.querySelector('.card[style*="linear-gradient"] div[style*="flex"]');
        if (heroBtnGroup && (window.location.pathname.endsWith('index.html') || window.location.pathname === '/' || window.location.pathname === '')) {
            heroBtnGroup.innerHTML = `
                <a href="/pages/admin-dashboard.html" class="btn" style="background: white; color: #0284c7; font-weight: 700; padding: 0.8rem 1.6rem;">Central Admin Console</a>
                <a href="/pages/manage-doctors.html" class="btn" style="background: rgba(255,255,255,0.2); color: white; border: 1px solid rgba(255,255,255,0.4); padding: 0.8rem 1.6rem;">Doctor Management &amp; Approvals</a>
            `;
        }
    }


    let linksHtml = `<li><a href="/pages/index.html">Home</a></li>
                     <li><a href="/pages/doctors.html">Doctors</a></li>
                     <li><a href="/pages/waiting-room.html">Waiting Room</a></li>
                     ${emergencyLink}`;

    if (!user) {
        linksHtml += `<li><a href="/pages/login.html" class="btn btn-sm btn-outline">Login</a></li>
                      <li><a href="/pages/register.html" class="btn btn-sm btn-primary">Register</a></li>`;
    } else {
        if (user.role === 'PATIENT') {
            linksHtml += `<li><a href="/pages/patient-dashboard.html">Dashboard</a></li>
                          <li><a href="/pages/my-appointments.html">My Bookings</a></li>`;
        } else if (user.role === 'DOCTOR') {
            linksHtml += `<li><a href="/pages/doctor-dashboard.html">Doctor Dashboard</a></li>`;
        } else if (user.role === 'ADMIN') {
            linksHtml += `<li><a href="/pages/admin-dashboard.html">Admin Dashboard</a></li>`;
        } else if (user.role === 'HOSPITAL') {
            linksHtml += `<li><a href="/pages/hospital-dashboard.html">Dispatch Console</a></li>`;
        }

        linksHtml += `<li>
            <span class="user-badge">👤 ${user.name} (${user.role})</span>
        </li>
        <li><a href="javascript:void(0)" onclick="logoutUser()" class="btn btn-sm btn-secondary">Logout</a></li>`;
    }

    navContainer.innerHTML = linksHtml;
}

function logoutUser() {
    if (typeof stopRepeatingSiren === 'function') {
        stopRepeatingSiren();
    }
    sessionStorage.clear();
    clearCurrentUser();
    showAlert('Logged out successfully.', 'info');
    setTimeout(() => {
        window.location.href = '/pages/login.html';
    }, 500);
}

// Role Route Guard
function checkAuthGuard(allowedRoles = []) {
    const user = getCurrentUser();
    if (!user) {
        window.location.href = '/pages/login.html';
        return false;
    }
    if (allowedRoles.length > 0 && !allowedRoles.includes(user.role)) {
        alert('Access Denied: You do not have permission to view this page.');
        window.location.href = '/pages/index.html';
        return false;
    }
    return true;
}

document.addEventListener('DOMContentLoaded', () => {
    renderNavbar();
});
