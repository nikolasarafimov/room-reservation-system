import { useEffect, useState } from "react";
import toast from "react-hot-toast";
import { ArrowRight, PlusCircle, RefreshCcw, Search, UserRound, Wrench } from "lucide-react";
import { createReservation, getEquipment, searchRooms } from "../api.js";

export default function CreateReservationPage({ activeUser }) {
    const [equipment, setEquipment] = useState([]);
    const [rooms, setRooms] = useState([]);

    const [form, setForm] = useState({
        reservationDate: "2026-07-20",
        startTime: "09:00",
        endTime: "10:00",
        minimumCapacity: 1,
        roomType: "",
        requiredEquipmentName: "",
    });

    const [selectedRoomId, setSelectedRoomId] = useState("");
    const [equipmentId, setEquipmentId] = useState("");
    const [quantity, setQuantity] = useState(1);
    const [selectedEquipment, setSelectedEquipment] = useState([]);
    const [created, setCreated] = useState(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        getEquipment()
            .then(setEquipment)
            .catch((err) => toast.error(err.message));
    }, []);

    function updateField(field, value) {
        setForm((prev) => ({ ...prev, [field]: value }));
    }

    async function loadAvailableRooms(e) {
        e.preventDefault();
        setCreated(null);

        try {
            const result = await searchRooms({
                ...form,
                minimumCapacity: Number(form.minimumCapacity),
            });

            setRooms(result);
            setSelectedRoomId("");
            toast.success(`${result.length} rooms loaded`);
        } catch (err) {
            setRooms([]);
            toast.error(err.message);
        }
    }

    function addEquipment() {
        if (!equipmentId) {
            toast.error("Select equipment first.");
            return;
        }

        if (Number(quantity) <= 0) {
            toast.error("Quantity must be greater than 0.");
            return;
        }

        const selected = equipment.find((item) => item.equipmentId === Number(equipmentId));

        setSelectedEquipment((prev) => [
            ...prev,
            {
                equipmentId: Number(equipmentId),
                requestedQuantity: Number(quantity),
                name: selected?.name || `Equipment ${equipmentId}`,
            },
        ]);

        setEquipmentId("");
        setQuantity(1);
        toast.success("Equipment added");
    }

    function removeEquipment(index) {
        setSelectedEquipment((prev) => prev.filter((_, i) => i !== index));
    }

    async function submitReservation(e) {
        e.preventDefault();
        setCreated(null);
        setLoading(true);

        try {
            const result = await createReservation({
                userId: activeUser.userId,
                reservationDate: form.reservationDate,
                startTime: form.startTime,
                endTime: form.endTime,
                roomId: selectedRoomId ? Number(selectedRoomId) : null,
                equipment: selectedEquipment.map((item) => ({
                    equipmentId: item.equipmentId,
                    requestedQuantity: item.requestedQuantity,
                })),
            });

            setCreated(result);
            toast.success(`Reservation #${result.reservationId} created`);
        } catch (err) {
            toast.error(err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <section>
            <Hero
                eyebrow="Reservation workflow"
                title="Create Reservation Request"
                subtitle="Create a pending reservation using the Spring Boot API and PostgreSQL transaction logic."
            />

            <div className="wizard-steps">
                <Step number="01" title="Define interval" />
                <Step number="02" title="Choose room" />
                <Step number="03" title="Add equipment" />
                <Step number="04" title="Submit request" />
            </div>

            <div className="workspace-grid">
                <form className="panel page-form" onSubmit={loadAvailableRooms}>
                    <SectionTitle
                        icon={Search}
                        title="1. Load available rooms"
                        subtitle="Search the database before choosing a room."
                    />

                    <div className="form-grid">
                        <Input label="Reservation date" type="date" value={form.reservationDate} onChange={(v) => updateField("reservationDate", v)} />
                        <Input label="Start time" type="time" value={form.startTime} onChange={(v) => updateField("startTime", v)} />
                        <Input label="End time" type="time" value={form.endTime} onChange={(v) => updateField("endTime", v)} />
                        <Input label="Minimum capacity" type="number" value={form.minimumCapacity} onChange={(v) => updateField("minimumCapacity", v)} />
                    </div>

                    <button className="secondary full">
                        Load available rooms
                        <RefreshCcw size={18} />
                    </button>
                </form>

                <form className="panel page-form" onSubmit={submitReservation}>
                    <SectionTitle
                        icon={PlusCircle}
                        title="2. Submit reservation"
                        subtitle="The requester is the currently logged-in database user."
                    />

                    <div className="requester-card">
                        <div className="avatar">{getInitials(activeUser.fullName)}</div>
                        <div>
                            <span>Requester</span>
                            <strong>{activeUser.fullName}</strong>
                        </div>
                        <Badge value={activeUser.role} />
                    </div>

                    <label>
                        Selected room
                        <select value={selectedRoomId} onChange={(e) => setSelectedRoomId(e.target.value)}>
                            <option value="">No room / equipment-only reservation</option>
                            {rooms.map((room) => (
                                <option key={room.roomId} value={room.roomId}>
                                    {room.roomCode} | {room.type} | capacity {room.capacity}
                                </option>
                            ))}
                        </select>
                    </label>

                    <div className="equipment-box">
                        <h4>Optional equipment</h4>

                        <label>
                            Equipment
                            <select value={equipmentId} onChange={(e) => setEquipmentId(e.target.value)}>
                                <option value="">Select equipment</option>
                                {equipment.map((item) => (
                                    <option key={item.equipmentId} value={item.equipmentId}>
                                        {item.name} | stock {item.stockQuantity}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <Input label="Quantity" type="number" value={quantity} onChange={setQuantity} />

                        <button type="button" className="secondary full" onClick={addEquipment}>
                            Add equipment
                            <Wrench size={18} />
                        </button>

                        {selectedEquipment.length > 0 && (
                            <ul className="selected-list">
                                {selectedEquipment.map((item, index) => (
                                    <li key={`${item.equipmentId}-${index}`}>
                                        <span>{item.name} x{item.requestedQuantity}</span>
                                        <button type="button" onClick={() => removeEquipment(index)}>
                                            Remove
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>

                    <button className="primary full" disabled={loading}>
                        {loading ? "Creating..." : "Create reservation"}
                        <ArrowRight size={18} />
                    </button>
                </form>
            </div>

            {created && (
                <div className="result-card">
                    <h3>Reservation created successfully</h3>
                    <pre>{JSON.stringify(created, null, 2)}</pre>
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
                    <PlusCircle size={16} />
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

function Step({ number, title }) {
    return (
        <div className="step">
            <span>{number}</span>
            <strong>{title}</strong>
        </div>
    );
}

function Input({ label, type = "text", value, onChange }) {
    return (
        <label>
            {label}
            <input type={type} value={value} onChange={(e) => onChange(e.target.value)} />
        </label>
    );
}

function Badge({ value }) {
    return <span className={`badge ${String(value || "").toLowerCase()}`}>{value}</span>;
}

function getInitials(name) {
    return String(name || "U")
        .split(" ")
        .map((part) => part[0])
        .slice(0, 2)
        .join("")
        .toUpperCase();
}