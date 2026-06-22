export type Stage =
  | "AUTHENTICATION"
  | "VALIDATION"
  | "BUSINESS_LOGIC"
  | "CACHE_ACCESS"
  | "DATABASE_QUERY"
  | "RESPONSE_SERIALIZATION";

export type Severity = "OK" | "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface StageLatency {
  stage: Stage;
  actualMs: number;
  budgetMs: number;
  exceeded: boolean;
  deviationPercent: number;
  severity: Severity;
}

export interface LatencyReport {
  totalLatencyMs: number;
  budgetMs: number;
  withinBudget: boolean;
  overallDeviationPercent: number;
  worstSeverity: Severity;
  stages: StageLatency[];
}

export interface TransactionResponse {
  transactionId: string;
  traceId: string;
  success: boolean;
  status: string;
  fromCurrency: string;
  toCurrency: string;
  amount: number;
  convertedAmount: number;
  exchangeRate: number;
  latencyReport: LatencyReport;
  timestamp: string;
}

export interface StageStats {
  stage: Stage;
  p50Ms: number;
  p95Ms: number;
  p99Ms: number;
  avgMs: number;
  budgetMs: number;
  violations: number;
  violationRate: number;
  healthy: boolean;
}

export interface LatencyStats {
  generatedAt: string;
  window: string;
  totalRequests: number;
  budgetViolations: number;
  violationRate: number;
  stageStats: StageStats[];
}

export interface AlertDto {
  id: number;
  transactionId: string;
  traceId: string;
  stage: Stage;
  severity: Severity;
  actualLatencyMs: number;
  budgetMs: number;
  deviationPercent: number;
  message: string;
  resolved: boolean;
  resolvedAt: string | null;
  notificationSent: boolean;
  createdAt: string;
}

export interface DataPoint {
  timestamp: string;
  totalRequests: number;
  violations: number;
  violationRate: number;
  avgLatencyMs: number;
}

export interface LatencyHistoryDto {
  window: string;
  buckets: number;
  bucketDurationMs: number;
  dataPoints: DataPoint[];
}

export interface AlertsResponse {
  content: AlertDto[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export interface HealthResponse {
  status: string;
  timestamp: string;
  traceId: string;
  components: {
    db: { status: string };
    redis: { status: string };
  };
}
