import { useState } from "react";
import { api } from "../api/client";
import type { TransactionResponse } from "../types";
import { Button } from "./ui/button";

interface TransactionTesterProps {
  onResult: (result: TransactionResponse) => void;
}

export default function TransactionTester({ onResult }: TransactionTesterProps) {
  const [form, setForm] = useState({
    userId: "user-demo",
    fromCurrency: "BTC",
    toCurrency: "USD",
    amount: "1.0",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const result = await api.createTransaction({
        ...form,
        amount: parseFloat(form.amount),
      });
      onResult(result);
    } catch (err: any) {
      setError(err?.response?.data?.message || err.message);
    } finally {
      setLoading(false);
    }
  };

  const fields = [
    { key: "userId", label: "User ID" },
    { key: "fromCurrency", label: "From Currency" },
    { key: "toCurrency", label: "To Currency" },
    { key: "amount", label: "Amount" },
  ];

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      {fields.map(({ key, label }) => (
        <div key={key}>
          <label className="mb-1 block text-xs text-slate-400">{label}</label>
          <input
            type="text"
            value={(form as any)[key]}
            onChange={(e) => setForm((f) => ({ ...f, [key]: e.target.value }))}
            className="w-full rounded border border-slate-600 bg-slate-800 px-3 py-2 text-sm text-white placeholder-slate-500 transition-colors focus:border-blue-500 focus:outline-none"
            placeholder={key}
          />
        </div>
      ))}

      {error && (
        <div className="rounded bg-red-900/20 px-3 py-2 text-xs text-red-400">
          ⚠ {error}
        </div>
      )}

      <Button
        type="submit"
        disabled={loading}
        className="w-full"
      >
        {loading ? "Processing…" : "Run Transaction"}
      </Button>
    </form>
  );
}
