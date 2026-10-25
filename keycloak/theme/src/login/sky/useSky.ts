import { useEffect, useState } from "react";
import { skyAt, type SkyState } from "./skies";

function now() {
  const date = new Date();
  // dev builds take ?t=<hour> to look at another time of day
  const pinned = import.meta.env.DEV ? Number(new URLSearchParams(location.search).get("t")) : NaN;
  const hour = Number.isFinite(pinned) && pinned > 0 ? pinned : date.getHours() + date.getMinutes() / 60;
  return { date, hour };
}

function read(): SkyState & { time: string } {
  const { date, hour } = now();
  const h = Math.floor(hour), m = Math.round((hour - h) * 60);
  return { ...skyAt(hour, date), time: `${String(h).padStart(2, "0")}:${String(m % 60).padStart(2, "0")}` };
}

/** The sky of the visitor's local hour, refreshed every minute. */
export function useSky() {
  const [sky, setSky] = useState(read);
  useEffect(() => {
    const timer = window.setInterval(() => setSky(read()), 60_000);
    return () => window.clearInterval(timer);
  }, []);
  return sky;
}
