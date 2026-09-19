/**
 * Patient Portal & Booking JavaScript
 */

let selectedDoctorId = null;
let selectedSlotTime = null;
let currentDoctorData = null;

// Load Specializations into Filter Dropdown
async function loadSpecializations(selectElementId = 'specFilter') {
    const select = document.getElementById(selectElementId);
    if (!select) return;

    try {
        const specs = await apiRequest('/admin/specializations');
        select.innerHTML = '<option value="">All Specializations</option>';
        specs.forEach(s => {
            select.innerHTML += `<option value="${s.id}">${s.name}</option>`;
        });
    } catch (error) {
        console.error('Failed to load specializations:', error);
    }
}

// Load Doctors with Filtering
async function loadDoctors() {
    const container = document.getElementById('doctorListContainer');
    if (!container) return;

    const specId = document.getElementById('specFilter') ? document.getElementById('specFilter').value : '';
    const search = document.getElementById('searchBox') ? document.getElementById('searchBox').value.trim() : '';

    let url = '/doctors?';
    if (specId) url += `specializationId=${specId}&`;
    if (search) url += `search=${encodeURIComponent(search)}&`;

    try {
        container.innerHTML = '<div style="text-align:center; padding: 2rem; color: var(--text-muted);">Loading doctors...</div>';
        const doctors = await apiRequest(url);

        if (doctors.length === 0) {
            container.innerHTML = '<div class="card" style="text-align:center; padding: 2rem; color: var(--text-muted);">No doctors found matching your criteria.</div>';
            return;
        }

        let html = '<div class="stats-grid">';
        doctors.forEach(doc => {
            html += `
            <div class="card" style="display: flex; flex-direction: column; justify-content: space-between;">
                <div>
                    <div style="display: flex; align-items: center; gap: 0.8rem; margin-bottom: 0.8rem;">
                        <div style="background: var(--primary-light); color: var(--primary); width: 44px; height: 44px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 1.3rem;">👨‍⚕️</div>
                        <div>
                            <h3 style="font-size: 1.1rem; color: var(--secondary);">${doc.name}</h3>
                            <span class="badge" style="background: var(--primary-light); color: var(--primary);">${doc.specializationName}</span>
                        </div>
                    </div>
                    <div style="font-size: 0.88rem; color: var(--text-muted); margin-bottom: 1rem;">
                        <p><strong>Fee:</strong> ₹${doc.consultationFee.toFixed(2)}</p>
                        <p><strong>Email:</strong> ${doc.email}</p>
                    </div>
                </div>
                <a href="/pages/book-appointment.html?doctorId=${doc.id}" class="btn btn-primary" style="width: 100%;">
                    Book Appointment
                </a>
            </div>`;
        });
        html += '</div>';
        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<div class="card" style="color: var(--danger);">Failed to load doctors: ${error.message}</div>`;
    }
}

// Initializer for Booking Page
async function initBookingPage() {
    const urlParams = new URLSearchParams(window.location.search);
    selectedDoctorId = urlParams.get('doctorId');

    if (!selectedDoctorId) {
        showAlert('No doctor selected. Redirecting to doctor directory...', 'error');
        setTimeout(() => window.location.href = '/pages/doctors.html', 1500);
        return;
    }

    // Set Default Date to Today
    const dateInput = document.getElementById('bookingDate');
    const today = new Date().toISOString().split('T')[0];
    dateInput.min = today;
    dateInput.value = today;

    await loadDoctorDetails(selectedDoctorId);
    await loadAvailableSlots();
}

// Load Selected Doctor Profile
async function loadDoctorDetails(doctorId) {
    try {
        currentDoctorData = await apiRequest(`/doctors/${doctorId}`);
        document.getElementById('docName').innerText = currentDoctorData.name;
        document.getElementById('docSpec').innerText = currentDoctorData.specializationName;
        document.getElementById('docFee').innerText = `₹${currentDoctorData.consultationFee.toFixed(2)}`;
    } catch (error) {
        showAlert('Failed to load doctor profile.', 'error');
    }
}

// Load Slots for Chosen Date
async function loadAvailableSlots() {
    const dateInput = document.getElementById('bookingDate');
    const slotContainer = document.getElementById('slotGridContainer');
    const workingHoursNotice = document.getElementById('workingHoursNotice');
    selectedSlotTime = null;
    document.getElementById('confirmBookingBtn').disabled = true;

    if (!dateInput.value) return;

    try {
        slotContainer.innerHTML = '<div style="grid-column: 1/-1; text-align:center; padding: 1.5rem; color: var(--text-muted);">Fetching available slots...</div>';
        const response = await apiRequest(`/doctors/${selectedDoctorId}/slots?date=${dateInput.value}`);

        workingHoursNotice.innerText = `Schedule for ${response.dayOfWeek}: ${response.workingHours}`;

        if (!response.slots || response.slots.length === 0) {
            slotContainer.innerHTML = `<div style="grid-column: 1/-1; text-align:center; padding: 1.5rem; color: var(--danger);">Doctor has no available working slots on this day (${response.dayOfWeek}). Please choose another date.</div>`;
            return;
        }

        let html = '';
        response.slots.forEach(slot => {
            if (slot.available) {
                html += `<button type="button" class="slot-btn available" onclick="selectSlot('${slot.time}', this)">
                            ${slot.displayTime}
                         </button>`;
            } else {
                html += `<button type="button" class="slot-btn booked" disabled title="Booked or Past Slot">
                            ${slot.displayTime}
                         </button>`;
            }
        });

        slotContainer.innerHTML = html;
    } catch (error) {
        slotContainer.innerHTML = `<div style="grid-column: 1/-1; color: var(--danger);">Error loading slots: ${error.message}</div>`;
    }
}

// Select a Slot Button
function selectSlot(timeStr, btnElement) {
    document.querySelectorAll('.slot-btn').forEach(b => b.classList.remove('selected'));
    btnElement.classList.add('selected');
    selectedSlotTime = timeStr;
    document.getElementById('confirmBookingBtn').disabled = false;
}

// Submit Appointment Booking
async function submitBooking() {
    const user = getCurrentUser();
    if (!user || user.role !== 'PATIENT') {
        showAlert('Please log in as a Patient to book an appointment.', 'error');
        setTimeout(() => window.location.href = '/pages/login.html', 1200);
        return;
    }

    const date = document.getElementById('bookingDate').value;
    if (!selectedSlotTime) {
        showAlert('Please select an appointment time slot.', 'error');
        return;
    }

    const confirmBtn = document.getElementById('confirmBookingBtn');

    try {
        confirmBtn.disabled = true;
        confirmBtn.textContent = 'Processing Booking...';

        const payload = {
            patientId: user.profileId,
            doctorId: parseInt(selectedDoctorId),
            appointmentDate: date,
            appointmentTime: selectedSlotTime
        };

        const response = await apiRequest('/appointments', 'POST', payload);

        // Show Token Confirmation Modal
        document.getElementById('modalTokenNumber').innerText = `#${response.tokenNumber}`;
        document.getElementById('modalDocName').innerText = response.doctorName;
        document.getElementById('modalDateTime').innerText = `${response.appointmentDate} at ${response.displayTime}`;
        document.getElementById('bookingSuccessModal').classList.add('active');

    } catch (error) {
        showAlert(error.message || 'Double booking error: Slot is already taken.', 'error');
        // Refresh slots to show updated state
        await loadAvailableSlots();
    } finally {
        confirmBtn.disabled = false;
        confirmBtn.textContent = 'Confirm & Book Slot';
    }
}

