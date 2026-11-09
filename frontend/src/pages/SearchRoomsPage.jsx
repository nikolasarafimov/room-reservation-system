import { useState } from "react";
import { motion } from "framer-motion";
import toast from "react-hot-toast";
import { DoorOpen, Search, SlidersHorizontal } from "lucide-react";
import { searchRooms } from "../api.js";

export default function SearchRoomsPage() {
    const [form, setForm] = useState({
        reservationDate: "2026-07-20",
        startTime: "09:00",
        endTime: "10:00",
        minimumCapacity: 1,
        roomType: "",
        requiredEquipmentName: "",
    });

    const [rooms, setRooms] = useState([]);
    const [searched, setSearched] = useState(false);
    const [loading, setLoading] = useState(false);

    function updateField(field, value) {
        setForm((prev) => ({ ...prev, [field]: value }));
    }

    async function submit(e) {
        e.preventDefault();
        setLoading(true);
        setSearched(true);

        try {
            const result = await searchRooms({
                ...form,
                minimumCapacity: Number(form.minimumCapacity),
            });

            setRooms(result);
            toast.success(`${result.length} available rooms found`);
        } catch (err) {
            setRooms([]);
            toast.error(err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <section>
            <Hero
                eyebrow="Availability search"
                title="Search Available Rooms"
                subtitle="Find rooms that satisfy capacity, type, equipment and time availability constraints."
            />

            <div className="workspace-grid">
                <form className="panel page-form" onSubmit={submit}>
                    <SectionTitle
                        icon={SlidersHorizontal}
                        title="Search filters"
                        subtitle="Define the reservation interval and optional room requirements."
                    />

                    <div className="form-grid">
                        <Input
                            label="Reservation date"
                            type="date"
                            value={form.reservationDate}
                            onChange={(value) => updateField("reservationDate", value)}
                        />

                        <Input
                            label="Start time"
                            type="time"
                            value={form.startTime}
                            onChange={(value) => updateField("startTime", value)}
                        />

                        <Input
                            label="End time"
                            type="time"
                            value={form.endTime}
                            onChange={(value) => updateField("endTime", value)}
                        />

                        <Input
                            label="Minimum capacity"
                            type="number"
                            value={form.minimumCapacity}
                            onChange={(value) => updateField("minimumCapacity", value)}
                        />

                        <label>
                            Room type
                            <select value={form.roomType} onChange={(e) => updateField("roomType", e.target.value)}>
                                <option value="">Any type</option>
                                <option value="classroom">classroom</option>
                                <option value="office">office</option>
                                <option value="meeting_room">meeting_room</option>
                                <option value="lab">lab</option>
                            </select>
                        </label>

                        <Input
                            label="Required equipment"
                            value={form.requiredEquipmentName}
                            placeholder="Example: Projector"
                            onChange={(value) => updateField("requiredEquipmentName", value)}
                        />
                    </div>

                    <button className="primary full" disabled={loading}>
                        {loading ? "Searching..." : "Search rooms"}
                        <Search size={18} />
                    </button>
                </form>

                <div className="panel">
                    <SectionTitle
                        icon={DoorOpen}
                        title="Available rooms"
                        subtitle="Results are loaded from the live faculty PostgreSQL database."
                    />

                    {loading && <div className="empty-state">Searching database...</div>}

                    {!loading && searched && rooms.length === 0 && (
                        <div className="empty-state">No available rooms found for the selected criteria.</div>
                    )}

                    {!loading && rooms.length > 0 && (
                        <div className="room-grid">
                            {rooms.map((room) => (
                                <RoomCard key={room.roomId} room={room} />
                            ))}
                        </div>
                    )}

                    {!searched && !loading && (
                        <div className="empty-state">Use the filters and run a search to display available rooms.</div>
                    )}
                </div>
            </div>
        </section>
    );
}

function RoomCard({ room }) {
    return (
        <motion.div className="room-card" whileHover={{ y: -5 }}>
            <div className="room-card-top">
                <DoorOpen size={24} />
                <Badge value={room.type} />
            </div>

            <h3>{room.roomCode}</h3>
            <p>{room.buildingName}</p>

            <div className="room-meta">
                <span>Capacity</span>
                <strong>{room.capacity}</strong>
            </div>

            <small>{room.address}</small>
        </motion.div>
    );
}

function Hero({ eyebrow, title, subtitle }) {
    return (
        <div className="hero">
            <div>
                <div className="eyebrow">
                    <Search size={16} />
                    {eyebrow}
                </div>
                <h1>{title}</h1>
                <p>{subtitle}</p>
            </div>
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

function Input({ label, type = "text", value, placeholder = "", onChange }) {
    return (
        <label>
            {label}
            <input type={type} value={value} placeholder={placeholder} onChange={(e) => onChange(e.target.value)} />
        </label>
    );
}

function Badge({ value }) {
    return <span className={`badge ${String(value || "").toLowerCase()}`}>{value}</span>;
}