/**
 * Admin Dashboard & Clinic Management
 */

// Load Admin Summary Stats
async function loadAdminStats() {
    try {
        const stats = await apiRequest('/admin/stats');
        document.getElementById('statTotalDoctors').innerText = stats.totalDoctors;
        document.getElementById('statTotalPatients').innerText = stats.totalPatients;
        document.getElementById('statTodayApts').innerText = stats.todayAppointments;
        document.getElementById('statCompletedApts').innerText = stats.totalCompletedAppointments;
    } catch (error) {
        console.error('Failed to load admin stats:', error);
    }
}

// Load Specializations Table
async function loadAdminSpecializations() {
    const tableBody = document.getElementById('specTableBody');
    if (!tableBody) return;

    try {
        const list = await apiRequest('/admin/specializations');
        if (list.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="3" style="text-align:center; color: var(--text-muted);">No specializations added yet.</td></tr>';
            return;
        }

        let html = '';
        list.forEach((s, idx) => {
            html += `
            <tr>
                <td>${idx + 1}</td>
                <td><strong>${s.name}</strong></td>
                <td>${s.description || '-'}</td>
            </tr>`;
        });
        tableBody.innerHTML = html;
    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="3" style="color:var(--danger);">Error: ${error.message}</td></tr>`;
    }
}

// Create Specialization
async function handleCreateSpecialization(event) {
    event.preventDefault();
    const name = document.getElementById('newSpecName').value.trim();
    const desc = document.getElementById('newSpecDesc').value.trim();

    if (!name) {
        showAlert('Specialization name is required.', 'error');
        return;
    }

    try {
        await apiRequest('/admin/specializations', 'POST', { name, description: desc });
        showAlert('Specialization added successfully!', 'success');
        document.getElementById('newSpecName').value = '';
        document.getElementById('newSpecDesc').value = '';
        await loadAdminSpecializations();
    } catch (error) {
        showAlert(error.message || 'Failed to add specialization.', 'error');
    }
}

// Load Doctors in Admin Table
async function loadAdminDoctors() {
    const tableBody = document.getElementById('adminDoctorsTable');
    if (!tableBody) return;

    try {
        const doctors = await apiRequest('/doctors');
        if (doctors.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="6" style="text-align:center; color: var(--text-muted);">No doctors added yet.</td></tr>';
            return;
        }

        let html = '';
        doctors.forEach(doc => {
            html += `
            <tr>
                <td><strong>${doc.name}</strong></td>
                <td><span class="badge" style="background:var(--primary-light); color:var(--primary);">${doc.specializationName}</span></td>
                <td>${doc.email}</td>
                <td>₹${doc.consultationFee.toFixed(2)}</td>
                <td>${doc.available ? '<span class="badge badge-completed">ACTIVE</span>' : '<span class="badge badge-cancelled">INACTIVE</span>'}</td>
                <td>
                    <button class="btn btn-sm btn-outline" onclick="toggleDoctorStatus(${doc.id}, ${!doc.available})">
                        ${doc.available ? 'Deactivate' : 'Activate'}
                    </button>
                </td>
            </tr>`;
        });
        tableBody.innerHTML = html;
    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="6" style="color:var(--danger);">Error loading doctors: ${error.message}</td></tr>`;
    }
}

// Toggle Doctor Status
async function toggleDoctorStatus(docId, newStatus) {
    try {
        await apiRequest(`/admin/doctors/${docId}`, 'PUT', { isAvailable: newStatus });
        showAlert(`Doctor ${newStatus ? 'activated' : 'deactivated'} successfully.`, 'success');
        await loadAdminDoctors();
    } catch (error) {
        showAlert(error.message || 'Failed to update doctor status.', 'error');
    }
}

// Handle Add New Doctor
async function handleCreateDoctor(event) {
    event.preventDefault();

    const name = document.getElementById('docNameInput').value.trim();
    const email = document.getElementById('docEmailInput').value.trim();
    const password = document.getElementById('docPasswordInput').value.trim();
    const specId = document.getElementById('docSpecSelect').value;
    const fee = document.getElementById('docFeeInput').value;

    if (!name || !email || !password || !specId || !fee) {
        showAlert('Please fill in all mandatory doctor fields.', 'error');
        return;
    }

    // Default Mon-Sat 09:00 AM - 01:00 PM availability
    const availabilities = [
        { dayOfWeek: 'MONDAY', startTime: '09:00:00', endTime: '13:00:00' },
        { dayOfWeek: 'TUESDAY', startTime: '09:00:00', endTime: '13:00:00' },
        { dayOfWeek: 'WEDNESDAY', startTime: '09:00:00', endTime: '13:00:00' },
        { dayOfWeek: 'THURSDAY', startTime: '09:00:00', endTime: '13:00:00' },
        { dayOfWeek: 'FRIDAY', startTime: '09:00:00', endTime: '13:00:00' },
        { dayOfWeek: 'SATURDAY', startTime: '09:00:00', endTime: '13:00:00' }
    ];

    try {
        const payload = {
            name,
            email,
            password,
            specializationId: parseInt(specId),
            consultationFee: parseFloat(fee),
            availabilities
        };

        await apiRequest('/admin/doctors', 'POST', payload);
        showAlert('Doctor added successfully with default schedule (Mon-Sat 9AM-1PM)!', 'success');
        document.getElementById('addDoctorModal').classList.remove('active');
        document.getElementById('addDoctorForm').reset();
        await loadAdminDoctors();
    } catch (error) {
        showAlert(error.message || 'Failed to create doctor.', 'error');
    }
}

// Load Master Appointment Audit Log
async function loadAdminAppointments() {
    const tableBody = document.getElementById('allAppointmentsTable');
    if (!tableBody) return;

    try {
        const list = await apiRequest('/admin/appointments');
        if (list.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">No appointments recorded.</td></tr>';
            return;
        }

        let html = '';
        list.forEach(apt => {
            let badgeClass = 'badge-booked';
            if (apt.status === 'IN_CONSULTATION') badgeClass = 'badge-consultation';
            if (apt.status === 'COMPLETED') badgeClass = 'badge-completed';
            if (apt.status === 'CANCELLED') badgeClass = 'badge-cancelled';

            html += `
            <tr>
                <td>#${apt.id}</td>
                <td>Token #${apt.tokenNumber}</td>
                <td>${apt.patientName}<br><small style="color:var(--text-muted);">${apt.patientPhone || ''}</small></td>
                <td>${apt.doctorName}<br><small style="color:var(--text-muted);">${apt.specialization}</small></td>
                <td>${apt.appointmentDate} at ${apt.displayTime}</td>
                <td><span class="badge ${badgeClass}">${apt.status.replace('_', ' ')}</span></td>
                <td>
                    ${apt.status === 'COMPLETED' ? `<a href="/pages/view-prescription.html?appointmentId=${apt.id}" class="btn btn-sm btn-outline">View Rx</a>` : '-'}
                </td>
            </tr>`;
        });
        tableBody.innerHTML = html;
    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="7" style="color:var(--danger);">Error loading audit log: ${error.message}</td></tr>`;
    }
}
