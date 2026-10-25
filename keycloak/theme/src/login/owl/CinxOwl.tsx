import { useEffect, useRef, useState } from "react";
import { RuntimeLoader, useRive } from "@rive-app/react-canvas-lite";
import wasmUrl from "@rive-app/canvas-lite/rive.wasm?url";
import owlRiv from "../../assets/rive/owl.riv?url";
import { OwlSvg } from "./OwlSvg";

// the runtime ships with the theme, not from the Rive CDN
RuntimeLoader.setWasmUrl(wasmUrl);

/** The free-plan Rive splash plays when the canvas starts; a cue fired before it ends would be missed. */
const SPLASH_MS = 2300;
const LOOP_MS = 25_000;
const DOZE_MS = 45_000;
const FLIGHT_MS = 2400;
// pointer distance (px) at which the eyes have turned half way
const REACH = 160;
const MOODS = ["listening", "thinking", "talking"] as const;

export type OwlMood = "error" | "success";

/** Fired by a password field's eye button so the owl can peek. */
export const REVEAL_EVENT = "cinx:reveal";

function prefersReducedMotion() {
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches;
}

/**
 * The owl on the island. It watches the page rather than being wired into each form: it follows the text field being
 * typed in, looks away from a password, peeks when one is shown, thinks while a form posts, reacts to the page's
 * error or success, takes a short flight after 25 s without input and dozes after 45 s. Reduced motion shows the still.
 */
export function CinxOwl({ mood }: { mood?: OwlMood }) {
  const [still] = useState(prefersReducedMotion);
  return <span className="owl" aria-hidden>{still ? <OwlSvg /> : <RiveOwl mood={mood} />}</span>;
}

function RiveOwl({ mood }: { mood?: OwlMood }) {
  const host = useRef<HTMLSpanElement>(null);
  const [ready, setReady] = useState(false);
  const { rive, RiveComponent } = useRive({
    src: owlRiv,
    artboard: "Owl",
    stateMachines: "Owl",
    autoplay: true,
    autoBind: true,
    onLoad: () => setReady(true),
  });

  useEffect(() => {
    const vm = rive?.viewModelInstance, el = host.current;
    if (!vm || !el) return;
    const lookX = vm.number("lookX"), lookY = vm.number("lookY"), sleepy = vm.boolean("sleepy"), flying = vm.boolean("flying");
    const fire = (name: string) => vm.trigger(name)?.trigger();
    const setMood = (m: (typeof MOODS)[number] | null) => MOODS.forEach((k) => { const p = vm.boolean(k); if (p) p.value = k === m; });
    let last = performance.now(), looped = false, land = 0, rest = 0;

    const look = (x: number, y: number) => {
      const box = el.getBoundingClientRect();
      const dx = x - (box.left + box.width / 2), dy = y - (box.top + box.height / 2);
      if (lookX) lookX.value = dx / (Math.abs(dx) + REACH);
      if (lookY) lookY.value = dy / (Math.abs(dy) + REACH);
    };
    const wake = () => {
      last = performance.now();
      looped = false;
      if (sleepy?.value) sleepy.value = false;
    };
    const field = (target: EventTarget | null) =>
      target instanceof HTMLInputElement && !["checkbox", "radio", "hidden", "submit", "button"].includes(target.type) ? target : null;
    const follow = (input: HTMLInputElement) => {
      if (input.type === "password") {
        // eyes up and away from the form
        look(-1e4, -1e4);
        setMood(null);
        return;
      }
      const box = input.getBoundingClientRect();
      look(box.left + Math.min(box.width - 12, 14 + input.value.length * 8.5), box.top + box.height / 2);
      setMood("listening");
    };

    const onFocus = (e: FocusEvent) => { const input = field(e.target); if (input) { window.clearTimeout(rest); follow(input); } wake(); };
    const onInput = (e: Event) => { const input = field(e.target); if (input) follow(input); wake(); };
    const onBlur = () => {
      rest = window.setTimeout(() => {
        if (field(document.activeElement)) return;
        setMood(null);
        if (lookX) lookX.value = 0;
        if (lookY) lookY.value = 0;
      }, 120);
    };
    const onMove = (e: PointerEvent) => { if (!field(document.activeElement)) look(e.clientX, e.clientY); wake(); };
    const onSubmit = () => { setMood("thinking"); wake(); };
    const onReveal = (e: Event) => { if ((e as CustomEvent<boolean>).detail) fire("peek"); wake(); };
    const idle = window.setInterval(() => {
      const quiet = performance.now() - last;
      if (quiet > LOOP_MS && !looped && flying && !sleepy?.value) {
        looped = true;
        flying.value = true;
        land = window.setTimeout(() => { flying.value = false; }, FLIGHT_MS);
      }
      if (sleepy && !sleepy.value && quiet > DOZE_MS) sleepy.value = true;
    }, 5000);
    const cue = mood ? window.setTimeout(() => fire(mood === "error" ? "confused" : "happy"), SPLASH_MS + 300) : 0;

    document.addEventListener("focusin", onFocus);
    document.addEventListener("input", onInput);
    document.addEventListener("focusout", onBlur);
    document.addEventListener("submit", onSubmit);
    document.addEventListener(REVEAL_EVENT, onReveal);
    window.addEventListener("pointermove", onMove, { passive: true });
    if (field(document.activeElement)) follow(document.activeElement as HTMLInputElement);
    return () => {
      document.removeEventListener("focusin", onFocus);
      document.removeEventListener("input", onInput);
      document.removeEventListener("focusout", onBlur);
      document.removeEventListener("submit", onSubmit);
      document.removeEventListener(REVEAL_EVENT, onReveal);
      window.removeEventListener("pointermove", onMove);
      window.clearInterval(idle);
      window.clearTimeout(land);
      window.clearTimeout(rest);
      window.clearTimeout(cue);
    };
  }, [rive, mood]);

  return (
    <span ref={host} className={ready ? "owl-host is-rive" : "owl-host"}>
      <OwlSvg />
      <RiveComponent className="owl-rive" />
    </span>
  );
}
