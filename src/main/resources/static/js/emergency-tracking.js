/**
 * Emergency Live Tracking Script with WebSocket and Polling Fallback
 */

let emergencyId = null;
let stompClient = null;
let pollTimer = null;
let etaCountdownTimer = null;
let currentEtaMinutes = 15;

document.addEventListener('DOMContentLoaded', async () => {
    const urlParams = new URLSearchParams(window.location.search);
    emergencyId = urlParams.get('id');

    if (!emergencyId) {
        alert('No Emergency ID specified. Returning to emergency portal.');
        window.location.href = '/pages/emergency.html';
        return;
    }

    document.getElementById('emergencyIdDisplay').textContent = `Emergency #${emergencyId}`;

    // 1. Initial Data Fetch
    await fetchEmergencyData();

    // 2. Connect WebSocket
    connectWebSocket();

    // 3. Fallback Polling every 4 seconds
    pollTimer = setInterval(fetchEmergencyData, 4000);

    // 4. Cancel Emergency button
    document.getElementById('cancelEmergencyBtn').addEventListener('click', handleCancelEmergency);

    // 5. Refresh timeline button
    document.getElementById('refreshTimelineBtn').addEventListener('click', fetchEmergencyData);
});

// ==================== WEBSOCKET CONNECTION ====================
function connectWebSocket() {
    try {
        const socket = new SockJS('/ws-emergency');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // suppress verbose stomp logs

        stompClient.connect({}, () => {
            const indicator = document.getElementById('socketLiveIndicator');
            if (indicator) indicator.textContent = 'Live Real-Time Stream Active';

            // Subscribe to this emergency's topic
            stompClient.subscribe(`/topic/emergency/${emergencyId}`, (message) => {
                if (message.body) {
                    const data = JSON.parse(message.body);
                    renderEmergencyState(data);
                }
            });
        }, (error) => {
            console.warn('WebSocket connection failed, relying on active polling:', error);
            const indicator = document.getElementById('socketLiveIndicator');
            if (indicator) indicator.textContent = 'Auto-Polling Active (3s)';
        });
    } catch (e) {
        console.warn('SockJS not available, fallback to polling:', e);
    }
}

// ==================== DATA FETCHING ====================
async function fetchEmergencyData() {
    try {
        const data = await apiRequest(`/emergencies/${emergencyId}`);
        renderEmergencyState(data);
    } catch (error) {
        console.error('Failed to fetch emergency status:', error);
    }
}

