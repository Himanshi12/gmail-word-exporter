const form = document.getElementById("exportForm");
const startDateTimeInput = document.getElementById("startDateTime");
const endDateTimeInput = document.getElementById("endDateTime");
const exportButton = document.getElementById("exportButton");
const statusMessage = document.getElementById("statusMessage");

const now = new Date();
const todayStart = new Date(now);
todayStart.setHours(0, 0, 0, 0);
startDateTimeInput.value = toDateTimeLocalValue(todayStart);
endDateTimeInput.value = toDateTimeLocalValue(now);

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const startDateTime = startDateTimeInput.value;
    const endDateTime = endDateTimeInput.value;

    if (!startDateTime || !endDateTime) {
        showStatus("Please select both date and time values.", "error");
        return;
    }

    if (endDateTime < startDateTime) {
        showStatus("End date and time must be greater than or equal to start date and time.", "error");
        return;
    }

    const exportUrl = `/api/emails/export?startDateTime=${encodeURIComponent(startDateTime)}&endDateTime=${encodeURIComponent(endDateTime)}`;

    setLoading(true);
    showStatus("Connecting to Gmail and preparing Word file...");

    try {
        const response = await fetch(exportUrl);

        if (response.status === 401) {
            showStatus("Gmail authorization is required. Opening Google login...");
            const authResponse = await fetch("/api/gmail/auth-url");

            if (!authResponse.ok) {
                throw new Error("Unable to create Gmail authorization URL.");
            }

            const authData = await authResponse.json();
            window.location.href = authData.authorizationUrl;
            return;
        }

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Export failed with status ${response.status}`);
        }

        const blob = await response.blob();
        const fileName = getFileName(response.headers.get("Content-Disposition"))
            || `gmail-emails-${safeFileDateTime(startDateTime)}-to-${safeFileDateTime(endDateTime)}.docx`;

        downloadBlob(blob, fileName);
        showStatus("Word file downloaded successfully.", "success");
    } catch (error) {
        showStatus(error.message || "Unable to export Gmail messages.", "error");
    } finally {
        setLoading(false);
    }
});

function downloadBlob(blob, fileName) {
    const link = document.createElement("a");
    const objectUrl = URL.createObjectURL(blob);

    link.href = objectUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();

    URL.revokeObjectURL(objectUrl);
}

function getFileName(contentDisposition) {
    if (!contentDisposition) {
        return "";
    }

    const match = contentDisposition.match(/filename="?([^"]+)"?/);
    return match ? match[1] : "";
}

function setLoading(isLoading) {
    exportButton.disabled = isLoading;
    exportButton.innerHTML = isLoading
        ? '<span class="button-icon" aria-hidden="true"></span> Preparing Word...'
        : '<span class="button-icon" aria-hidden="true"></span> Get Gmail Word';
}

function showStatus(message, type = "") {
    statusMessage.textContent = message;
    statusMessage.className = `status ${type}`.trim();
}

function toDateTimeLocalValue(date) {
    const year = date.getFullYear();
    const month = pad(date.getMonth() + 1);
    const day = pad(date.getDate());
    const hours = pad(date.getHours());
    const minutes = pad(date.getMinutes());

    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

function safeFileDateTime(value) {
    return value.replaceAll("-", "").replace("T", "-").replace(":", "");
}

function pad(value) {
    return String(value).padStart(2, "0");
}
