/**
 * Doctor Dashboard & Consultation Management
 */

let currentDoctorQueue = [];
let activeConsultationAppointment = null;

// Load Doctor Dashboard Queue & Live Stats
async function loadDoctorQueue() {
    const user = getCurrentUser();
    if (!user || user.role !== 'DOCTOR') return;

    const doctorId = user.profileId;
    const dateInput = document.getElementById('filterDate');
    const selectedDate = dateInput && dateInput.value ? dateInput.value : new Date().toISOString().split('T')[0];

    try {
        const data = await apiRequest(`/queue/doctor/${doctorId}?date=${selectedDate}`);
        currentDoctorQueue = data.queue || [];

        // Update Stat Cards
        document.getElementById('statCurrentToken').innerText = data.currentToken ? `#${data.currentToken}` : 'None';
        document.getElementById('statNextToken').innerText = data.nextToken ? `#${data.nextToken}` : 'None';
        document.getElementById('statWaitingCount').innerText = data.waitingCount;

        const tableBody = document.getElementById('doctorQueueTable');
        if (!tableBody) return;

        if (currentDoctorQueue.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 2rem;">No appointments scheduled for this date.</td></tr>';
            return;
        }

        let html = '';
        currentDoctorQueue.forEach(apt => {
            let badgeClass = 'badge-booked';
            if (apt.status === 'IN_CONSULTATION') badgeClass = 'badge-consultation';
            if (apt.status === 'COMPLETED') badgeClass = 'badge-completed';
            if (apt.status === 'CANCELLED') badgeClass = 'badge-cancelled';

            let actionHtml = '';
            if (apt.status === 'IN_CONSULTATION') {
                actionHtml = `<a href="/pages/consultation.html?appointmentId=${apt.id}" class="btn btn-sm btn-success">Prescribe & Complete</a>`;
            } else if (apt.status === 'BOOKED') {
                actionHtml = `<span style="color: var(--text-muted); font-size: 0.85rem;">Waiting in line</span>`;
            } else if (apt.status === 'COMPLETED') {
                actionHtml = `<a href="/pages/view-prescription.html?appointmentId=${apt.id}" class="btn btn-sm btn-outline">View Rx</a>`;
            } else {
                actionHtml = `<span style="color: var(--text-muted); font-size: 0.85rem;">Cancelled</span>`;
            }

            html += `
            <tr style="${apt.status === 'IN_CONSULTATION' ? 'background-color: #f0fdf4; font-weight: 600;' : ''}">
                <td><strong style="font-size: 1.05rem; color: var(--primary);">#${apt.tokenNumber}</strong></td>
                <td>${apt.patientName}<br><small style="color:var(--text-muted);">📞 ${apt.patientPhone || 'N/A'}</small></td>
                <td>${apt.displayTime}</td>
                <td><span class="badge ${badgeClass}">${apt.status.replace('_', ' ')}</span></td>
                <td>${actionHtml}</td>
            </tr>`;
        });

        tableBody.innerHTML = html;
    } catch (error) {
        console.error('Failed to load queue:', error);
    }
}

// Doctor Calls Next Patient
async function callNextPatient() {
    const user = getCurrentUser();
    if (!user || user.role !== 'DOCTOR') return;

    const callBtn = document.getElementById('callNextBtn');

    try {
        callBtn.disabled = true;
        callBtn.textContent = 'Calling Next...';

        const apt = await apiRequest(`/queue/doctor/${user.profileId}/call-next`, 'PUT');
        showAlert(`Token #${apt.tokenNumber} (${apt.patientName}) called into consultation!`, 'success');

        // Automatically load consultation page for called patient
        setTimeout(() => {
            window.location.href = `/pages/consultation.html?appointmentId=${apt.id}`;
        }, 1200);

    } catch (error) {
        showAlert(error.message || 'No waiting patients found.', 'error');
        await loadDoctorQueue();
    } finally {
        callBtn.disabled = false;
        callBtn.textContent = '📢 Call Next Patient';
    }
}

