const shapes = {
  laptop: <><rect x="4" y="4" width="16" height="12" rx="2" /><path d="m4 16-2 4h20l-2-4" /></>,
  check: <><circle cx="12" cy="12" r="9" /><path d="m8 12 3 3 5-6" /></>,
  bell: <><path d="M5 17h14l-2-4V9a5 5 0 0 0-10 0v4zM10 21h4" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  wifi: <><path d="M3 9a14 14 0 0 1 18 0M6 12a9 9 0 0 1 12 0m-9 3a4.5 4.5 0 0 1 6 0" /><circle cx="12" cy="18" r=".7" /></>,
  lock: <><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></>,
  printer: <><path d="M7 8V3h10v5M7 17H4V8h16v9h-3M7 12h2" /><path d="M7 15h10v6H7z" /></>,
};

export default function DashboardIcon({ name }) {
  return (
    <span className="itsm-icon" aria-hidden="true">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round">
        {shapes[name]}
      </svg>
    </span>
  );
}
