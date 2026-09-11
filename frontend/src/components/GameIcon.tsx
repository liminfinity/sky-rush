export type IconName =
  | 'rules'
  | 'history'
  | 'trophy'
  | 'sound'
  | 'mute'
  | 'more'
  | 'back'
  | 'check'
  | 'arrow'
  | 'shield'
  | 'ticket';
const paths: Record<IconName, string> = {
  rules:
    'M9 9a3 3 0 0 1 6 0c0 2-3 2-3 5m0 3v.1M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20',
  history: 'M3 5v5h5M3 10a9 9 0 1 1 2 8m7-12v6l4 2',
  trophy:
    'M7 3h10v6a5 5 0 0 1-10 0V3ZM7 5H3v3a4 4 0 0 0 4 4m10-7h4v3a4 4 0 0 1-4 4m-5 2v6m-4 1h8',
  sound: 'M4 9h4l5-5v16l-5-5H4V9Zm13-2a7 7 0 0 1 0 10m2-13a11 11 0 0 1 0 16',
  mute: 'M4 9h4l5-5v16l-5-5H4V9Zm13 0 5 6m0-6-5 6',
  more: 'M5 12h.1m7 0h.1m7 0h.1',
  back: 'm14 5-7 7 7 7M7 12h14',
  check: 'm5 12 4 4L19 6',
  arrow: 'M4 12h16m-6-6 6 6-6 6',
  shield: 'm12 2 8 4v6c0 5-8 10-8 10S4 17 4 12V6l8-4Zm-4 10 3 3 5-6',
  ticket: 'M3 5h18v5a2 2 0 0 0 0 4v5H3v-5a2 2 0 0 0 0-4V5Zm12 0v3m0 3v2m0 3v3',
};
export function GameIcon({ name }: { name: IconName }) {
  return (
    <svg
      className="game-icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={name === 'more' ? 4 : 1.8}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={paths[name]} />
    </svg>
  );
}
export function FragmentIcon() {
  return (
    <svg className="fragment-art" viewBox="0 0 80 90" aria-hidden="true">
      <path
        d="m40 3 32 23-7 42-25 19L10 64 7 26Z"
        fill="#57edd4"
        stroke="#d7fff3"
        strokeWidth="3"
      />
      <path
        d="m40 3 8 31 24-8-23 28 16 14-25 19 9-33-39 10 17-32L7 26l33-23Z"
        fill="#18a998"
      />
      <path d="m40 3 8 31-21-2Z" fill="#c9ffda" />
      <path d="m27 32 22 22-9 33Z" fill="#70f0ae" />
      <path d="m27 32 21 2 1 20Z" fill="#ebffe6" />
    </svg>
  );
}

export function ScoreIcon() {
  return (
    <svg className="score-star" viewBox="0 0 80 90" aria-hidden="true">
      <path
        d="m40 5 10 25 27 2-21 18 7 28-23-15-23 15 7-28L3 32l27-2Z"
        fill="#ffda7a"
        stroke="#fff0b6"
        strokeWidth="3"
      />
      <path d="m40 5 1 39-24 34 7-28L3 32l27-2Z" fill="#e5ad57" />
      <path d="m40 5 10 25 27 2-36 12Z" fill="#fff0b6" />
    </svg>
  );
}
