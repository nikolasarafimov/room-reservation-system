import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { Boxes, CalendarCheck, ShieldCheck, Users } from "lucide-react";
import { getDashboardStats, getEquipment, getPendingReservations, getUsers } from "../api.js";

export default function AdminPanelPage({ activeUser }) {
    const [users, setUsers] = useState([]);
    const [equipment, setEquipment] = useState([]);
    const [pending, setPending] = useState([]);
    const [stats, setStats] = useState(null);

    useEffect(() => {
        if (activeUser.role !== "admin") {
            return;
        }

        Promise.all([
            getUsers(),
            getEquipment(),
            getPendingReservations(),
            getDashboardStats(),
        ])
            .then(([usersData, equipmentData, pendingData, statsData]) => {
                setUsers(usersData);
                setEquipment(equipmentData);
                setPending(pendingData);
                setStats(statsData);
            })
            .catch((err) => toast.error(err.message));
    }, [activeUser.role]);

    if (activeUser.role !== "admin") {
        return (
            <section>
                <Hero
                    eyebrow="Access denied"
                    title="Admin Panel"
                    subtitle="This module is available only for administrator users."
                />
            </section>
        );
    }

    return (
        <section>
            <Hero
                eyebrow="Administration"
                title="Admin Panel"
                subtitle="Read-only operational overview of users, equipment and pending reservation activity."
            />

            {stats && (
                <div className="stats-grid compact">
                    <SmallStat icon={Users} label="Users" value={stats.totalUsers} />
                    <SmallStat icon={Boxes} label="Equipment" value={stats.totalEquipmentTypes} />
                    <SmallStat icon={CalendarCheck} label="Pending" value={stats.pendingReservations} />
                </div>
            )}

            <div className="admin-grid">
                <div className="panel">
                    <SectionTitle icon={Users} title="System users" subtitle="Loaded from project.users." />
                    <MiniList
                        items={users.map((u) => ({
                            title: u.fullName,
                            subtitle: `${u.username} · ${u.email}`,
                            badge: u.role,
                        }))}
                    />
                </div>

                <div className="panel">
                    <SectionTitle icon={Boxes} title="Equipment inventory" subtitle="Registered equipment types." />
                    <MiniList
                        items={equipment.map((e) => ({
                            title: e.name,
                            subtitle: `Stock quantity: ${e.stockQuantity}`,
                            badge: "equipment",
                        }))}
                    />
                </div>

                <div className="panel">
                    <SectionTitle icon={CalendarCheck} title="Pending reservations" subtitle="Requests waiting for approval." />
                    <MiniList
                        items={pending.map((p) => ({
                            title: `#${p.reservationId} · ${p.requesterName}`,
                            subtitle: `${p.reservationDate} · ${p.startTime}-${p.endTime}`,
                            badge: p.status,
                        }))}
                    />
                </div>
            </div>
        </section>
    );
}

function Hero({ eyebrow, title, subtitle }) {
    return (
        <div className="hero">
            <div>
                <div className="eyebrow">
                    <ShieldCheck size={16} />
                    {eyebrow}
                </div>
                <h1>{title}</h1>
                <p>{subtitle}</p>
            </div>
        </div>
    );
}

function SmallStat({ icon: Icon, label, value }) {
    return (
        <div className="stat-card blue">
            <div className="stat-icon">
                <Icon size={24} />
            </div>
            <span>{label}</span>
            <strong>{value}</strong>
        </div>
    );
}

function SectionTitle({ icon: Icon, title, subtitle }) {
    return (
        <div className="section-title">
            <Icon size={21} />
            <div>
                <h3>{title}</h3>
                <p>{subtitle}</p>
            </div>
        </div>
    );
}

function MiniList({ items }) {
    if (!items.length) {
        return <div className="empty-state small">No records available.</div>;
    }

    return (
        <div className="mini-list">
            {items.map((item, index) => (
                <div className="mini-list-item" key={index}>
                    <div>
                        <strong>{item.title}</strong>
                        <span>{item.subtitle}</span>
                    </div>
                    <Badge value={item.badge} />
                </div>
            ))}
        </div>
    );
}

function Badge({ value }) {
    return <span className={`badge ${String(value || "").toLowerCase()}`}>{value}</span>;
}