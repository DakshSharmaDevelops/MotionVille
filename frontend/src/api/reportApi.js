import { apiRequest } from "./videoApi.js";

export function fetchReports(userId) {
    return apiRequest(`/reports?userId=${encodeURIComponent(userId)}`, {
        cache: "no-store",
    });
}

export function fetchReport(userId, reportId) {
    return apiRequest(
        `/reports/${encodeURIComponent(reportId)}?userId=${encodeURIComponent(userId)}`,
        { cache: "no-store" },
    );
}

export function createReport(report) {
    return apiRequest("/reports", {
        method: "POST",
        body: JSON.stringify(report),
    });
}

export function updateReportStatus(userId, reportId, status) {
    return apiRequest(
        `/reports/${encodeURIComponent(reportId)}/status?userId=${encodeURIComponent(userId)}`,
        {
            method: "PUT",
            body: JSON.stringify({ status }),
        },
    );
}