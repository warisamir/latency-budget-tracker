import { Outlet } from "react-router-dom";
import Navigation from "../components/Navigation";

export default function RootLayout() {
  return (
    <div className="flex h-screen flex-col bg-slate-950">
      <Navigation />
      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}
