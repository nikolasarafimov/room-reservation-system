import { useState } from "react";
import toast from "react-hot-toast";
import { BarChart3, Boxes, DoorOpen } from "lucide-react";
import { getEquipmentDemandReport, getRoomUtilizationReport } from "../api.js";

export default function ReportsPage() {
    const [roomReport, setRoomReport] = useState([]);
    const [equipmentReport, setEquipmentReport] = useState([]);

    async function loadRoomReport() {
        try {
            const data = await getRoomUtilizationReport();
            setRoomReport(data);
            toast.success("Room utilization report loaded");
        } catch (err) {
            toast.error(err.message);
        }
    }

    async function loadEquipmentReport() {
        try {
            const data = await getEquipmentDemandReport();
            setEquipmentReport(data);
            toast.success("Equipment demand report loaded");
        } catch (err) {
            toast.error(err.message);
        }
    }

    return (
        <section>
            <Hero
                eyebrow="Analytics"
                title="Advanced SQL Reports"
                subtitle="Reports are generated from PostgreSQL queries, views, grouping and ranking logic."
            />

            <div className="report-actions">
                <ActionCard
                    icon={DoorOpen}
                    title="Room utilization"
                    description="Quarterly utilization by room, status and approved hours."
                    onClick={loadRoomReport}
                />

                <ActionCard
                    icon={Boxes}
                    title="Equipment demand"
                    description="Equipment demand, registered quantity and stock risk."
                    onClick={loadEquipmentReport}
                />
            </div>

            {roomReport.length > 0 && (
                <PremiumTable
                    title="Room utilization report"
                    columns={["Quarter", "Building", "Room", "Type", "Total", "Approved", "Hours", "Rank", "Level"]}
                    rows={roomReport.map((row) => [
                        row.quarterStart,
                        row.buildingName,
                        row.roomCode,
                        row.type,
                        row.totalRoomReservations,
                        row.approvedReservations,
                        row.approvedHours,
                        row.utilizationRank,
                        row.utilizationLevel,
                    ])}
                />
            )}

            {equipmentReport.length > 0 && (
                <PremiumTable
                    title="Equipment demand report"
                    columns={["Quarter", "Equipment", "Stock", "Assigned", "Registered", "Requested", "Demand %", "Rank", "Level"]}
                    rows={equipmentReport.map((row) => [
                        row.quarterStart,
                        row.equipmentName,
                        row.stockQuantity,
                        row.assignedRoomQuantity,
                        row.totalRegisteredQuantity,
                        row.totalRequestedQuantity,
                        row.demandToRegisteredPercent,
                        row.demandRank,
                        row.demandLevel,
                    ])}
                />
            )}
        </section>
    );
}

function Hero({ eyebrow, title, subtitle }) {
    return (
        <div className="hero">
            <div>
                <div className="eyebrow">
                    <BarChart3 size={16} />
                    {eyebrow}
                </div>
                <h1>{title}</h1>
                <p>{subtitle}</p>
            </div>
        </div>
    );
}

function ActionCard({ icon: Icon, title, description, onClick }) {
    return (
        <button className="action-card" onClick={onClick}>
            <Icon size={24} />
            <strong>{title}</strong>
            <span>{description}</span>
        </button>
    );
}

function PremiumTable({ title, columns, rows }) {
    return (
        <div className="panel table-panel">
            <h3>{title}</h3>

            <div className="table-wrapper">
                <table>
                    <thead>
                    <tr>
                        {columns.map((column) => (
                            <th key={column}>{column}</th>
                        ))}
                    </tr>
                    </thead>

                    <tbody>
                    {rows.map((row, index) => (
                        <tr key={index}>
                            {row.map((cell, cellIndex) => (
                                <td key={cellIndex}>{String(cell ?? "")}</td>
                            ))}
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    );
}