/**
 * Hospital Management System - Frontend Utilities
 */

document.addEventListener("DOMContentLoaded", function () {
    // 1. Confirm before destructive actions (Delete, Deactivate)
    const confirmButtons = document.querySelectorAll("[data-confirm]");
    confirmButtons.forEach(button => {
        button.addEventListener("click", function (event) {
            const message = this.getAttribute("data-confirm") || "Are you sure you want to proceed with this action?";
            if (!confirm(message)) {
                event.preventDefault();
            }
        });
    });

    // 2. Client-side quick filter for searchable tables
    const tableSearchInput = document.getElementById("tableQuickSearch");
    if (tableSearchInput) {
        tableSearchInput.addEventListener("keyup", function () {
            const filter = this.value.toLowerCase();
            const tableRows = document.querySelectorAll(".searchable-table tbody tr");
            tableRows.forEach(row => {
                const text = row.textContent.toLowerCase();
                row.style.display = text.includes(filter) ? "" : "none";
            });
        });
    }

    // 3. Client-side validation for appointment booking time (cannot be in the past)
    const appointmentTimeInput = document.getElementById("appointmentDatetimeInput");
    if (appointmentTimeInput && !appointmentTimeInput.value) {
        // Default to 1 hour ahead rounded to next 30-minute block
        const now = new Date();
        now.setHours(now.getHours() + 1);
        now.setMinutes(Math.ceil(now.getMinutes() / 30) * 30);
        now.setSeconds(0);
        
        // Format YYYY-MM-DDTHH:MM
        const pad = (n) => (n < 10 ? "0" + n : n);
        const isoString = now.getFullYear() + "-" +
            pad(now.getMonth() + 1) + "-" +
            pad(now.getDate()) + "T" +
            pad(now.getHours()) + ":" +
            pad(now.getMinutes());

        appointmentTimeInput.min = new Date().toISOString().slice(0, 16);
    }
});
