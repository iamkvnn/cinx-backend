import { sunCrossings, sunElevation } from "./sun";

/**
 * The sky photos through the day and the light each one gives the island still. A trimmed port of cinx-web
 * `features/catalog/components/landing/sky/skies.ts` (keys, elevations and brightness are the same).
 */
export type Light = "dawn" | "day" | "golden" | "dusk" | "night";
export type Palette = "day" | "dusk" | "night";
export type Greeting = "night" | "dawn" | "dusk" | "morning" | "noon" | "afternoon";

interface SkyKey {
  /** the photo file name; "night" uses night-lite */
  key: string;
  el: number;
  afternoon: boolean;
  /** brightness behind the greeting, which picks the copy's palette */
  lum: number;
  light: Light;
}

const NIGHT: SkyKey = { key: "night", el: -12, afternoon: true, lum: 0.14, light: "night" };

const SKIES: SkyKey[] = [
  NIGHT,
  { key: "dawn", el: -7, afternoon: false, lum: 0.6, light: "dawn" },
  { key: "sunrise", el: 1, afternoon: false, lum: 0.72, light: "golden" },
  { key: "morning", el: 25, afternoon: false, lum: 0.52, light: "day" },
  { key: "noon", el: 90, afternoon: false, lum: 0.44, light: "day" },
  { key: "afternoon", el: 40, afternoon: true, lum: 0.68, light: "day" },
  { key: "golden", el: 14, afternoon: true, lum: 0.58, light: "golden" },
  { key: "sunset", el: 3, afternoon: true, lum: 0.67, light: "golden" },
  { key: "dusk", el: -1.5, afternoon: true, lum: 0.58, light: "dusk" },
  { key: "bluehour", el: -6, afternoon: true, lum: 0.3, light: "dusk" },
  NIGHT,
];

const smooth = (x: number, a: number, b: number) => {
  const t = Math.min(1, Math.max(0, (x - a) / (b - a)));
  return t * t * (3 - 2 * t);
};

export interface SkyState {
  /** the photo before and after, and how far the change has gone (0–1) */
  photos: [string, string];
  f: number;
  lights: [Light, Light];
  lightF: number;
  palette: Palette;
  greeting: Greeting;
}

export function skyAt(hour: number, date = new Date()): SkyState {
  const { noon, cross } = sunCrossings(date);
  const times = SKIES.map((s, i) => (i === 0 ? -1 : s.key === "noon" ? noon : cross(s.el, s.afternoon)));
  times[times.length - 1] = Math.max(times[times.length - 1], times[times.length - 2] + 0.3);
  let i = 0;
  while (i < times.length - 2 && hour >= times[i + 1]) i++;
  const a = SKIES[i], b = SKIES[i + 1], ta = times[i], tb = times[i + 1];
  // each photo holds around its moment; the change happens across the middle of the gap (night holds until just before dawn)
  const mid = i === 0 ? tb - 0.35 : (ta + tb) / 2;
  const fade = i === 0 ? 0.5 : Math.min(0.5, (tb - ta) * 0.55);
  const f = hour < ta ? 0 : smooth(hour, mid - fade / 2, mid + fade / 2);
  const el = sunElevation(hour, date);
  const lum = a.lum + (b.lum - a.lum) * f;
  return {
    photos: [a.key, b.key],
    f,
    lights: [a.light, b.light],
    lightF: a.light === b.light ? 0 : f,
    palette: lum > 0.42 ? "day" : lum > 0.22 ? "dusk" : "night",
    greeting: el < -7 ? "night" : el < 2 ? (hour < 12 ? "dawn" : "dusk") : hour < 11 ? "morning" : hour < 15.5 ? "noon" : "afternoon",
  };
}
