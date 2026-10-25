/**
 * The sun's elevation for the visitor's hour: NOAA's simple solar position for central Vietnam (16°N, 107.5°E, UTC+7).
 * Ported from cinx-web `features/catalog/components/landing/sky/sun.ts`; keep the two in step.
 */
const LAT = (16 * Math.PI) / 180;
const LON = 107.5;
const TZ = 7;

function dayOfYear(date: Date) {
  return Math.floor((date.getTime() - new Date(date.getFullYear(), 0, 0).getTime()) / 864e5);
}

/** Elevation in degrees, negative below the horizon. */
export function sunElevation(hour: number, date = new Date()): number {
  const b = ((2 * Math.PI) / 365) * (dayOfYear(date) - 81);
  const eot = 9.87 * Math.sin(2 * b) - 7.53 * Math.cos(b) - 1.5 * Math.sin(b);
  const solar = hour + (4 * (LON - 15 * TZ) + eot) / 60;
  const hra = ((15 * (solar - 12)) * Math.PI) / 180;
  const decl = ((23.45 * Math.PI) / 180) * Math.sin(b);
  return (Math.asin(Math.sin(decl) * Math.sin(LAT) + Math.cos(decl) * Math.cos(LAT) * Math.cos(hra)) * 180) / Math.PI;
}

/** The hours today at which the sun crosses `elevation`, before and after its highest point, plus that noon. */
export function sunCrossings(date = new Date()) {
  const els = Array.from({ length: 24 * 12 + 1 }, (_, i) => sunElevation(i / 12, date));
  const peakIndex = els.indexOf(Math.max(...els));
  const cross = (elevation: number, afternoon: boolean) => {
    const from = afternoon ? peakIndex : 0;
    const to = afternoon ? els.length - 1 : peakIndex;
    for (let i = from; i < to; i++) if ((els[i] - elevation) * (els[i + 1] - elevation) <= 0) return i / 12;
    return afternoon ? 23.9 : 0.1;
  };
  return { noon: peakIndex / 12, cross };
}
