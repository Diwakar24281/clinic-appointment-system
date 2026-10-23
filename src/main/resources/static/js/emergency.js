/**
 * Emergency Reporting, Voice Assistant, and Geolocation Manager
 */

let recognition = null;
let isRecording = false;

document.addEventListener('DOMContentLoaded', async () => {
    // Check user role: If doctor, show advisory banner
    const user = getCurrentUser();
    if (user && user.role === 'DOCTOR') {
        const docNotice = document.getElementById('doctorRoleNotice');
        if (docNotice) docNotice.style.display = 'block';
    }

    // 1. Initialize user info if logged in
    if (user) {
        if (user.phone) {
            document.getElementById('callbackPhone').value = user.phone;
        } else if (user.role === 'PATIENT' && user.profileId) {
            try {
                const patient = await apiRequest(`/patients/${user.profileId}`);
                if (patient && patient.phone) {
                    document.getElementById('callbackPhone').value = patient.phone;
                }
                if (patient && patient.address) {
                    document.getElementById('manualAddress').value = patient.address;
                }
            } catch (err) {
                console.warn('Could not prefill patient details:', err);
            }
        }
    }

    // Check if there is an active emergency already for this user
    if (user && user.role === 'PATIENT') {
        try {
            const activeEmergency = await apiRequest(`/emergencies/active/user/${user.id}`);
            if (activeEmergency && activeEmergency.id) {
                const proceed = confirm(`You have an active emergency request (#${activeEmergency.id} - ${activeEmergency.status}). Would you like to view its live tracking?`);
                if (proceed) {
                    window.location.href = `/pages/emergency-tracking.html?id=${activeEmergency.id}`;
                    return;
                }
            }
        } catch (e) {
            // No active emergency or error, ignore
        }
    }

    // 2. Initialize Geolocation
    initGeolocation();

    // 3. Initialize Speech Recognition
    initSpeechRecognition();

    // 4. Category chips click handler
    setupCategoryChips();

    // 5. One-Tap SOS Button
    document.getElementById('oneTapSosBtn').addEventListener('click', handleOneTapSos);

    // 6. Form Submission
    document.getElementById('emergencyForm').addEventListener('submit', handleEmergencySubmit);

    // 7. Refresh GPS button
    document.getElementById('refreshGpsBtn').addEventListener('click', initGeolocation);
});

