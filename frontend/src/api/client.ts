import axios from "axios";
import type { AlertDto, LatencyStats, TransactionResponse, LatencyHistoryDto, AlertsResponse, HealthResponse } from "../types";

const API_BASE = import.meta.env.VITE_API_URL || "/api/v1";

const client = axios.create({
  baseURL: API_BASE,
  auth: {
    username: import.meta.env.VITE_API_USER || "admin",
    password: import.meta.env.VITE_API_PASSWORD || "admin123",
  },
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
    console.log("getStats response:", { window, data, stageStatsType: typeof data?.stageStats, isArray: Array.isArray(data?.stageStats) });
    return data;
  },

  getAlerts: async (page = 0, size = 20): Promise<AlertsResponse> => {
    const { data } = await client.get<AlertsResponse>(`/alerts?page=${page}&size=${size}`);
    console.log("getAlerts response:", { page, contentType: typeof data?.content, isArray: Array.isArray(data?.content) });
    return data;
  },

  getCriticalAlerts: async (): Promise<AlertDto[]> => {
    const { data } = await client.get<AlertDto[]>("/alerts/critical");
    console.log("getCriticalAlerts response:", { dataType: typeof data, isArray: Array.isArray(data) });
    return data;
  },

  getAlertCount: async (): Promise<number> => {
    const { data } = await client.get<number>("/alerts/count");
    return data;
  },

  resolveAlert: async (id: number): Promise<AlertDto> => {
    const { data } = await client.patch<AlertDto>(`/alerts/${id}/resolve`);
    return data;
  },

  getLatencyHistory: async (window: "1h" | "24h" | "7d" = "24h", buckets: number = 24): Promise<LatencyHistoryDto> => {
    const { data } = await client.get<LatencyHistoryDto>(`/latency/history?window=${window}&buckets=${buckets}`);
    console.log("getLatencyHistory response:", { window, buckets, dataType: typeof data, dataPointsType: typeof data?.dataPoints, isArray: Array.isArray(data?.dataPoints), dataPointsLength: data?.dataPoints?.length });
    return data;
  },

  getHealth: async (): Promise<HealthResponse> => {
    const { data } = await client.get<HealthResponse>("/health");
    return data;
  },
};
