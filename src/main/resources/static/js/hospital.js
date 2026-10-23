/**
 * Hospital Emergency Dispatch Console JavaScript
 */

let currentHospital = null;
let stompClient = null;
let soundEnabled = true;
let audioContext = null;
let pollTimer = null;

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Role Guard
    if (!checkAuthGuard(['HOSPITAL', 'ADMIN'])) {
        return;
    }

    const user = getCurrentUser();

    // 2. Fetch Hospital Profile
    try {
        currentHospital = await apiRequest(`/hospitals/user/${user.id}`);
        document.getElementById('hospitalTitleDisplay').textContent = currentHospital.name;
        document.getElementById('hospitalMetaDisplay').textContent = 
            `📍 Area: ${currentHospital.area || 'Bangalore'} | Service Radius: ${currentHospital.serviceRadiusKm} km | Phone: ${currentHospital.phone || 'Emergency Desk'}`;
    } catch (err) {
        console.warn('Could not find specific hospital for user, falling back to first active hospital:', err);
        const hospitals = await apiRequest('/hospitals');
        if (hospitals && hospitals.length > 0) {
            currentHospital = hospitals[0];
            document.getElementById('hospitalTitleDisplay').textContent = `${currentHospital.name} (Admin Mode)`;
        } else {
            alert('No registered hospital profile found. Please contact clinic admin.');
            return;
        }
    }

    // 3. Setup Audio Siren Toggle
    setupAudioControls();

    // 4. Initial Load of Queue & Ambulances
    await refreshConsoleData();

    // 5. Connect WebSocket
    connectHospitalWebSocket();

    // 6. Polling Fallback (every 4 seconds)
    pollTimer = setInterval(refreshConsoleData, 4000);

    // 7. Event listeners
    document.getElementById('manualRefreshBtn').addEventListener('click', refreshConsoleData);
    document.getElementById('assignForm').addEventListener('submit', handleAssignSubmit);
});

// ==================== WEBSOCKET ====================
function connectHospitalWebSocket() {
    try {
        const socket = new SockJS('/ws-emergency');
        stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, () => {
            document.getElementById('socketStatusTag').textContent = '● Real-Time Stream Active';
            document.getElementById('socketStatusTag').style.color = '#4ade80';

            stompClient.subscribe('/topic/emergencies', (message) => {
                if (message.body) {
                    const data = JSON.parse(message.body);
                    handleIncomingBroadcast(data);
                }
            });
        }, (err) => {
            console.warn('Hospital websocket fallback to polling:', err);
            document.getElementById('socketStatusTag').textContent = '● Polling Mode (4s)';
            document.getElementById('socketStatusTag').style.color = '#f59e0b';
        });
    } catch (e) {
        console.warn('SockJS unavailable:', e);
    }
}

function handleIncomingBroadcast(data) {
    if (data.status === 'ALERTING_HOSPITALS') {
        playEmergencySiren();
        showAlert(`🚨 NEW EMERGENCY BROADCAST #${data.id}: ${data.category || 'URGENT'}`, 'error');
    }
    refreshConsoleData();
}

// ==================== AUDIO SIREN ====================
function setupAudioControls() {
    const btn = document.getElementById('audioToggleBtn');
    const label = document.getElementById('soundStatusLabel');

    btn.addEventListener('click', () => {
        soundEnabled = !soundEnabled;
        label.textContent = soundEnabled ? 'ON' : 'OFF';
        label.style.color = soundEnabled ? '#4ade80' : '#f87171';
        if (soundEnabled) {
            playBeep(880, 0.15); // preview confirmation beep
        }
    });
}

function playEmergencySiren() {
    if (!soundEnabled) return;
    try {
        if (!audioContext) {
            audioContext = new (window.AudioContext || window.webkitAudioContext)();
        }
        if (audioContext.state === 'suspended') {
            audioContext.resume();
        }

        const osc = audioContext.createOscillator();
        const gain = audioContext.createGain();
        osc.type = 'sawtooth';
        
        // Modulated siren pitch
        const now = audioContext.currentTime;
        osc.frequency.setValueAtTime(650, now);
        osc.frequency.linearRampToValueAtTime(950, now + 0.25);
        osc.frequency.linearRampToValueAtTime(650, now + 0.5);
        osc.frequency.linearRampToValueAtTime(950, now + 0.75);

        gain.gain.setValueAtTime(0.3, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.9);

        osc.connect(gain);
        gain.connect(audioContext.destination);

        osc.start(now);
        osc.stop(now + 0.9);
    } catch (e) {
        console.warn('Audio playback error:', e);
    }
}