// ==================== RENDER EMERGENCY STATE ====================
function renderEmergencyState(data) {
    if (!data) return;

    const status = data.status;
    const headerPill = document.getElementById('headerStatusPill');
    const headline = document.getElementById('emergencyHeadline');
    const etaText = document.getElementById('etaTimeDisplay');
    const speedText = document.getElementById('liveSpeedText');
    const distanceText = document.getElementById('liveDistanceText');
    const gpsStateText = document.getElementById('liveGpsState');

    // 1. Update Header Pill & Headline
    headerPill.className = `status-pill ${status}`;
    headerPill.textContent = status ? status.replace('_', ' ') : 'SUBMITTED';

    // GPS & Staleness evaluation
    let hasGps = data.ambulanceLatitude && data.ambulanceLongitude;
    let gpsUpdateTime = data.ambulanceLocationUpdatedAt ? new Date(data.ambulanceLocationUpdatedAt) : null;
    let isStale = false;
    let lastPingStr = '';

    if (gpsUpdateTime) {
        const ageSeconds = Math.round((Date.now() - gpsUpdateTime.getTime()) / 1000);
        lastPingStr = gpsUpdateTime.toLocaleTimeString();
        if (ageSeconds > 60) {
            isStale = true;
        }
    }

    switch (status) {
        case 'CREATED':
        case 'ALERTING_HOSPITALS':
            headline.textContent = '📡 Alerting Nearby Emergency Hospitals...';
            updateStepper(1, '15%');
            setRadarAmbulancePosition(10);
            speedText.textContent = 'N/A';
            distanceText.textContent = 'Awaiting Dispatch';
            gpsStateText.textContent = 'Awaiting Hospital Acceptance';
            gpsStateText.style.color = '#94a3b8';
            etaText.textContent = '-- mins';
            break;
        case 'ACCEPTED':
            headline.textContent = `🏥 Emergency Accepted by ${data.assignedHospitalName || 'Hospital'}`;
            updateStepper(2, '35%');
            setRadarAmbulancePosition(25);
            speedText.textContent = 'N/A';
            distanceText.textContent = 'Coordinating Vehicle';
            gpsStateText.textContent = 'Hospital Accepted (Dispatching)';
            gpsStateText.style.color = '#38bdf8';
            etaText.textContent = data.estimatedArrivalMinutes ? `${data.estimatedArrivalMinutes} mins` : 'Estimating...';
            break;
        case 'DOCTOR_ASSIGNED':
            headline.textContent = `👨‍⚕️ Emergency Physician Assigned: Dr. ${data.assignedDoctorName || 'Assigned'}`;
            updateStepper(3, '55%');
            setRadarAmbulancePosition(40);
            speedText.textContent = 'N/A';
            distanceText.textContent = 'Preparing Unit';
            gpsStateText.textContent = 'Physician Assigned';
            gpsStateText.style.color = '#a855f7';
            etaText.textContent = data.estimatedArrivalMinutes ? `${data.estimatedArrivalMinutes} mins` : 'Estimating...';
            break;
        case 'AMBULANCE_DISPATCHED':
            headline.textContent = `🚑 Ambulance ${data.assignedAmbulanceNumber || ''} Dispatched & Mobilizing`;
            updateStepper(4, '75%');
            setRadarAmbulancePosition(60);
            if (hasGps) {
                if (isStale) {
                    gpsStateText.textContent = `⚠️ Stale GPS (Last ping ${lastPingStr})`;
                    gpsStateText.style.color = '#f59e0b';
                } else {
                    gpsStateText.textContent = `Active GPS Signal (${lastPingStr})`;
                    gpsStateText.style.color = '#4ade80';
                }
                speedText.textContent = 'In Transit';
            } else {
                gpsStateText.textContent = 'Awaiting First GPS Ping';
                gpsStateText.style.color = '#94a3b8';
                speedText.textContent = 'Dispatched';
            }
            break;
        case 'EN_ROUTE':
            headline.textContent = `⚡ Ambulance Approaching Your Location`;
            updateStepper(5, '90%');
            setRadarAmbulancePosition(85);
            if (hasGps) {
                if (isStale) {
                    gpsStateText.textContent = `⚠️ Stale GPS (Last ping ${lastPingStr})`;
                    gpsStateText.style.color = '#f59e0b';
                } else {
                    gpsStateText.textContent = `Live GPS (${lastPingStr})`;
                    gpsStateText.style.color = '#4ade80';
                }
                speedText.textContent = 'En Route';
            } else {
                gpsStateText.textContent = 'GPS Signal Pending';
                gpsStateText.style.color = '#94a3b8';
                speedText.textContent = 'In Transit';
            }
            break;
        case 'ARRIVED':
            headline.textContent = `🏁 Ambulance Has Arrived at Your Location`;
            updateStepper(6, '100%');
            setRadarAmbulancePosition(96);
            etaText.textContent = 'Arrived';
            speedText.textContent = '0 km/h';
            distanceText.textContent = 'At Scene';
            gpsStateText.textContent = 'Ambulance On Scene';
            gpsStateText.style.color = '#10b981';
            break;
        case 'COMPLETED':
            headline.textContent = `✅ Emergency Case Completed & Patient Admitted`;
            updateStepper(6, '100%');
            etaText.textContent = 'Completed';
            speedText.textContent = '0 km/h';
            distanceText.textContent = 'At Hospital';
            gpsStateText.textContent = 'Mission Complete';
            gpsStateText.style.color = '#10b981';
            break;
        case 'CANCELLED':
            headline.textContent = `⚠️ Emergency Request Cancelled`;
            headerPill.className = 'status-pill CANCELLED';
            headerPill.textContent = 'CANCELLED';
            speedText.textContent = 'N/A';
            distanceText.textContent = 'Cancelled';
            gpsStateText.textContent = 'Request Cancelled';
            gpsStateText.style.color = '#ef4444';
            etaText.textContent = '--';
            break;
    }

    // 2. ETA & Distance Display (Only when reliable estimate provided)
    if (status !== 'ARRIVED' && status !== 'COMPLETED' && status !== 'CANCELLED') {
        if (data.estimatedArrivalMinutes != null && data.estimatedArrivalMinutes > 0) {
            currentEtaMinutes = data.estimatedArrivalMinutes;
            etaText.textContent = `${currentEtaMinutes} mins`;
            distanceText.textContent = `~${(currentEtaMinutes * 0.7).toFixed(1)} km`;
        } else if (status === 'AMBULANCE_DISPATCHED' || status === 'EN_ROUTE') {
            etaText.textContent = 'Calculating...';
            distanceText.textContent = 'En Route';
        }
    }

    // 3. Hospital Card
    if (data.assignedHospitalName) {
        document.getElementById('hospitalNameText').textContent = data.assignedHospitalName;
        document.getElementById('hospitalDetailsSection').style.display = 'block';
        if (data.assignedHospitalPhone) {
            document.getElementById('hospitalPhoneText').textContent = data.assignedHospitalPhone;
            document.getElementById('hospitalCallBtn').href = `tel:${data.assignedHospitalPhone}`;
        }
        document.getElementById('hospitalAddressText').textContent = data.manualAddress || 'Metro Emergency Center';
    }

    // 4. Doctor Card
    if (data.assignedDoctorName) {
        document.getElementById('doctorNameText').textContent = `Dr. ${data.assignedDoctorName}`;
        document.getElementById('doctorDetailsSection').style.display = 'block';
        if (data.doctorAcknowledged) {
            document.getElementById('doctorAckStatus').textContent = 'Acknowledged & Trauma Unit Ready';
            document.getElementById('doctorAckStatus').style.color = '#10b981';
        } else {
            document.getElementById('doctorAckStatus').textContent = 'Alert Dispatched (Pending Ack)';
            document.getElementById('doctorAckStatus').style.color = '#f59e0b';
        }
    }

    // 5. Ambulance Card
    if (data.assignedAmbulanceNumber) {
        document.getElementById('ambulanceNumberText').textContent = data.assignedAmbulanceNumber;
        document.getElementById('ambulanceDetailsSection').style.display = 'block';
        document.getElementById('vehicleNumberDisplay').textContent = data.assignedAmbulanceNumber;
        document.getElementById('radarVehicleText').textContent = data.assignedAmbulanceNumber;
    }

    // 6. Timeline History Audit Trail
    renderTimeline(data.statusHistory);
}

