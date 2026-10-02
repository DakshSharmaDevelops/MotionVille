import { useEffect, useState } from "react";
import {
    fetchReports,
    updateReportStatus,
} from "../api/reportApi.js";

const STATUSES = ["OPEN", "REVIEWING", "RESOLVED", "REJECTED"];

export default function ReportsAdminPanel({ userId, onClose }) {
    const [reports, setReports] = useState([]);
    const [draftStatuses, setDraftStatuses] = useState({});
    const [loading, setLoading] = useState(true);
    const [busyReportId, setBusyReportId] = useState(null);
    const [error, setError] = useState("");

    useEffect(() => {
        let active = true;
        setLoading(true);
        setError("");

        fetchReports(userId)
            .then((items) => {
                if (active) setReports(items);
            })
            .catch((requestError) => {
                if (active) setError(requestError.message);
            })
            .finally(() => {
                if (active) setLoading(false);
            });

        return () => {
            active = false;
        };
    }, [userId]);

    useEffect(() => {
        function closeOnEscape(event) {
            if (event.key === "Escape") onClose();
        }

        window.addEventListener("keydown", closeOnEscape);
        return () => window.removeEventListener("keydown", closeOnEscape);
    }, [onClose]);

    async function saveStatus(reportId, currentStatus) {
        const status = draftStatuses[reportId] || currentStatus;
        setBusyReportId(reportId);
        setError("");

        try {
            const updated = await updateReportStatus(userId, reportId, status);
            setReports((items) =>
                items.map((item) => item.id === reportId ? updated : item),
            );
            setDraftStatuses((items) => {
                const next = { ...items };
                delete next[reportId];
                return next;
            });
        } catch (requestError) {
            setError(requestError.message);
        } finally {
            setBusyReportId(null);
        }
    }

    return (
        <div
            className="modal-backdrop reports-admin-backdrop"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget) onClose();
            }}
        >
            <section
                className="dialog reports-admin-dialog"
                aria-labelledby="reports-title"
                aria-modal="true"
                role="dialog"
            >
                <div className="dialog-topline">
                    <span className="dialog-step">Moderation</span>
                    <button className="text-button" type="button" onClick={onClose}>
                        Close
                    </button>
                </div>
                <h2 id="reports-title">Report queue</h2>
                <p className="dialog-subtitle">Review reports and update their status.</p>

                {error && <p className="inline-error" role="alert">{error}</p>}
                {loading && <p className="report-queue-message">Loading reports…</p>}
                {!loading && !error && reports.length === 0 && (
                    <p className="report-queue-message">No reports found.</p>
                )}

                <div className="report-list">
                    {reports.map((report) => (
                        <article className="report-item" key={report.id}>
                            <div className="report-item-heading">
                                <strong>Report #{report.id}</strong>
                                <span>{report.reason}</span>
                            </div>
                            <p className="report-item-target">
                                {report.videoId != null
                                    ? `Video ${report.videoId}`
                                    : `Comment ${report.commentId}`}
                            </p>
                            <p className="report-item-details">
                                {report.details || "No additional details."}
                            </p>
                            <p className="report-item-meta">
                                Reporter {report.reporterId} ·{" "}
                                {new Date(report.createdAt).toLocaleString()}
                            </p>

                            <div className="report-item-actions">
                                <label>
                                    Status
                                    <select
                                        value={draftStatuses[report.id] || report.status}
                                        onChange={(event) =>
                                            setDraftStatuses((items) => ({
                                                ...items,
                                                [report.id]: event.target.value,
                                            }))
                                        }
                                    >
                                        {STATUSES.map((status) => (
                                            <option key={status} value={status}>{status}</option>
                                        ))}
                                    </select>
                                </label>
                                <button
                                    className="button button-primary"
                                    type="button"
                                    disabled={
                                        busyReportId === report.id ||
                                        (draftStatuses[report.id] || report.status) === report.status
                                    }
                                    onClick={() => saveStatus(report.id, report.status)}
                                >
                                    {busyReportId === report.id ? "Saving…" : "Save status"}
                                </button>
                            </div>
                        </article>
                    ))}
                </div>
            </section>
        </div>
    );
}