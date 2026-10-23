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
            tableBody.innerHTML = '<tr><td colspan="4" style="text-align:center; color: var(--text-muted); padding: 2rem;">No specializations added yet.</td></tr>';
            return;
        }

        let html = '';
        list.forEach((s, idx) => {
            html += `
            <tr>
                <td>${idx + 1}</td>
                <td><strong>${s.name}</strong></td>
                <td>${s.description || '-'}</td>
                <td style="text-align: center;">
                    <button class="btn btn-sm" style="background:#fee2e2; color:#b91c1c; border:1px solid #fca5a5; font-weight:600; padding:0.25rem 0.65rem;" onclick="handleDeleteSpecialization(${s.id}, '${s.name.replace(/'/g, "\\'")}')">
                        🗑️ Remove
                    </button>
                </td>
            </tr>`;
        });
        tableBody.innerHTML = html;
    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="4" style="color:var(--danger);">Error: ${error.message}</td></tr>`;
    }
}

// Delete Specialization with referential integrity protection
async function handleDeleteSpecialization(id, name) {
    if (!confirm(`Are you sure you want to remove the medical specialization "${name}"?\n\nNote: If this specialization is currently assigned to doctors or historical appointments, removal will be prevented to protect clinical records.`)) {
        return;
    }

    try {
        await apiRequest(`/admin/specializations/${id}`, 'DELETE');
        showAlert(`Specialization "${name}" removed successfully.`, 'success');
        await loadAdminSpecializations();
    } catch (error) {
        showAlert(error.message || `Could not remove specialization "${name}".`, 'error');
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

let cachedAdminDoctors = [];

// Load Doctor Overview on Admin Home Page (Phase 7)
async function loadDoctorOverview() {
    const tableBody = document.getElementById('adminDoctorOverviewTable');
    if (!tableBody) return;

    try {
        const doctors = await apiRequest('/doctors?status=ALL');
        if (!doctors || doctors.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--text-muted); padding: 2rem;">No doctors registered in the clinic yet.</td></tr>';
            return;
        }

        let html = '';
        doctors.forEach(doc => {
            const status = doc.approvalStatus || 'APPROVED';
            let statusBadge = '';
            if (status === 'PENDING_APPROVAL') {
                statusBadge = '<span class="badge" style="background:#fef3c7; color:#b45309; border:1px solid #fde68a; font-weight:700;">⏳ PENDING APPROVAL</span>';
            } else if (status === 'APPROVED') {
                statusBadge = '<span class="badge" style="background:#ecfdf5; color:#059669; border:1px solid #a7f3d0; font-weight:700;">✅ ACTIVE / APPROVED</span>';
            } else {
                statusBadge = '<span class="badge" style="background:#fee2e2; color:#dc2626; border:1px solid #fca5a5; font-weight:700;">⛔ INACTIVE</span>';
            }

            const availBadge = doc.available 
                ? '<span class="badge" style="background:#ecfdf5; color:#059669;">🟢 Online / Available</span>' 
                : '<span class="badge" style="background:#f1f5f9; color:#64748b;">⚪ Offline</span>';

            html += `
            <tr>
                <td>
                    <strong>${doc.name}</strong><br>
                    <small style="color:var(--text-muted);">${doc.phone ? '📞 ' + doc.phone + ' • ' : ''}${doc.email}</small>
                </td>
                <td><span class="badge" style="background:var(--primary-light); color:var(--primary); font-weight:600;">${doc.specializationName || 'Unassigned'}</span></td>
                <td><strong>₹${(doc.consultationFee || 0).toFixed(2)}</strong></td>
                <td>${statusBadge}</td>
                <td>${availBadge}</td>
                <td><small style="color:#0f172a;">${doc.todayWorkingHours || '09:00 AM - 10:00 PM (Lunch: 1:00-2:30 PM)'}</small></td>
                <td style="text-align: center;">
                    <a href="/pages/manage-doctors.html" class="btn btn-sm btn-outline" style="font-size:0.8rem; padding: 0.3rem 0.6rem;">Review / Edit</a>
                </td>
            </tr>`;
        });
        tableBody.innerHTML = html;
    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="7" style="color:var(--danger); text-align:center; padding: 2rem;">Error loading doctor overview: ${error.message}</td></tr>`;
    }
}