// ==================== STEPPER HELPER ====================
function updateStepper(activeStep, progressWidth) {
    document.getElementById('stepperProgressBar').style.width = progressWidth;

    for (let i = 1; i <= 6; i++) {
        const node = document.getElementById(`stepNode${i}`);
        if (i < activeStep) {
            node.className = 'step-node completed';
        } else if (i === activeStep) {
            node.className = 'step-node active';
        } else {
            node.className = 'step-node';
        }
    }
}

// Position animated ambulance marker on radar canvas (percentage from left)
function setRadarAmbulancePosition(percent) {
    const marker = document.getElementById('radarAmbulanceMarker');
    if (marker) {
        marker.style.left = `${percent}%`;
    }
}

// ==================== TIMELINE RENDERING ====================
function renderTimeline(historyList) {
    const container = document.getElementById('timelineContainer');
    if (!historyList || historyList.length === 0) {
        container.innerHTML = '<p style="color: #64748b; font-size: 0.88rem;">No timeline updates logged yet.</p>';
        return;
    }

    let html = '<div style="display: flex; flex-direction: column; gap: 0.85rem; padding-left: 0.5rem;">';
    historyList.forEach(item => {
        const timeStr = item.timestamp ? new Date(item.timestamp).toLocaleTimeString() : '';
        html += `
            <div style="display: flex; align-items: flex-start; gap: 0.75rem; border-left: 2px solid #e2e8f0; padding-left: 1rem; position: relative;">
                <div style="width: 10px; height: 10px; border-radius: 50%; background: #0284c7; position: absolute; left: -6px; top: 4px;"></div>
                <div>
                    <div style="font-size: 0.85rem; font-weight: 700; color: #1e293b;">
                        ${item.toStatus ? item.toStatus.replace('_', ' ') : 'Status Updated'}
                        <span style="font-weight: 400; color: #94a3b8; font-size: 0.75rem; margin-left: 0.5rem;">${timeStr}</span>
                    </div>
                    <div style="font-size: 0.82rem; color: #64748b;">${item.notes || 'Status changed'}</div>
                </div>
            </div>
        `;
    });
    html += '</div>';
    container.innerHTML = html;
}

// ==================== CANCELLATION ====================
async function handleCancelEmergency() {
    const confirmCancel = confirm('Are you sure you want to cancel this emergency request?');
    if (!confirmCancel) return;

    const user = getCurrentUser();
    const userId = user ? user.id : 1;

    try {
        await apiRequest(`/emergencies/${emergencyId}/status?status=CANCELLED&userId=${userId}`, 'PUT');
        showAlert('Emergency request has been cancelled.', 'info');
        fetchEmergencyData();
    } catch (err) {
        showAlert(err.message || 'Failed to cancel emergency.', 'error');
    }
}