// Load Patient Bookings
async function loadPatientAppointments() {
    const user = getCurrentUser();
    if (!user || user.role !== 'PATIENT') return;

    const container = document.getElementById('patientAppointmentsTable');
    if (!container) return;

    try {
        const list = await apiRequest(`/appointments/patient/${user.profileId}`);
        if (list.length === 0) {
            container.innerHTML = '<tr><td colspan="7" style="text-align: center; color: var(--text-muted); padding: 2rem;">No appointments booked yet.</td></tr>';
            return;
        }

        let html = '';
        list.forEach(apt => {
            let badgeClass = 'badge-booked';
            if (apt.status === 'IN_CONSULTATION') badgeClass = 'badge-consultation';
            if (apt.status === 'COMPLETED') badgeClass = 'badge-completed';
            if (apt.status === 'CANCELLED') badgeClass = 'badge-cancelled';

            let actionBtn = '';
            if (apt.status === 'BOOKED') {
                actionBtn = `<button class="btn btn-sm btn-danger" onclick="cancelAppointment(${apt.id})">Cancel</button>`;
            } else if (apt.status === 'COMPLETED') {
                actionBtn = `<a href="/pages/view-prescription.html?appointmentId=${apt.id}" class="btn btn-sm btn-success">View Rx</a>`;
            } else {
                actionBtn = `<span style="color: var(--text-muted); font-size: 0.85rem;">-</span>`;
            }

            html += `
            <tr>
                <td><strong>Token #${apt.tokenNumber}</strong></td>
                <td>${apt.doctorName}<br><small style="color:var(--text-muted);">${apt.specialization}</small></td>
                <td>${apt.appointmentDate}</td>
                <td>${apt.displayTime}</td>
                <td>₹${apt.consultationFee.toFixed(2)}</td>
                <td><span class="badge ${badgeClass}">${apt.status.replace('_', ' ')}</span></td>
                <td>${actionBtn}</td>
            </tr>`;
        });

        container.innerHTML = html;
    } catch (error) {
        container.innerHTML = `<tr><td colspan="7" style="color: var(--danger);">Failed to load appointments: ${error.message}</td></tr>`;
    }
}

async function cancelAppointment(aptId) {
    if (!confirm('Are you sure you want to cancel this appointment? This will release the slot.')) {
        return;
    }

    try {
        const res = await apiRequest(`/appointments/${aptId}/cancel`, 'PUT');
        showAlert(res.message, 'success');
        await loadPatientAppointments();
    } catch (error) {
        showAlert(error.message || 'Failed to cancel appointment.', 'error');
    }
}