// Load Doctors in Admin Table with Status Badges and Activation
async function loadAdminDoctors() {
    const tableBody = document.getElementById('adminDoctorsTable');
    if (!tableBody) return;

    try {
        const doctors = await apiRequest('/doctors?status=ALL');
        cachedAdminDoctors = doctors || [];

        if (doctors.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="7" style="text-align:center; color: var(--text-muted); padding: 2rem;">No doctors registered in the system yet.</td></tr>';
            return;
        }

        let html = '';
        doctors.forEach(doc => {
            const status = doc.approvalStatus || 'APPROVED';
            let statusBadge = '';
            if (status === 'PENDING_APPROVAL') {
                statusBadge = '<span class="badge" style="background:#fef3c7; color:#b45309; border:1px solid #fde68a; font-weight:700;">⏳ PENDING APPROVAL</span>';
            } else if (status === 'APPROVED') {
                statusBadge = '<span class="badge" style="background:#ecfdf5; color:#059669; border:1px solid #a7f3d0; font-weight:700;">✅ APPROVED</span>';
            } else {
                statusBadge = '<span class="badge" style="background:#fee2e2; color:#dc2626; border:1px solid #fca5a5; font-weight:700;">⛔ INACTIVE</span>';
            }

            let actionButtons = '';
            if (status === 'PENDING_APPROVAL') {
                actionButtons = `
                    <button class="btn btn-sm btn-primary" style="font-weight:700;" onclick="openEditDoctorModal(${doc.id})">
                        🔍 Review & Activate
                    </button>
                `;
            } else if (status === 'APPROVED') {
                actionButtons = `
                    <button class="btn btn-sm btn-outline" onclick="openEditDoctorModal(${doc.id})">
                        ✏️ Edit Details
                    </button>
                    <button class="btn btn-sm" style="background:#fee2e2; color:#b91c1c; border:1px solid #fca5a5;" onclick="toggleDoctorStatus(${doc.id}, false)">
                        Deactivate
                    </button>
                `;
            } else {
                actionButtons = `
                    <button class="btn btn-sm btn-outline" onclick="openEditDoctorModal(${doc.id})">
                        ✏️ Edit Details
                    </button>
                    <button class="btn btn-sm btn-primary" onclick="toggleDoctorStatus(${doc.id}, true)">
                        Activate
                    </button>
                `;
            }

            html += `
            <tr>
                <td><strong>${doc.name}</strong></td>
                <td>
                    ${doc.phone ? '📞 ' + doc.phone + '<br>' : ''}
                    <small style="color:var(--text-muted);">${doc.email}</small>
                </td>
                <td><span class="badge" style="background:var(--primary-light); color:var(--primary);">${doc.specializationName || '<em style="color:#f59e0b;">Unassigned</em>'}</span></td>
                <td>₹${(doc.consultationFee || 0).toFixed(2)}</td>
                <td>${statusBadge}</td>
                <td>${doc.available ? '<span class="badge badge-completed">ONLINE</span>' : '<span class="badge badge-cancelled">OFFLINE</span>'}</td>
                <td style="display:flex; gap:0.4rem; justify-content:center; flex-wrap:wrap;">
                    ${actionButtons}
                </td>
            </tr>`;
        });
        tableBody.innerHTML = html;
    } catch (error) {
        tableBody.innerHTML = `<tr><td colspan="7" style="color:var(--danger);">Error loading doctors: ${error.message}</td></tr>`;
    }
}

// Toggle Doctor Status
async function toggleDoctorStatus(docId, newStatus) {
    try {
        const newApprovalStatus = newStatus ? 'APPROVED' : 'INACTIVE';
        await apiRequest(`/admin/doctors/${docId}`, 'PUT', { isAvailable: newStatus, approvalStatus: newApprovalStatus });
        showAlert(`Doctor ${newStatus ? 'activated' : 'deactivated'} successfully.`, 'success');
        await loadAdminDoctors();
    } catch (error) {
        showAlert(error.message || 'Failed to update doctor status.', 'error');
    }
}

// Open Edit / Review Doctor Modal
function openEditDoctorModal(doctorId) {
    const doc = cachedAdminDoctors.find(d => d.id === doctorId);
    if (!doc) {
        showAlert('Doctor details not found. Please refresh the page.', 'error');
        return;
    }

    document.getElementById('editDoctorId').value = doc.id;
    document.getElementById('editDoctorName').value = doc.name;
    document.getElementById('editDoctorEmail').value = doc.email;
    document.getElementById('editDoctorPhone').value = doc.phone || '';

    // Set specialization if already assigned
    const specSelect = document.getElementById('editDocSpecSelect');
    if (specSelect) {
        specSelect.value = doc.specializationId || '';
    }

    // Set fee
    document.getElementById('editDocFeeInput').value = doc.consultationFee || 500;

    // Set status
    const statusSelect = document.getElementById('editDocStatusSelect');
    if (statusSelect) {
        statusSelect.value = doc.approvalStatus || (doc.available ? 'APPROVED' : 'PENDING_APPROVAL');
    }

    // Default times
    document.getElementById('editDocStartTime').value = '09:00';
    document.getElementById('editDocEndTime').value = '22:00';

    document.getElementById('editDoctorModal').classList.add('active');
}

// Handle Update Doctor Details & Activation
async function handleUpdateDoctor(event) {
    event.preventDefault();

    const docId = document.getElementById('editDoctorId').value;
    const phone = document.getElementById('editDoctorPhone').value.trim();
    const specId = document.getElementById('editDocSpecSelect').value;
    const fee = document.getElementById('editDocFeeInput').value;
    const approvalStatus = document.getElementById('editDocStatusSelect').value;
    const startTimeStr = document.getElementById('editDocStartTime').value + ':00';
    const endTimeStr = document.getElementById('editDocEndTime').value + ':00';

    if (!specId) {
        showAlert('Please assign a medical specialization to this doctor before saving.', 'error');
        return;
    }
    if (!fee || parseFloat(fee) <= 0) {
        showAlert('Please set a valid consultation fee.', 'error');
        return;
    }

    // Gather selected days
    const dayCheckboxes = ['dayMon', 'dayTue', 'dayWed', 'dayThu', 'dayFri', 'daySat', 'daySun'];
    const availabilities = [];
    dayCheckboxes.forEach(id => {
        const el = document.getElementById(id);
        if (el && el.checked) {
            availabilities.push({
                dayOfWeek: el.value,
                startTime: startTimeStr,
                endTime: endTimeStr
            });
        }
    });

    const isAvailable = (approvalStatus === 'APPROVED');

    const payload = {
        phone: phone,
        specializationId: parseInt(specId),
        consultationFee: parseFloat(fee),
        approvalStatus: approvalStatus,
        isAvailable: isAvailable,
        availabilities: availabilities
    };

    try {
        await apiRequest(`/admin/doctors/${docId}`, 'PUT', payload);
        showAlert(`Doctor configuration saved successfully! (Status: ${approvalStatus})`, 'success');
        document.getElementById('editDoctorModal').classList.remove('active');
        await loadAdminDoctors();
    } catch (error) {
        showAlert(error.message || 'Failed to update doctor profile.', 'error');
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
