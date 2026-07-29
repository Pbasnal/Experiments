type HowStepIconName = 'upload' | 'schedule' | 'share';

const icons: Record<HowStepIconName, JSX.Element> = {
  upload: (
    <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
      <rect x="10" y="14" width="22" height="28" rx="2.5" fill="currentColor" opacity="0.18" />
      <rect x="14" y="10" width="22" height="28" rx="2.5" fill="currentColor" opacity="0.35" />
      <rect x="18" y="6" width="22" height="28" rx="2.5" stroke="currentColor" strokeWidth="2" fill="var(--primary-soft)" />
      <path
        d="M29 16v8M25 20h8"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  ),
  schedule: (
    <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
      <rect x="8" y="12" width="32" height="28" rx="3" fill="var(--primary-soft)" stroke="currentColor" strokeWidth="2" />
      <path d="M8 20h32" stroke="currentColor" strokeWidth="2" />
      <path d="M16 8v6M32 8v6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
      <circle cx="18" cy="28" r="2.25" fill="currentColor" />
      <circle cx="24" cy="28" r="2.25" fill="currentColor" opacity="0.45" />
      <circle cx="30" cy="28" r="2.25" fill="currentColor" opacity="0.45" />
      <circle cx="18" cy="34" r="2.25" fill="currentColor" opacity="0.45" />
      <circle cx="24" cy="34" r="2.25" fill="currentColor" />
    </svg>
  ),
  share: (
    <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
      <circle cx="14" cy="24" r="5" fill="var(--primary-soft)" stroke="currentColor" strokeWidth="2" />
      <circle cx="34" cy="14" r="5" fill="var(--primary-soft)" stroke="currentColor" strokeWidth="2" />
      <circle cx="34" cy="34" r="5" fill="var(--primary-soft)" stroke="currentColor" strokeWidth="2" />
      <path d="M18.5 21.5 29.5 16M18.5 26.5 29.5 32" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  ),
};

export default function HowStepIcon({ name, step }: { name: HowStepIconName; step: number }) {
  return (
    <span className="how-icon">
      {icons[name]}
      <span className="how-num">{step}</span>
    </span>
  );
}
