import { Moon, Sun, Sunrise, Sunset } from "lucide-react";
import type { SkyState } from "../sky/skies";
import { CinxOwl, type OwlMood } from "../owl/CinxOwl";

const PHOTOS = import.meta.glob<string>("../../assets/sky/*.webp", { eager: true, query: "?url", import: "default" });
const ISLES = import.meta.glob<string>("../../assets/isles/*.webp", { eager: true, query: "?url", import: "default" });

const photo = (key: string) => PHOTOS[`../../assets/sky/${key === "night" ? "night-lite" : key}.webp`];
const isle = (light: string) => ISLES[`../../assets/isles/perch-${light}.webp`];

/** The hour's two sky photos, cross-faded. */
export function Sky({ sky }: { sky: SkyState }) {
  return (
    <div className="sky" aria-hidden>
      <i style={{ backgroundImage: `url(${photo(sky.photos[0])})` }} />
      <i style={{ backgroundImage: `url(${photo(sky.photos[1])})`, opacity: sky.f }} />
    </div>
  );
}

/** The island still in the hour's light, with the owl standing where the landing's 3D owl sits. */
export function Island({ sky, mood }: { sky: SkyState; mood?: OwlMood }) {
  return (
    <div className="isle" aria-hidden>
      <img src={isle(sky.lights[0])} alt="" width={900} height={900} />
      {sky.lightF > 0 ? <img className="is-b" src={isle(sky.lights[1])} alt="" width={900} height={900} style={{ opacity: sky.lightF }} /> : null}
      <CinxOwl mood={mood} />
    </div>
  );
}

const CHIP_ICON = { night: Moon, dawn: Sunrise, dusk: Sunset, morning: Sun, noon: Sun, afternoon: Sun };

/** "09:00 · ☀ Chào buổi sáng". */
export function ClockChip({ sky, time, label }: { sky: SkyState; time: string; label: string }) {
  const Icon = CHIP_ICON[sky.greeting];
  return (
    <span className="chip">
      <Icon aria-hidden />
      {time} · {label}
    </span>
  );
}

/** The cinx mark, as in cinx-web's LearnNav. */
export function LearnMark({ href }: { href?: string }) {
  const mark = (
    <>
      <svg viewBox="0 0 32 32" aria-hidden>
        <rect width="32" height="32" rx="10" />
        <path d="M9 21.5c3.2 0 3.6-11 7-11s3.8 11 7 11" fill="none" stroke="#fff" strokeWidth="3.2" strokeLinecap="round" />
      </svg>
      cinx
    </>
  );
  return href ? <a className="mark" href={href}>{mark}</a> : <span className="mark">{mark}</span>;
}

/** VI · EN, as links that reload the page in that language. */
export function LangSwitch(props: { languages: { languageTag: string; label: string; href: string }[]; current: string; label: string }) {
  const { languages, current, label } = props;
  // Keycloak lists the locales alphabetically; cinx shows Vietnamese first
  const ordered = [...languages].sort((a, b) => Number(b.languageTag === "vi") - Number(a.languageTag === "vi"));
  return (
    <nav className="lang" aria-label={label}>
      {ordered.map((l) => (
        <a key={l.languageTag} href={l.href} lang={l.languageTag} aria-current={l.languageTag === current ? "true" : undefined} title={l.label}>
          {l.languageTag.slice(0, 2).toUpperCase()}
        </a>
      ))}
    </nav>
  );
}
