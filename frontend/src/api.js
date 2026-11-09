const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";

let csrfToken = null;

async function fetchCsrfToken() {
    const response = await fetch(`${API_BASE_URL}/auth/csrf`, {
        credentials: "same-origin",
    });
    if (!response.ok) {
        throw new Error("Could not initialize request security.");
    }
    const data = await response.json();
    csrfToken = data.token;
    return csrfToken;
}

async function ensureCsrfToken() {
    return csrfToken || fetchCsrfToken();
}

async function request(path, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const isMutation = !["GET", "HEAD", "OPTIONS"].includes(method);
    const token = isMutation ? await ensureCsrfToken() : null;
    const response = await fetch(`${API_BASE_URL}${path}`, {
        credentials: "same-origin",
        ...options,
        headers: {
            ...(options.body ? { "Content-Type": "application/json" } : {}),
            ...(token ? { "X-CSRF-TOKEN": token } : {}),
            ...(options.headers || {}),
        },
    });

    if (!response.ok) {
        let message = `Request failed with status ${response.status}`;
        try {
            const data = await response.json();
            message = data.message || message;
        } catch {
            // Keep the HTTP status message when JSON is unavailable.
        }
        const error = new Error(message);
        error.status = response.status;
        throw error;
    }
    return response.status === 204 ? null : response.json();
}

export async function login(username, password) {
    const token = await ensureCsrfToken();
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: "POST",
        credentials: "same-origin",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "X-CSRF-TOKEN": token,
        },
        body: new URLSearchParams({ username, password }).toString(),
    });
    if (!response.ok) {
        throw new Error("Invalid username or password.");
    }
    const user = await response.json();
    csrfToken = null;
    await fetchCsrfToken();
    return user;
}

export async function logout() {
    try {
        await request("/auth/logout", { method: "POST" });
    } finally {
        csrfToken = null;
    }
}

export function getCurrentUser() {
    return request("/auth/me");
}

export const getHealth = () => request("/health");
export const getDashboardStats = () => request("/dashboard");
export const getUsers = () => request("/users");
export const getApprovers = () => request("/approvers");
export const getEquipment = () => request("/equipment");
export const getRooms = () => request("/rooms");
export const searchRooms = (payload) => request("/rooms/search", { method: "POST", body: JSON.stringify(payload) });
export const createReservation = (payload) => request("/reservations", { method: "POST", body: JSON.stringify(payload) });
export const getReservation = (id) => request(`/reservations/${id}`);
export const getPendingReservations = () => request("/reservations/pending");
export const approveReservation = (payload) => request("/approvals", { method: "POST", body: JSON.stringify(payload) });
export const getRoomUtilizationReport = () => request("/reports/room-utilization");
export const getEquipmentDemandReport = () => request("/reports/equipment-demand");
