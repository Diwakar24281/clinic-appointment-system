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

// ==================== DOCTOR EMERGENCY MONITORING ====================
let doctorAlertAudioContext = null;
let activeDoctorAlert = null;
let doctorSirenInterval = null;
let doctorAudioUnlocked = false;
let acknowledgedEmergencyIds = new Set();
let doctorEmergencyPollInterval = null;
let doctorCrossTabSyncAttached = false;
let doctorAudioInteractionAttached = false;

function getDoctorAlertPrefKey(doctorId) {
    const user = getCurrentUser();
    const docId = doctorId || (user ? (user.profileId || user.id) : null) || 'default';
    return `doctor_alert_sound_enabled_${docId}`;
}

function isDoctorAlertSoundEnabled(doctorId) {
    const key = getDoctorAlertPrefKey(doctorId);
    return localStorage.getItem(key) === 'true';
}

function setDoctorAlertSoundEnabled(enabled, doctorId) {
    const key = getDoctorAlertPrefKey(doctorId);
    localStorage.setItem(key, enabled ? 'true' : 'false');
}

function updateDoctorAlertUI(isEnabled) {
    const btn = document.getElementById('enableAudioAlertBtn');
    if (btn) {
        if (isEnabled) {
            btn.innerHTML = '🔕 Disable Emergency Alert Sound';
            btn.style.background = 'rgba(16, 185, 129, 0.2)';
            btn.style.borderColor = '#10b981';
            btn.style.color = '#34d399';
            btn.title = 'Click to disable emergency alert sound';
            btn.setAttribute('aria-label', 'Disable Emergency Alert Sound');
            btn.disabled = false;
        } else {
            btn.innerHTML = '🔔 Enable Emergency Alert Sound';
            btn.style.background = 'rgba(239, 68, 68, 0.2)';
            btn.style.borderColor = '#ef4444';
            btn.style.color = '#fca5a5';
            btn.title = 'Click to enable emergency alert sound';
            btn.setAttribute('aria-label', 'Enable Emergency Alert Sound');
            btn.disabled = false;
        }
    }
}

function toggleDoctorAudioAlerts() {
    if (isDoctorAlertSoundEnabled()) {
        disableDoctorAudioAlerts();
    } else {
        enableDoctorAudioAlerts();
    }
}

