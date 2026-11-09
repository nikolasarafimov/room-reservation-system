import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import toast from "react-hot-toast";
import { CheckCircle2, ClipboardCheck, XCircle } from "lucide-react";
import { approveReservation, getPendingReservations } from "../api.js";

export default function ApprovalsPage({ activeUser }) {
    const [pending, setPending] = useState([]);
    const [loading, setLoading] = useState(true);

    function loadData() {
        setLoading(true);

        getPendingReservations()
            .then(setPending)
            .catch((err) => toast.error(err.message))
            .finally(() => setLoading(false));
    }

    useEffect(() => {
        loadData();
    }, []);

    async function saveDecision(reservationId, decision) {
        try {
            await approveReservation({
                reservationId,
                approverId: activeUser.userId,
                decision,
                note: `${decision} through React UI.`,
            });

            toast.success(`Reservation ${decision}`);
            loadData();
        } catch (err) {
            toast.error(err.message);
        }
    }

    return (
        <section>
            <Hero
                eyebrow="Approval center"
                title="Review Pending Reservations"
                subtitle="Approve or reject pending requests through the PostgreSQL stored function."
            />

            {loading && <div className="empty-state">Loading pending reservations...</div>}

            {!loading && pending.length === 0 && (
                <div className="empty-state">No pending reservations at the moment.</div>
            )}

            {!loading && pending.length > 0 && (
                <div className="approval-grid">
                    {pending.map((item) => (
                        <motion.div className="approval-card" key={item.reservationId} whileHover={{ y: -4 }}>
                            <div className="approval-card-top">
                                <Badge value={item.status} />
                                <span>#{item.reservationId}</span>
                            </div>

                            <h3>{item.requesterName}</h3>
                            <p>{item.reservationDate} · {item.startTime} - {item.endTime}</p>

                            <div className="approval-meta">
                                <span>Room</span>
                                <strong>{item.roomCode || "No room"}</strong>
                            </div>

                            <div className="approval-meta">
                                <span>Building</span>
                                <strong>{item.buildingName || "No building"}</strong>
                            </div>

                            <div className="approval-meta column">
                                <span>Equipment</span>
                                <strong>{item.requestedEquipment || "No equipment"}</strong>
                            </div>

                            <div className="approval-actions">
                                <button className="success-button" onClick={() => saveDecision(item.reservationId, "approved")}>
                                    <CheckCircle2 size={18} />
                                    Approve
                                </button>

                                <button className="danger-button" onClick={() => saveDecision(item.reservationId, "rejected")}>
                                    <XCircle size={18} />
                                    Reject
                                </button>
                            </div>
                        </motion.div>
                    ))}
                </div>
            )}
        </section>
    );
}

function Hero({ eyebrow, title, subtitle }) {
    return (
        <div className="hero">
            <div>
                <div className="eyebrow">
                    <ClipboardCheck size={16} />
                    {eyebrow}
                </div>
                <h1>{title}</h1>
                <p>{subtitle}</p>
            </div>
        </div>
    );
}

function Badge({ value }) {
    return <span className={`badge ${String(value || "").toLowerCase()}`}>{value}</span>;
}