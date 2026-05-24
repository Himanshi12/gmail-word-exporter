const form = document.getElementById("exportForm");
const startDateInput = document.getElementById("startDate");
const endDateInput = document.getElementById("endDate");
const exportButton = document.getElementById("exportButton");
const statusMessage = document.getElementById("statusMessage");

const today = new Date().toISOString().slice(0, 10);
startDateInput.value = today;
endDateInput.value = today;

form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const startDate = startDateInput.value;
    const endDate = endDateInput.value;

    if (!startDate || !endDate) {
        showStatus("Please select both dates.", "error");
        return;
    }

    if (endDate < startDate) {
        showStatus("End date must be greater than or equal to start date.", "error");
        return;
    }

    const exportUrl = `/api/emails/export?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`;

    setLoading(true);
    showStatus("Connecting to Gmail and preparing Excel file...");

    try {
        const response = await fetch(exportUrl);

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Export failed with status ${response.status}`);
        }

        const blob = await response.blob();
        const fileName = getFileName(response.headers.get("Content-Disposition"))
            || `gmail-emails-${startDate}-to-${endDate}.xlsx`;

        downloadBlob(blob, fileName);
        showStatus("Excel file downloaded successfully.", "success");
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
        ? '<span class="button-icon" aria-hidden="true"></span> Preparing Excel...'
        : '<span class="button-icon" aria-hidden="true"></span> Get Gmail Excel';
}

function showStatus(message, type = "") {
    statusMessage.textContent = message;
    statusMessage.className = `status ${type}`.trim();
}
