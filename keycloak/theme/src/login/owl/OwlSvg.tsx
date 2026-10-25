/** The owl as a still: shown until Rive loads and under reduced motion. Copied from cinx-web `shared/ui/learn/OwlAvatar.tsx`. */
export function OwlSvg({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 100 100" aria-hidden className={className}>
      <ellipse cx="50" cy="96" rx="26" ry="3.5" fill="#16143a" opacity=".08" />
      <ellipse cx="50" cy="70" rx="31" ry="27" fill="#8a6a4f" />
      <ellipse cx="50" cy="78" rx="19" ry="16" fill="#f1e3c8" />
      <path d="M42 74l3 3 3-3M52 74l3 3 3-3M47 82l3 3 3-3" fill="none" stroke="#8a6a4f" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <ellipse cx="22" cy="72" rx="8" ry="15" fill="#76583f" transform="rotate(12 22 72)" />
      <ellipse cx="78" cy="72" rx="8" ry="15" fill="#76583f" transform="rotate(-12 78 72)" />
      <path d="M25 30 L20 10 L37 24 Z M75 30 L80 10 L63 24 Z" fill="#5e4533" stroke="#5e4533" strokeWidth="3" strokeLinejoin="round" />
      <ellipse cx="50" cy="41" rx="31" ry="27" fill="#8a6a4f" />
      <circle cx="37" cy="42" r="15.5" fill="#f6ecdb" />
      <circle cx="63" cy="42" r="15.5" fill="#f6ecdb" />
      <circle cx="37" cy="42" r="10.5" fill="#fff" />
      <circle cx="63" cy="42" r="10.5" fill="#fff" />
      <circle cx="38" cy="43" r="6.5" fill="#16143a" />
      <circle cx="64" cy="43" r="6.5" fill="#16143a" />
      <circle cx="35.5" cy="40" r="2.2" fill="#fff" />
      <circle cx="61.5" cy="40" r="2.2" fill="#fff" />
      <path d="M45 52 Q50 50 55 52 L50 61 Z" fill="#f2a33a" stroke="#e08f22" strokeWidth="1" strokeLinejoin="round" />
      <path d="M24 62 Q50 72 76 62 L76 68 Q50 79 24 68 Z" fill="#4338ca" />
      <path d="M60 68 L70 88 L62 88 L56 70 Z" fill="#3730a3" />
      <circle cx="38" cy="69" r="4.2" fill="#84cc16" stroke="#5d8f10" strokeWidth="1.2" />
    </svg>
  );
}