// ==================== GEOLOCATION ====================
function initGeolocation() {
    const coordsText = document.getElementById('gpsCoordsText');
    const statusTitle = document.getElementById('gpsStatusTitle');
    const gpsIcon = document.getElementById('gpsIcon');

    if (!navigator.geolocation) {
        statusTitle.textContent = 'GPS Not Supported by Browser';
        coordsText.textContent = 'Please enter street address manually below.';
        setFallbackLocation();
        return;
    }

    statusTitle.textContent = 'Acquiring GPS Fix...';
    coordsText.textContent = 'Requesting device satellite coordinates...';

    navigator.geolocation.getCurrentPosition(
        (position) => {
            const lat = position.coords.latitude;
            const lng = position.coords.longitude;
            const accuracy = position.coords.accuracy || 10;

            document.getElementById('latitude').value = lat.toFixed(6);
            document.getElementById('longitude').value = lng.toFixed(6);
            document.getElementById('locationAccuracy').value = accuracy.toFixed(1);

            statusTitle.textContent = 'GPS Location Locked (Accuracy ~' + Math.round(accuracy) + 'm)';
            coordsText.textContent = `Lat: ${lat.toFixed(5)}°, Lng: ${lng.toFixed(5)}° (Accuracy: ${Math.round(accuracy)}m)`;
            gpsIcon.classList.add('active');
            gpsIcon.textContent = '📍';
        },
        (error) => {
            console.warn('Geolocation failed:', error);
            statusTitle.textContent = 'GPS Access Denied / Unavailable';
            coordsText.textContent = 'Please enter street address and landmark in the fields below.';
            setFallbackLocation();
        },
        { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    );
}

function setFallbackLocation() {
    // Default Bangalore coordinates if GPS denied, user can still manually specify address
    document.getElementById('latitude').value = '12.971600';
    document.getElementById('longitude').value = '77.594600';
    document.getElementById('locationAccuracy').value = '50';
}

// ==================== SPEECH RECOGNITION (Web Speech API) ====================
function initSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const micBtn = document.getElementById('micBtn');
    const startVoiceBtn = document.getElementById('startVoiceBtn');
    const stopVoiceBtn = document.getElementById('stopVoiceBtn');
    const transcriptEl = document.getElementById('voiceTranscript');
    const badge = document.getElementById('voiceStatusBadge');
    const guideBtn = document.getElementById('playSpokenGuideBtn');

    if (!SpeechRecognition) {
        transcriptEl.innerHTML = '⚠️ <em>Speech recognition is not supported in this browser. Please type your symptoms directly in the box below.</em>';
        if (micBtn) { micBtn.style.opacity = '0.5'; micBtn.disabled = true; }
        if (startVoiceBtn) startVoiceBtn.disabled = true;
        if (stopVoiceBtn) stopVoiceBtn.disabled = true;
        if (badge) badge.textContent = 'Voice Unavailable';
        return;
    }

    recognition = new SpeechRecognition();
    recognition.continuous = true;
    recognition.interimResults = true;

    // Play spoken voice instructions
    if (guideBtn) {
        guideBtn.addEventListener('click', playSpokenGuidance);
    }

    recognition.onstart = () => {
        isRecording = true;
        if (micBtn) micBtn.classList.add('listening');
        if (startVoiceBtn) startVoiceBtn.disabled = true;
        if (stopVoiceBtn) stopVoiceBtn.disabled = false;
        const lang = document.getElementById('voiceLangSelect').value;
        const langName = lang === 'ta-IN' ? 'Tamil' : 'English (India)';
        transcriptEl.innerHTML = `<em>Listening in ${langName}... Speak symptoms clearly now...</em>`;
        badge.innerHTML = '<span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:#ef4444; animation: blink 1s infinite;"></span> Listening';
    };

    recognition.onresult = (event) => {
        let interimTranscript = '';
        let finalChunk = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
            if (event.results[i].isFinal) {
                finalChunk += event.results[i][0].transcript;
            } else {
                interimTranscript += event.results[i][0].transcript;
            }
        }

        const displayText = finalChunk || interimTranscript;
        if (displayText) {
            transcriptEl.textContent = `"${displayText}"`;
        }

        if (finalChunk && finalChunk.trim().length > 0) {
            const descInput = document.getElementById('emergencyDescription');
            const trimmedFinal = finalChunk.trim();
            const currentVal = descInput.value.trim();

            // Non-destructive: Preserve existing text and avoid duplicate repetition
            if (!currentVal.toLowerCase().includes(trimmedFinal.toLowerCase())) {
                descInput.value = currentVal ? currentVal + ' ' + trimmedFinal : trimmedFinal;
            }

            detectCategoryFromSpeech(trimmedFinal);
        }
    };

    recognition.onerror = (event) => {
        console.warn('Speech recognition status:', event.error);
        stopRecording();
        if (event.error === 'not-allowed') {
            transcriptEl.innerHTML = '⚠️ <strong>Microphone access denied.</strong> Please enable microphone permission in your browser settings or type your symptoms below.';
            badge.innerHTML = '<span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:#f59e0b;"></span> Mic Denied';
        } else if (event.error === 'no-speech') {
            transcriptEl.innerHTML = '<em>No speech detected. Click Start Speaking again or type your symptoms.</em>';
        } else {
            transcriptEl.innerHTML = `<em>Microphone stopped (${event.error}). You may type symptoms below.</em>`;
        }
    };

    recognition.onend = () => {
        stopRecording();
    };

    // Button controls
    if (startVoiceBtn) {
        startVoiceBtn.addEventListener('click', startRecording);
    }
    if (stopVoiceBtn) {
        stopVoiceBtn.addEventListener('click', stopRecording);
    }
    if (micBtn) {
        micBtn.addEventListener('click', () => {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });
    }
}

function startRecording() {
    if (!recognition) return;
    try {
        const langSelect = document.getElementById('voiceLangSelect');
        recognition.lang = langSelect ? langSelect.value : 'en-IN';
        recognition.start();
    } catch (err) {
        console.warn('Error starting speech:', err);
    }
}