// Initializer for Consultation Page
async function initConsultationPage() {
    const urlParams = new URLSearchParams(window.location.search);
    const appointmentId = urlParams.get('appointmentId');

    if (!appointmentId) {
        showAlert('No appointment selected.', 'error');
        setTimeout(() => window.location.href = '/pages/doctor-dashboard.html', 1200);
        return;
    }

    try {
        const apt = await apiRequest(`/appointments/${appointmentId}`);
        activeConsultationAppointment = apt;

        document.getElementById('patientNameBanner').innerText = apt.patientName;
        document.getElementById('patientPhoneBanner').innerText = `Phone: ${apt.patientPhone || 'N/A'}`;
        document.getElementById('tokenBanner').innerText = `Token #${apt.tokenNumber}`;
        document.getElementById('appointmentTimeBanner').innerText = `${apt.appointmentDate} at ${apt.displayTime}`;

        // Add first medicine row by default
        addMedicineRow();

    } catch (error) {
        showAlert('Failed to load consultation appointment details.', 'error');
    }
}

// Add Dynamic Medicine Row to Prescription Form
function addMedicineRow() {
    const tableBody = document.getElementById('medicineTableBody');
    if (!tableBody) return;

    const rowCount = tableBody.children.length;
    const row = document.createElement('tr');
    row.innerHTML = `
        <td><input type="text" class="form-control med-name" placeholder="e.g. Paracetamol 500mg" required></td>
        <td><input type="text" class="form-control med-dosage" placeholder="e.g. 1 tab" required></td>
        <td><input type="text" class="form-control med-frequency" placeholder="e.g. 1-0-1 (Twice daily)" required></td>
        <td><input type="text" class="form-control med-duration" placeholder="e.g. 5 days" required></td>
        <td><input type="text" class="form-control med-instructions" placeholder="e.g. After meals"></td>
        <td style="text-align: center;">
            <button type="button" class="btn btn-sm btn-danger" onclick="removeMedicineRow(this)">✕</button>
        </td>
    `;
    tableBody.appendChild(row);
}

function removeMedicineRow(button) {
    const tableBody = document.getElementById('medicineTableBody');
    if (tableBody.children.length > 1) {
        button.closest('tr').remove();
    } else {
        showAlert('At least one medication row or empty prescription is required.', 'info');
    }
}

// Submit Prescription & Complete Consultation
async function submitPrescription(event) {
    event.preventDefault();

    if (!activeConsultationAppointment) {
        showAlert('Invalid appointment session.', 'error');
        return;
    }

    const diagnosis = document.getElementById('prescDiagnosis').value.trim();
    const notes = document.getElementById('prescNotes').value.trim();
    const submitBtn = document.getElementById('submitPrescBtn');

    if (!diagnosis) {
        showAlert('Please enter a clinical diagnosis.', 'error');
        return;
    }

    // Collect Medicine items
    const medRows = document.querySelectorAll('#medicineTableBody tr');
    const medicines = [];

    medRows.forEach(row => {
        const name = row.querySelector('.med-name').value.trim();
        const dosage = row.querySelector('.med-dosage').value.trim();
        const freq = row.querySelector('.med-frequency').value.trim();
        const duration = row.querySelector('.med-duration').value.trim();
        const instructions = row.querySelector('.med-instructions').value.trim();

        if (name && dosage && freq && duration) {
            medicines.push({
                medicineName: name,
                dosage: dosage,
                frequency: freq,
                duration: duration,
                instructions: instructions
            });
        }
    });

    try {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Saving Prescription...';

        const payload = {
            appointmentId: activeConsultationAppointment.id,
            diagnosis: diagnosis,
            notes: notes,
            medicines: medicines
        };

        const response = await apiRequest('/prescriptions', 'POST', payload);
        showAlert('Prescription saved & consultation completed successfully!', 'success');

        setTimeout(() => {
            window.location.href = `/pages/view-prescription.html?appointmentId=${activeConsultationAppointment.id}`;
        }, 1000);

    } catch (error) {
        showAlert(error.message || 'Failed to complete consultation.', 'error');
        submitBtn.disabled = false;
        submitBtn.textContent = 'Save Prescription & Finish Consultation';
    }
}
