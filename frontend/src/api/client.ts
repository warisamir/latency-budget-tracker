import axios from "axios";
import type { AlertDto, LatencyStats, TransactionResponse } from "../types";

const API_BASE = import.meta.env.VITE_API_URL || "/api/v1";

const client = axios.create({
  baseURL: API_BASE,
  auth: { username: "admin", password: "admin123" },
  timeout: 30_000,
  headers: { "Content-Type": "application/json" },
});

export const api = {
  createTransaction: async (payload: {
    userId: string;
    fromCurrency: string;
    toCurrency: string;
    amount: number;
  }): Promise<TransactionResponse> => {
    const { data } = await client.post<TransactionResponse>("/transactions", payload);
    return data;
  },

  getStats: async (window: "1h" | "24h" | "7d" = "1h"): Promise<LatencyStats> => {
    const { data } = await client.get<LatencyStats>(`/latency/stats?window=${window}`);
    return data;
  },

  getAlerts: async (page = 0, size = 20): Promise<{ content: AlertDto[]; totalElements: number }> => {
    const { data } = await client.get(`/alerts?page=${page}&size=${size}`);
    return data;
  },

  getCriticalAlerts: async (): Promise<AlertDto[]> => {
    const { data } = await client.get<AlertDto[]>("/alerts/critical");
    return data;
  },

  resolveAlert: async (id: number): Promise<AlertDto> => {
    const { data } = await client.patch<AlertDto>(`/alerts/${id}/resolve`);
    return data;
  },

  getHealth: async () => {
    const { data } = await client.get("/health");
    return data;
  },
};