function stopRecording() {
    isRecording = false;
    if (recognition) {
        try { recognition.stop(); } catch (e) {}
    }
    const micBtn = document.getElementById('micBtn');
    const startVoiceBtn = document.getElementById('startVoiceBtn');
    const stopVoiceBtn = document.getElementById('stopVoiceBtn');
    const badge = document.getElementById('voiceStatusBadge');

    if (micBtn) micBtn.classList.remove('listening');
    if (startVoiceBtn) startVoiceBtn.disabled = false;
    if (stopVoiceBtn) stopVoiceBtn.disabled = true;
    if (badge) {
        badge.innerHTML = '<span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:#10b981;"></span> Voice Ready';
    }
}

function playSpokenGuidance() {
    if ('speechSynthesis' in window) {
        const lang = document.getElementById('voiceLangSelect') ? document.getElementById('voiceLangSelect').value : 'en-IN';
        const utterance = new SpeechSynthesisUtterance();
        utterance.lang = lang;
        if (lang === 'ta-IN') {
            utterance.text = 'தயவுசெய்து உங்கள் அவசர அறிகுறிகளை தெளிவாக கூறவும்.';
        } else {
            utterance.text = 'Please describe your emergency symptoms clearly after starting the microphone.';
        }
        window.speechSynthesis.speak(utterance);
    } else {
        const helpText = document.getElementById('micHelpText');
        if (helpText) {
            helpText.innerHTML = '<strong>Guide:</strong> Speak your emergency symptoms clearly into the microphone. You can also type directly in the box below.';
        }
    }
}

function detectCategoryFromSpeech(text) {
    const lower = text.toLowerCase();
    let detected = 'OTHER';

    // English and Tamil keyword matching for symptom triage
    if (lower.includes('chest') || lower.includes('heart') || lower.includes('cardiac') || lower.includes('pressure')
            || lower.includes('நெஞ்சு') || lower.includes('மாரடைப்பு') || lower.includes('இதயம்')) {
        detected = 'CHEST_PAIN';
    } else if (lower.includes('accident') || lower.includes('bleed') || lower.includes('fall') || lower.includes('injury') || lower.includes('fracture') || lower.includes('trauma')
            || lower.includes('விபத்து') || lower.includes('ரத்தம்') || lower.includes('காயம்') || lower.includes('முறிவு')) {
        detected = 'TRAUMA_ACCIDENT';
    } else if (lower.includes('breath') || lower.includes('chok') || lower.includes('suffocat') || lower.includes('asthma') || lower.includes('lungs')
            || lower.includes('மூச்சு') || lower.includes('சுவாசம்')) {
        detected = 'BREATHING_DIFFICULTY';
    } else if (lower.includes('stroke') || lower.includes('unconscious') || lower.includes('faint') || lower.includes('seizure') || lower.includes('collapsed')
            || lower.includes('மயக்கம்') || lower.includes('பக்கவாதம்') || lower.includes('வலிப்பு')) {
        detected = 'STROKE_UNCONSCIOUS';
    } else if (lower.includes('labor') || lower.includes('pregnan') || lower.includes('delivery') || lower.includes('water broke')
            || lower.includes('பிரசவம்') || lower.includes('கர்ப்பம்')) {
        detected = 'PREGNANCY_LABOR';
    }

    selectCategoryChip(detected);
}

// ==================== CATEGORY CHIPS ====================
function setupCategoryChips() {
    const chips = document.querySelectorAll('.category-chip');
    chips.forEach(chip => {
        chip.addEventListener('click', () => {
            chips.forEach(c => c.classList.remove('selected'));
            chip.classList.add('selected');
            document.getElementById('emergencyCategory').value = chip.dataset.category;
        });
    });
}

function selectCategoryChip(category) {
    const chips = document.querySelectorAll('.category-chip');
    chips.forEach(c => {
        if (c.dataset.category === category) {
            c.classList.add('selected');
            document.getElementById('emergencyCategory').value = category;
        } else {
            c.classList.remove('selected');
        }
    });
}