function playBeep(freq = 600, duration = 0.2) {
    try {
        if (!audioContext) audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const osc = audioContext.createOscillator();
        const gain = audioContext.createGain();
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.2, audioContext.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + duration);
        osc.connect(gain);
        gain.connect(audioContext.destination);
        osc.start();
        osc.stop(audioContext.currentTime + duration);
    } catch (e) {}
}

// ==================== DATA REFRESH ====================
async function refreshConsoleData() {
    if (!currentHospital) return;

    try {
        // 1. Fetch all active emergencies seeking hospitals
        const activeList = await apiRequest('/emergencies/active');
        const pending = (activeList || []).filter(e => e.status === 'ALERTING_HOSPITALS');

        // 2. Fetch emergencies assigned to our hospital
        const ourCases = await apiRequest(`/emergencies/hospital/${currentHospital.id}`);

        // 3. Fetch ready ambulances
        const availableAmbulances = await apiRequest(`/hospitals/${currentHospital.id}/ambulances/available`);

        // Update Stat Counters
        document.getElementById('pendingCount').textContent = pending.length;
        document.getElementById('activeCasesCount').textContent = (ourCases || []).length;
        document.getElementById('availableAmbulancesCount').textContent = (availableAmbulances || []).length;

        // Toggle Urgent Alert Banner
        const banner = document.getElementById('urgentAlertBanner');
        if (pending.length > 0) {
            banner.style.display = 'block';
        } else {
            banner.style.display = 'none';
        }

        // Render Lists
        renderIncomingList(pending);
        renderActiveCasesList(ourCases);

    } catch (error) {
        console.error('Error refreshing hospital console data:', error);
    }
}

