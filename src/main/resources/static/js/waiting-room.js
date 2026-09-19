/**
 * Live Waiting Room Queue Poller
 */

let activePollingInterval = null;
let selectedDoctorId = null;

// Populate Doctors dropdown for Waiting Room
async function initWaitingRoom() {
    const doctorSelect = document.getElementById('waitingDoctorSelect');
    if (!doctorSelect) return;

    try {
        const doctors = await apiRequest('/doctors');
        doctorSelect.innerHTML = '<option value="">-- Select a Doctor / Cabin --</option>';
        doctors.forEach(doc => {
            doctorSelect.innerHTML += `<option value="${doc.id}">${doc.name} (${doc.specializationName})</option>`;
        });

        // Automatically select first doctor if available
        if (doctors.length > 0) {
            doctorSelect.value = doctors[0].id;
            changeWaitingDoctor();
        }
    } catch (error) {
        console.error('Failed to load doctors for waiting room:', error);
    }
}

// Change Doctor and restart polling ticker
function changeWaitingDoctor() {
    const select = document.getElementById('waitingDoctorSelect');
    selectedDoctorId = select.value;

    if (activePollingInterval) {
        clearInterval(activePollingInterval);
    }

    if (selectedDoctorId) {
        fetchQueueData();
        // Periodically poll queue every 5 seconds (Fetch API periodic refresh as requested)
        activePollingInterval = setInterval(fetchQueueData, 5000);
    } else {
        resetWaitingDisplay();
    }
}

async function fetchQueueData() {
    if (!selectedDoctorId) return;

    const today = new Date().toISOString().split('T')[0];

    try {
        const data = await apiRequest(`/queue/doctor/${selectedDoctorId}?date=${today}`);

        document.getElementById('displayDoctorName').innerText = data.doctorName;
        document.getElementById('displaySpecialization').innerText = data.specialization;

        // Current Token
        const currentTokenEl = document.getElementById('displayCurrentToken');
        if (data.currentToken) {
            currentTokenEl.innerText = `Token #${data.currentToken}`;
            currentTokenEl.style.color = '#10b981';
        } else {
            currentTokenEl.innerText = 'Ready / None';
            currentTokenEl.style.color = '#94a3b8';
        }

        // Next Token
        const nextTokenEl = document.getElementById('displayNextToken');
        if (data.nextToken) {
            nextTokenEl.innerText = `Token #${data.nextToken}`;
        } else {
            nextTokenEl.innerText = 'None';
        }

        // Waiting Count
        document.getElementById('displayWaitingCount').innerText = `${data.waitingCount} Patients`;

        // Update Pulse Timestamp
        const now = new Date();
        document.getElementById('lastUpdatedTime').innerText = `Live Updated at ${now.toLocaleTimeString()}`;

    } catch (error) {
        console.error('Queue poll error:', error);
        document.getElementById('lastUpdatedTime').innerText = `Reconnecting... (${error.message})`;
    }
}

function resetWaitingDisplay() {
    document.getElementById('displayDoctorName').innerText = 'Please Select a Doctor';
    document.getElementById('displaySpecialization').innerText = '';
    document.getElementById('displayCurrentToken').innerText = '--';
    document.getElementById('displayNextToken').innerText = '--';
    document.getElementById('displayWaitingCount').innerText = '--';
}
