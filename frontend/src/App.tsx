import { QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route } from "react-router-dom";
import { queryClient } from "./lib/queryClient";
import RootLayout from "./layouts/RootLayout";
import Dashboard from "./pages/Dashboard";
import Performance from "./pages/Performance";
import Alerts from "./pages/Alerts";
import Monitoring from "./pages/Monitoring";
import NotFound from "./pages/NotFound";
import "./index.css";

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<RootLayout />}>
            <Route path="/" element={<Dashboard />} />
            <Route path="/performance" element={<Performance />} />
            <Route path="/alerts" element={<Alerts />} />
            <Route path="/monitoring" element={<Monitoring />} />
            <Route path="*" element={<NotFound />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  );
}