function enableDoctorAudioAlerts(force = false) {
    // If invoked via click on button when already enabled and not forced, toggle to disable
    if (!force && isDoctorAlertSoundEnabled()) {
        disableDoctorAudioAlerts();
        return;
    }

    try {
        if (!doctorAlertAudioContext) {
            doctorAlertAudioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        if (doctorAlertAudioContext.state === 'suspended') {
            doctorAlertAudioContext.resume().catch(() => {});
        }
        doctorAudioUnlocked = true;
        const user = getCurrentUser();
        const docId = user ? (user.profileId || user.id) : null;
        setDoctorAlertSoundEnabled(true, docId);
        updateDoctorAlertUI(true);
        showAlert('Emergency siren audio enabled for this session.', 'success');

        // Check if an unacknowledged emergency is active and start siren immediately
        if (activeDoctorAlert && !activeDoctorAlert.doctorAcknowledged && !acknowledgedEmergencyIds.has(activeDoctorAlert.id)) {
            startRepeatingSiren();
        } else {
            playAudioChirp();
            if (docId) {
                checkDoctorEmergencyAlerts(docId);
            }
        }
    } catch (e) {
        console.warn('Could not unlock audio context:', e);
        showAlert('Audio not supported or blocked by browser settings.', 'error');
    }
}

function disableDoctorAudioAlerts() {
    const user = getCurrentUser();
    const docId = user ? (user.profileId || user.id) : null;
    setDoctorAlertSoundEnabled(false, docId);
    stopRepeatingSiren();
    updateDoctorAlertUI(false);
    showAlert('Emergency alert sound disabled.', 'info');
}

function playAudioChirp() {
    if (!doctorAlertAudioContext) return;
    try {
        if (doctorAlertAudioContext.state === 'suspended') {
            doctorAlertAudioContext.resume().catch(() => {});
        }
        const now = doctorAlertAudioContext.currentTime;
        const osc = doctorAlertAudioContext.createOscillator();
        const gain = doctorAlertAudioContext.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(880, now);
        osc.frequency.exponentialRampToValueAtTime(1320, now + 0.15);
        gain.gain.setValueAtTime(0.15, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.2);
        osc.connect(gain);
        gain.connect(doctorAlertAudioContext.destination);
        osc.start(now);
        osc.stop(now + 0.2);
    } catch (e) {}
}

function initDoctorEmergencyMonitoring() {
    const user = getCurrentUser();
    if (!user || user.role !== 'DOCTOR') return;

    const doctorId = user.profileId;

    // Restore saved alert preference
    const soundEnabled = isDoctorAlertSoundEnabled(doctorId);
    updateDoctorAlertUI(soundEnabled);

    // Initialize audio context if preference was previously enabled
    if (soundEnabled) {
        try {
            if (!doctorAlertAudioContext) {
                doctorAlertAudioContext = new (window.AudioContext || window.webkitAudioContext)();
            }
            if (doctorAlertAudioContext.state === 'suspended') {
                doctorAlertAudioContext.resume().catch(() => {});
            }
        } catch (e) {}
    }

    // Attach user gesture resume listeners once
    if (!doctorAudioInteractionAttached) {
        doctorAudioInteractionAttached = true;
        const resumeOnInteraction = () => {
            if (doctorAlertAudioContext && doctorAlertAudioContext.state === 'suspended') {
                doctorAlertAudioContext.resume().catch(() => {});
            }
        };
        window.addEventListener('click', resumeOnInteraction, { passive: true });
        window.addEventListener('keydown', resumeOnInteraction, { passive: true });
    }

    // Tab visibility handling: Ensure alarms continue playing when returning to tab
    // Never stop or disable alerts because of tab switching
    document.addEventListener('visibilitychange', () => {
        if (!document.hidden && isDoctorAlertSoundEnabled(doctorId)) {
            if (doctorAlertAudioContext && doctorAlertAudioContext.state === 'suspended') {
                doctorAlertAudioContext.resume().catch(() => {});
            }
            if (activeDoctorAlert && !activeDoctorAlert.doctorAcknowledged && !acknowledgedEmergencyIds.has(activeDoctorAlert.id)) {
                if (!doctorSirenInterval) {
                    startRepeatingSiren();
                }
            }
        }
    });

    // 1. Initial check
    checkDoctorEmergencyAlerts(doctorId);

    // 2. WebSocket listener (subscribing to /topic/doctor/{doctorId})
    try {
        if (typeof SockJS !== 'undefined' && typeof Stomp !== 'undefined') {
            const socket = new SockJS('/ws');
            const stompClient = Stomp.over(socket);
            stompClient.debug = null;
            stompClient.connect({}, () => {
                stompClient.subscribe(`/topic/doctor/${doctorId}`, (message) => {
                    if (message.body) {
                        const alertData = JSON.parse(message.body);
                        triggerDoctorEmergencyAlert(alertData);
                    }
                });
            }, (err) => {
                console.warn('Doctor websocket fallback to polling:', err);
            });
        }
    } catch (e) {
        console.warn('Doctor websocket error:', e);
    }

    // 3. Fallback polling interval every 4s (clear previous to avoid duplicates)
    if (doctorEmergencyPollInterval) {
        clearInterval(doctorEmergencyPollInterval);
    }
    doctorEmergencyPollInterval = setInterval(() => {
        checkDoctorEmergencyAlerts(doctorId);
    }, 4000);

    // Setup modal acknowledge button
    const ackBtn = document.getElementById('modalAcknowledgeBtn');
    if (ackBtn && !ackBtn.dataset.listenerAttached) {
        ackBtn.addEventListener('click', handleDoctorAcknowledgeAlert);
        ackBtn.dataset.listenerAttached = 'true';
    }

    // Setup cross-tab synchronization
    setupDoctorCrossTabSync(doctorId);
}

function setupDoctorCrossTabSync(doctorId) {
    if (doctorCrossTabSyncAttached) return;
    doctorCrossTabSyncAttached = true;

    window.addEventListener('storage', (event) => {
        const prefKey = getDoctorAlertPrefKey(doctorId);
        if (event.key === prefKey) {
            const isEnabled = event.newValue === 'true';
            updateDoctorAlertUI(isEnabled);
            if (!isEnabled) {
                stopRepeatingSiren();
            } else {
                if (activeDoctorAlert && !activeDoctorAlert.doctorAcknowledged && !acknowledgedEmergencyIds.has(activeDoctorAlert.id)) {
                    startRepeatingSiren();
                }
            }
        } else if (event.key === `doctor_ack_emergency_${doctorId}`) {
            try {
                const payload = JSON.parse(event.newValue);
                if (payload && payload.id) {
                    acknowledgedEmergencyIds.add(payload.id);
                    if (activeDoctorAlert && activeDoctorAlert.id === payload.id) {
                        stopRepeatingSiren();
                        const modal = document.getElementById('doctorAlertModal');
                        if (modal) modal.style.display = 'none';
                        activeDoctorAlert = null;
                        checkDoctorEmergencyAlerts(doctorId);
                    }
                }
            } catch (e) {}
        }
    });
}

async function checkDoctorEmergencyAlerts(doctorId) {
    try {
        // Fetch unacknowledged alerts for modal
        const unacked = await apiRequest(`/emergencies/doctor/${doctorId}/unacknowledged`);
        if (unacked && unacked.length > 0) {
            const firstUnacked = unacked[0];
            if (!acknowledgedEmergencyIds.has(firstUnacked.id)) {
                if (!activeDoctorAlert || activeDoctorAlert.id !== firstUnacked.id) {
                    triggerDoctorEmergencyAlert(firstUnacked);
                } else if (isDoctorAlertSoundEnabled(doctorId) && !doctorSirenInterval && !firstUnacked.doctorAcknowledged) {
                    startRepeatingSiren();
                }
            }
        } else {
            if (activeDoctorAlert && activeDoctorAlert.doctorAcknowledged) {
                stopRepeatingSiren();
            }
        }

        // Fetch all assigned emergencies for dashboard list
        const allDoctorEmergencies = await apiRequest(`/emergencies/doctor/${doctorId}`);
        renderDoctorEmergencyList(allDoctorEmergencies);
    } catch (err) {
        // Ignore periodic polling errors
    }
}

function triggerDoctorEmergencyAlert(data) {
    if (!data || acknowledgedEmergencyIds.has(data.id)) return;

    // If already displaying this emergency and status hasn't changed, don't duplicate sound
    const isSameAlert = activeDoctorAlert && activeDoctorAlert.id === data.id;
    activeDoctorAlert = data;

    // Populate all submitted details into modal
    const setSafe = (id, val) => {
        const el = document.getElementById(id);
        if (el) el.textContent = val || '--';
    };

    setSafe('modalEmergencyCaseId', `Case #${data.id}`);
    setSafe('modalCategoryBadge', (data.category || 'EMERGENCY').replace('_', ' '));
    setSafe('modalStatusBadge', (data.status || 'DOCTOR_ASSIGNED').replace('_', ' '));
    setSafe('modalPatientCondition', data.description || 'Emergency symptoms reported by patient.');
    setSafe('modalPatientName', data.reportedByName || 'Emergency Reporter');
    setSafe('modalContactPhone', data.callbackPhone || 'N/A');
    setSafe('modalHospitalName', data.assignedHospitalName || 'Regional Trauma Center');
    setSafe('modalTimeReceived', data.createdAt ? new Date(data.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : 'Just now');
    setSafe('modalAmbulanceNumber', data.assignedAmbulanceNumber || 'Awaiting Dispatch');
    setSafe('modalEstimatedEta', data.estimatedArrivalMinutes ? `${data.estimatedArrivalMinutes} minutes` : 'Estimating...');
    setSafe('modalReportedAddress', data.manualAddress || 'GPS Location Supplied');
    setSafe('modalLandmark', data.landmark || data.area || 'None specified');
    setSafe('modalGpsCoords', (data.latitude && data.longitude) ? `${data.latitude.toFixed(5)}, ${data.longitude.toFixed(5)} (±${data.locationAccuracy ? Math.round(data.locationAccuracy) : 10}m)` : 'Manual Address Only');

    const trackLink = document.getElementById('modalTrackLink');
    if (trackLink) {
        trackLink.href = `/pages/emergency-tracking.html?id=${data.id}`;
    }

    const modal = document.getElementById('doctorAlertModal');
    if (modal) {
        modal.style.display = 'flex';
    }

    // Play and start repeating sound loop if enabled and not already looping for this case
    if (!isSameAlert) {
        if (isDoctorAlertSoundEnabled()) {
            startRepeatingSiren();
        }
    }
}

function startRepeatingSiren() {
    stopRepeatingSiren();
    if (!isDoctorAlertSoundEnabled()) return;
    playDoctorSirenSound();
    doctorSirenInterval = setInterval(() => {
        if (isDoctorAlertSoundEnabled() && activeDoctorAlert && !activeDoctorAlert.doctorAcknowledged && !acknowledgedEmergencyIds.has(activeDoctorAlert.id)) {
            playDoctorSirenSound();
        } else {
            stopRepeatingSiren();
        }
    }, 2400); // repeats every 2.4s until acknowledged
}

function stopRepeatingSiren() {
    if (doctorSirenInterval) {
        clearInterval(doctorSirenInterval);
        doctorSirenInterval = null;
    }
}

function isDoctorSirenActive() {
    return doctorSirenInterval !== null;
}

async function handleDoctorAcknowledgeAlert() {
    if (!activeDoctorAlert) return;

    const user = getCurrentUser();
    const doctorId = user.profileId;
    const emergencyId = activeDoctorAlert.id;
    const ackBtn = document.getElementById('modalAcknowledgeBtn');

    // Stop the repeating siren immediately for this emergency case
    stopRepeatingSiren();
    acknowledgedEmergencyIds.add(emergencyId);

    // Notify other open tabs of this emergency acknowledgment
    try {
        localStorage.setItem(`doctor_ack_emergency_${doctorId}`, JSON.stringify({ id: emergencyId, time: Date.now() }));
    } catch (e) {}

    try {
        ackBtn.disabled = true;
        ackBtn.textContent = 'Acknowledging...';

        await apiRequest(`/emergencies/${emergencyId}/doctor-ack?doctorId=${doctorId}`, 'POST');
        showAlert(`Emergency Case #${emergencyId} acknowledged. Trauma response notified.`, 'success');

        const modal = document.getElementById('doctorAlertModal');
        if (modal) modal.style.display = 'none';
        activeDoctorAlert = null;

        checkDoctorEmergencyAlerts(doctorId);
    } catch (err) {
        showAlert(err.message || 'Failed to acknowledge emergency.', 'error');
        const modal = document.getElementById('doctorAlertModal');
        if (modal) modal.style.display = 'none';
        activeDoctorAlert = null;
    } finally {
        ackBtn.disabled = false;
        ackBtn.textContent = '🩺 Acknowledge & Prep Trauma Room';
    }
}

function renderDoctorEmergencyList(items) {
    const card = document.getElementById('doctorEmergencyCard');
    const container = document.getElementById('doctorEmergencyList');
    const badge = document.getElementById('doctorEmergencyCountBadge');
    if (!card || !container) return;

    const activeCases = (items || []).filter(e => e.status !== 'COMPLETED' && e.status !== 'CANCELLED');

    if (activeCases.length === 0) {
        card.style.display = 'none';
        return;
    }

    card.style.display = 'block';
    badge.textContent = `${activeCases.length} Active`;

    let html = '';
    activeCases.forEach(item => {
        html += `
            <div style="background: #f8fafc; border: 1.5px solid #e2e8f0; border-radius: 12px; padding: 1.1rem; margin-bottom: 0.85rem; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.85rem;">
                <div style="flex: 1; min-width: 260px;">
                    <div style="display: flex; align-items: center; gap: 0.5rem; flex-wrap: wrap;">
                        <span class="status-pill ${item.status}">${item.status.replace('_', ' ')}</span>
                        <strong style="color: #0f172a; font-size: 1.05rem;">Case #${item.id}</strong>
                        <span style="font-size: 0.82rem; font-weight: 600; color: #dc2626;">[${(item.category || 'EMERGENCY').replace('_', ' ')}]</span>
                    </div>
                    <p style="margin: 0.4rem 0 0.3rem; color: #1e293b; font-size: 0.95rem; font-weight: 500;">
                        📝 <strong>Reported Symptoms:</strong> "${item.description}"
                    </p>
                    <div style="font-size: 0.82rem; color: #64748b; display: flex; flex-wrap: wrap; gap: 0.75rem; margin-top: 0.3rem;">
                        <span>👤 <strong>Patient:</strong> ${item.reportedByName || 'Caller'}</span>
                        <span>📞 <strong>Phone:</strong> ${item.callbackPhone || 'N/A'}</span>
                        <span>📍 <strong>Location:</strong> ${item.manualAddress || (item.latitude ? `${item.latitude.toFixed(4)}, ${item.longitude.toFixed(4)}` : 'GPS')}</span>
                        <span>🚑 <strong>Ambulance:</strong> ${item.assignedAmbulanceNumber ? `${item.assignedAmbulanceNumber} (Dispatched)` : 'Awaiting Dispatch'}</span>
                        <span>⏱️ <strong>ETA:</strong> <strong style="color: #0284c7;">${item.estimatedArrivalMinutes ? item.estimatedArrivalMinutes + ' mins' : '--'}</strong></span>
                        <span>🔔 <strong>Status:</strong> ${item.doctorAcknowledged ? '<span style="color:#059669; font-weight:700;">✅ Acknowledged</span>' : '<span style="color:#dc2626; font-weight:700;">⚠️ Pending Ack</span>'}</span>
                    </div>
                </div>
                <div style="display: flex; gap: 0.5rem; align-items: center;">
                    ${!item.doctorAcknowledged ? `
                        <button type="button" onclick="triggerDoctorEmergencyAlert(${JSON.stringify(item).replace(/"/g, '&quot;')})" class="btn btn-sm btn-primary" style="font-weight: 700;">
                            🩺 Review & Acknowledge
                        </button>
                    ` : ''}
                    <a href="/pages/emergency-tracking.html?id=${item.id}" target="_blank" class="btn btn-sm btn-outline">
                        📍 Track Live
                    </a>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

function playDoctorSirenSound() {
    if (!isDoctorAlertSoundEnabled()) return;
    try {
        if (!doctorAlertAudioContext) {
            doctorAlertAudioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        if (doctorAlertAudioContext.state === 'suspended') {
            doctorAlertAudioContext.resume().catch(() => {});
        }
        const now = doctorAlertAudioContext.currentTime;
        const osc = doctorAlertAudioContext.createOscillator();
        const gain = doctorAlertAudioContext.createGain();

        // Dual-tone emergency siren wave pattern
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(700, now);
        osc.frequency.linearRampToValueAtTime(1150, now + 0.25);
        osc.frequency.linearRampToValueAtTime(700, now + 0.5);
        osc.frequency.linearRampToValueAtTime(1150, now + 0.75);

        gain.gain.setValueAtTime(0.2, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.9);

        osc.connect(gain);
        gain.connect(doctorAlertAudioContext.destination);

        osc.start(now);
        osc.stop(now + 0.9);
    } catch (e) {
        console.warn('Audio alert play notice (interaction needed):', e);
    }
}

if (typeof window !== 'undefined') {
    window.isDoctorAlertSoundEnabled = isDoctorAlertSoundEnabled;
    window.enableDoctorAudioAlerts = enableDoctorAudioAlerts;
    window.disableDoctorAudioAlerts = disableDoctorAudioAlerts;
    window.toggleDoctorAudioAlerts = toggleDoctorAudioAlerts;
    window.startRepeatingSiren = startRepeatingSiren;
    window.stopRepeatingSiren = stopRepeatingSiren;
    window.isDoctorSirenActive = isDoctorSirenActive;
    window.initDoctorEmergencyMonitoring = initDoctorEmergencyMonitoring;
    window.checkDoctorEmergencyAlerts = checkDoctorEmergencyAlerts;
    window.triggerDoctorEmergencyAlert = triggerDoctorEmergencyAlert;
    window.handleDoctorAcknowledgeAlert = handleDoctorAcknowledgeAlert;
    window.getDoctorAlertPrefKey = getDoctorAlertPrefKey;
}