// ==================== RENDER INCOMING BROADCASTS ====================
function renderIncomingList(items) {
    const container = document.getElementById('incomingListContainer');
    if (!items || items.length === 0) {
        container.innerHTML = '<p style="color: #64748b; text-align: center; padding: 2rem 0;">No pending incoming broadcasts right now. Standby mode active.</p>';
        return;
    }

    let html = '';
    items.forEach(item => {
        const timeAgo = formatTimeAgo(item.createdAt);
        html += `
            <div class="emergency-inbox-item critical">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 0.75rem;">
                    <div>
                        <div style="display: flex; align-items: center; gap: 0.5rem;">
                            <span class="status-pill ALERTING_HOSPITALS">🚨 ${item.category || 'EMERGENCY'}</span>
                            <span style="font-weight: 700; color: #0f172a;">#${item.id}</span>
                            <span style="font-size: 0.8rem; color: #94a3b8;">${timeAgo}</span>
                        </div>
                        <h4 style="color: #1e293b; margin: 0.4rem 0 0.2rem; font-size: 1.05rem;">
                            ${item.description || 'Critical assistance requested.'}
                        </h4>
                        <div style="font-size: 0.85rem; color: #64748b; margin-top: 0.25rem;">
                            📍 <strong>Location:</strong> ${item.manualAddress || 'GPS Coordinates Provided'} (${item.area || 'Bangalore'})
                            <span style="margin-left: 0.75rem;">📞 <strong>Patient Phone:</strong> ${item.callbackPhone || '--'}</span>
                        </div>
                    </div>

                    <!-- First-Wins Accept / Reject Action Buttons -->
                    <div style="display: flex; gap: 0.6rem; align-items: center;">
                        <button type="button" onclick="rejectEmergencyRequest(${item.id})" class="btn btn-sm btn-outline" style="color: #dc2626; border-color: #fca5a5;">
                            ✖ Decline
                        </button>
                        <button type="button" onclick="acceptEmergencyRequest(${item.id})" class="btn btn-sm" style="background: #10b981; color: white; font-weight: 800; box-shadow: 0 2px 8px rgba(16, 185, 129, 0.4);">
                            ✅ ACCEPT EMERGENCY
                        </button>
                    </div>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

// ==================== RENDER ACTIVE CASES ====================
function renderActiveCasesList(items) {
    const container = document.getElementById('activeCasesListContainer');
    if (!items || items.length === 0) {
        container.innerHTML = '<p style="color: #64748b; text-align: center; padding: 2rem 0;">No active dispatches currently managed by your unit.</p>';
        return;
    }

    let html = '';
    items.forEach(item => {
        const status = item.status;
        const needsAssignment = !item.assignedDoctorId || !item.assignedAmbulanceId;

        html += `
            <div class="emergency-inbox-item ${status === 'ACCEPTED' ? 'accepted' : 'dispatched'}">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 0.75rem;">
                    <div>
                        <div style="display: flex; align-items: center; gap: 0.5rem;">
                            <span class="status-pill ${status}">${status.replace('_', ' ')}</span>
                            <span style="font-weight: 700; color: #0f172a;">Case #${item.id}</span>
                            <span style="font-size: 0.82rem; color: #64748b;">Patient: <strong>${item.reportedByName || 'Emergency Caller'}</strong> (${item.callbackPhone || '--'})</span>
                        </div>
                        <p style="color: #334155; margin: 0.4rem 0 0.2rem; font-size: 0.95rem;">
                            ${item.description}
                        </p>
                        <div style="font-size: 0.82rem; color: #64748b; display: flex; gap: 1rem; flex-wrap: wrap; margin-top: 0.4rem;">
                            <span>👨‍⚕️ Doctor: <strong style="color: #0f172a;">${item.assignedDoctorName || 'Not Assigned'}</strong></span>
                            <span>🚑 Ambulance: <strong style="color: #0f172a;">${item.assignedAmbulanceNumber || 'Not Allocated'}</strong></span>
                            <span>⏱️ ETA: <strong style="color: #0284c7;">${item.estimatedArrivalMinutes ? item.estimatedArrivalMinutes + ' mins' : '--'}</strong></span>
                        </div>
                    </div>

                    <!-- Workflow Actions -->
                    <div style="display: flex; flex-direction: column; gap: 0.5rem; align-items: flex-end;">
                        ${needsAssignment ? `
                            <button type="button" onclick="openAssignModal(${item.id})" class="btn btn-sm btn-primary">
                                ➕ Assign Doctor & Ambulance
                            </button>
                        ` : ''}

                        <div style="display: flex; gap: 0.4rem; flex-wrap: wrap;">
                            ${status === 'DOCTOR_ASSIGNED' ? `
                                <button type="button" onclick="advanceStatus(${item.id}, 'AMBULANCE_DISPATCHED')" class="btn btn-sm" style="background: #0284c7; color: white;">
                                    🚑 Dispatch Unit
                                </button>
                            ` : ''}

                            ${status === 'AMBULANCE_DISPATCHED' ? `
                                <button type="button" onclick="advanceStatus(${item.id}, 'EN_ROUTE')" class="btn btn-sm" style="background: #0284c7; color: white;">
                                    ⚡ Mark En Route
                                </button>
                                <button type="button" onclick="simulateGpsPing(${item.id}, ${item.latitude || 12.97}, ${item.longitude || 77.59})" class="btn btn-sm btn-outline" title="Simulate GPS ping towards patient">
                                    🛰️ Sim GPS
                                </button>
                            ` : ''}

                            ${status === 'EN_ROUTE' ? `
                                <button type="button" onclick="advanceStatus(${item.id}, 'ARRIVED')" class="btn btn-sm" style="background: #10b981; color: white;">
                                    🏁 Mark Arrived at Scene
                                </button>
                                <button type="button" onclick="simulateGpsPing(${item.id}, ${item.latitude || 12.97}, ${item.longitude || 77.59})" class="btn btn-sm btn-outline">
                                    🛰️ Sim GPS
                                </button>
                            ` : ''}

                            ${status === 'ARRIVED' ? `
                                <button type="button" onclick="advanceStatus(${item.id}, 'COMPLETED')" class="btn btn-sm" style="background: #059669; color: white;">
                                    ✅ Patient Admitted / Complete
                                </button>
                            ` : ''}

                            <a href="/pages/emergency-tracking.html?id=${item.id}" target="_blank" class="btn btn-sm btn-outline" style="font-size: 0.78rem;">
                                👁️ Patient View
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

// ==================== HOSPITAL ACCEPTANCE (First-Wins) ====================
async function acceptEmergencyRequest(id) {
    if (!currentHospital) return;

    const user = getCurrentUser();
    const payload = {
        hospitalId: currentHospital.id,
        userId: user.id
    };

    try {
        const result = await apiRequest(`/emergencies/${id}/accept`, 'POST', payload);
        showAlert(`Emergency #${id} successfully accepted! Now assign doctor and ambulance.`, 'success');
        await refreshConsoleData();
        // Automatically open assignment modal
        openAssignModal(id);
    } catch (error) {
        // Handle 409 Conflict: Another hospital accepted first
        alert(`⚠️ ACCEPTANCE CONFLICT: ${error.message || 'This emergency has already been accepted by another hospital.'}`);
        await refreshConsoleData();
    }
}

async function rejectEmergencyRequest(id) {
    if (!currentHospital) return;
    const reason = prompt('Please enter reason for declining (e.g. Trauma beds full / No available ambulance):', 'Ambulances in transit');
    if (reason === null) return;

    const user = getCurrentUser();
    try {
        await apiRequest(`/emergencies/${id}/reject?hospitalId=${currentHospital.id}&userId=${user.id}&reason=${encodeURIComponent(reason)}`, 'POST');
        showAlert(`Emergency #${id} declined.`, 'info');
        await refreshConsoleData();
    } catch (err) {
        showAlert(err.message || 'Failed to decline emergency.', 'error');
    }
}

// ==================== ASSIGNMENT MODAL ====================
async function openAssignModal(id) {
    document.getElementById('modalEmergencyId').value = id;

    // Load available doctors
    try {
        const doctors = await apiRequest('/hospitals/doctors/available');
        const docSelect = document.getElementById('assignDoctorSelect');
        docSelect.innerHTML = '<option value="">Select Available Doctor...</option>';
        (doctors || []).forEach(doc => {
            docSelect.innerHTML += `<option value="${doc.id}">Dr. ${doc.user ? doc.user.name : 'Doctor'} (${doc.specialization ? doc.specialization.name : 'Specialist'})</option>`;
        });
    } catch (e) {
        console.warn('Could not load doctors:', e);
    }

    // Load available ambulances
    try {
        const ambulances = await apiRequest(`/hospitals/${currentHospital.id}/ambulances/available`);
        const ambSelect = document.getElementById('assignAmbulanceSelect');
        ambSelect.innerHTML = '<option value="">Select Ready Ambulance...</option>';
        (ambulances || []).forEach(amb => {
            ambSelect.innerHTML += `<option value="${amb.id}">${amb.vehicleNumber} (Driver: ${amb.driverName || 'Unit'})</option>`;
        });
    } catch (e) {
        console.warn('Could not load ambulances:', e);
    }

    const modal = document.getElementById('assignModal');
    modal.style.display = 'flex';
    modal.classList.add('active');
}

function closeAssignModal() {
    const modal = document.getElementById('assignModal');
    modal.style.display = 'none';
    modal.classList.remove('active');
}

async function handleAssignSubmit(e) {
    e.preventDefault();

    const emergencyId = document.getElementById('modalEmergencyId').value;
    const doctorId = document.getElementById('assignDoctorSelect').value;
    const ambulanceId = document.getElementById('assignAmbulanceSelect').value;
    const etaMinutes = parseInt(document.getElementById('estimatedMinutesInput').value) || 15;
    const user = getCurrentUser();

    const payload = {
        hospitalId: currentHospital.id,
        doctorId: doctorId ? parseInt(doctorId) : null,
        ambulanceId: ambulanceId ? parseInt(ambulanceId) : null,
        estimatedArrivalMinutes: etaMinutes,
        userId: user.id
    };

    const submitBtn = document.getElementById('confirmAssignBtn');
    try {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Allocating Resources...';

        await apiRequest(`/emergencies/${emergencyId}/assign`, 'POST', payload);
        showAlert(`Doctor and Ambulance allocated for Emergency #${emergencyId}!`, 'success');
        closeAssignModal();
        await refreshConsoleData();
    } catch (err) {
        showAlert(err.message || 'Failed to assign resources.', 'error');
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Confirm & Dispatch';
    }
}

// ==================== STATUS ADVANCEMENT ====================
async function advanceStatus(id, newStatus) {
    const user = getCurrentUser();
    try {
        await apiRequest(`/emergencies/${id}/status?status=${newStatus}&userId=${user.id}`, 'PUT');
        showAlert(`Case #${id} status advanced to ${newStatus.replace('_', ' ')}!`, 'success');
        await refreshConsoleData();
    } catch (err) {
        showAlert(err.message || 'Status update failed.', 'error');
    }
}

// ==================== SIMULATED GPS PING ====================
async function simulateGpsPing(id, destLat, destLng) {
    // Generate a slightly offset location closer to destination
    const deltaLat = (Math.random() - 0.5) * 0.005;
    const deltaLng = (Math.random() - 0.5) * 0.005;

    const payload = {
        latitude: destLat + deltaLat,
        longitude: destLng + deltaLng,
        speedKmph: 45.0 + Math.round(Math.random() * 15),
        simulated: true
    };

    try {
        await apiRequest(`/emergencies/${id}/ambulance-location`, 'POST', payload);
        showAlert(`Ambulance GPS ping sent for Case #${id} (Speed: ${payload.speedKmph} km/h)`, 'info');
    } catch (err) {
        showAlert('GPS update failed: ' + err.message, 'error');
    }
}

// ==================== HELPERS ====================
function scrollToPending() {
    document.getElementById('pendingSection').scrollIntoView({ behavior: 'smooth' });
}

function formatTimeAgo(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    const seconds = Math.floor((new Date() - date) / 1000);
    if (seconds < 60) return `${seconds}s ago`;
    const mins = Math.floor(seconds / 60);
    if (mins < 60) return `${mins}m ago`;
    return date.toLocaleTimeString();
}