// ==================== ONE-TAP SOS ====================
async function handleOneTapSos() {
    const user = getCurrentUser();
    if (!user) {
        alert('Please login or provide contact details to dispatch emergency response.');
        window.location.href = '/pages/login.html';
        return;
    }

    const confirmSos = confirm('🚨 TRIGGER INSTANT SOS DISPATCH?\n\nThis will immediately alert all nearby emergency hospitals and request the nearest ambulance to your current GPS position.');
    if (!confirmSos) return;

    const lat = parseFloat(document.getElementById('latitude').value) || 12.9716;
    const lng = parseFloat(document.getElementById('longitude').value) || 77.5946;
    const accuracy = parseFloat(document.getElementById('locationAccuracy').value) || 10.0;
    const phone = document.getElementById('callbackPhone').value.trim() || '9876543210';

    const payload = {
        userId: user.id,
        category: 'OTHER',
        description: '🚨 INSTANT ONE-TAP SOS TRIGGERED BY PATIENT. Urgent emergency response requested.',
        callbackPhone: phone,
        latitude: lat,
        longitude: lng,
        locationAccuracy: accuracy,
        manualAddress: document.getElementById('manualAddress').value.trim() || 'One-Tap SOS Location',
        area: document.getElementById('emergencyArea').value.trim() || 'Bangalore Central'
    };

    try {
        const sosBtn = document.getElementById('oneTapSosBtn');
        sosBtn.disabled = true;
        sosBtn.style.opacity = '0.7';

        const result = await apiRequest('/emergencies', 'POST', payload);
        showAlert('Emergency Alert Broadcasted to Hospitals!', 'success');
        setTimeout(() => {
            window.location.href = `/pages/emergency-tracking.html?id=${result.id}`;
        }, 500);
    } catch (err) {
        alert('Failed to broadcast SOS alert: ' + (err.message || 'Server error'));
        document.getElementById('oneTapSosBtn').disabled = false;
        document.getElementById('oneTapSosBtn').style.opacity = '1';
    }
}

// ==================== FORM SUBMISSION ====================
async function handleEmergencySubmit(e) {
    e.preventDefault();

    const user = getCurrentUser();
    if (!user) {
        alert('Please login to report an emergency, so hospital coordinators can view your profile and contact you.');
        window.location.href = '/pages/login.html';
        return;
    }

    const description = document.getElementById('emergencyDescription').value.trim();
    const callbackPhone = document.getElementById('callbackPhone').value.trim();
    const category = document.getElementById('emergencyCategory').value || 'OTHER';
    const area = document.getElementById('emergencyArea').value.trim();
    const manualAddress = document.getElementById('manualAddress').value.trim();
    const landmark = document.getElementById('landmark').value.trim();

    const latStr = document.getElementById('latitude').value;
    const lngStr = document.getElementById('longitude').value;
    const accuracyStr = document.getElementById('locationAccuracy').value;

    const lat = latStr ? parseFloat(latStr) : 12.9716;
    const lng = lngStr ? parseFloat(lngStr) : 77.5946;
    const accuracy = accuracyStr ? parseFloat(accuracyStr) : 10.0;

    if (!description) {
        showAlert('Please provide emergency symptoms description.', 'error');
        return;
    }

    if (!callbackPhone) {
        showAlert('Callback contact phone is required.', 'error');
        return;
    }

    const payload = {
        userId: user.id,
        category: category,
        description: description,
        callbackPhone: callbackPhone,
        latitude: lat,
        longitude: lng,
        locationAccuracy: accuracy,
        manualAddress: manualAddress || 'GPS Coordinates Provided',
        area: area || 'Metropolitan Area',
        landmark: landmark
    };

    const submitBtn = document.getElementById('submitEmergencyBtn');
    try {
        submitBtn.disabled = true;
        submitBtn.textContent = '⏳ Dispatching Emergency Alert...';

        const result = await apiRequest('/emergencies', 'POST', payload);
        showAlert('Emergency Broadcasted Successfully! Connecting to live tracking...', 'success');

        setTimeout(() => {
            window.location.href = `/pages/emergency-tracking.html?id=${result.id}`;
        }, 800);
    } catch (error) {
        showAlert(error.message || 'Failed to dispatch emergency request.', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = '🚨 Dispatch Emergency Ambulance & Alert Hospitals Now';
    }
}
