import { useId } from 'react';
import { useCosmetics } from '../features/profile/Cosmetics';
export function Balloon({
  small = false,
  skin,
}: {
  small?: boolean;
  skin?: string;
}) {
  const equipped = useCosmetics();
  const cosmetic = skin ?? equipped.balloonSkin;
  const id = useId();
  return (
    <svg
      className={small ? 'balloon-svg small' : 'balloon-svg'}
      data-skin={cosmetic}
      viewBox="0 0 260 350"
      fill="none"
      aria-hidden="true"
    >
      <defs>
        <linearGradient
          id={id + 'fabric'}
          x1="36"
          y1="35"
          x2="218"
          y2="245"
          gradientUnits="userSpaceOnUse"
        >
          <stop stopColor="var(--balloon-light)" />
          <stop offset=".6" stopColor="var(--balloon)" />
          <stop offset="1" stopColor="var(--balloon-dark)" />
        </linearGradient>
        <radialGradient id={id + 'shine'} cx=".3" cy=".2" r=".8">
          <stop stopColor="#fff9de" stopOpacity=".55" />
          <stop offset="1" stopColor="#fff9de" stopOpacity="0" />
        </radialGradient>
        <linearGradient id={id + 'basket'}>
          <stop stopColor="#d3a267" />
          <stop offset="1" stopColor="#7b472e" />
        </linearGradient>
      </defs>
      <path
        d="M130 15C58 15 19 56 22 116c3 69 64 125 86 160h44c22-35 83-91 86-160 3-60-36-101-108-101Z"
        fill={`url(#${id}fabric)`}
        stroke="var(--balloon-dark)"
        strokeWidth="3"
      />
      <path
        d="M130 16C73 26 53 71 59 123c6 58 42 111 55 153h32c13-42 49-95 55-153 6-52-14-97-71-107Z"
        fill="#fff4d8"
        opacity=".92"
      />
      <path
        d="M130 16c-25 23-35 56-32 109 3 63 21 111 25 151h14c4-40 22-88 25-151 3-53-7-86-32-109Z"
        fill={`url(#${id}fabric)`}
      />
      <path
        d="M130 15C58 15 19 56 22 116c3 69 64 125 86 160h44c22-35 83-91 86-160 3-60-36-101-108-101Z"
        fill={`url(#${id}shine)`}
      />
      <g stroke="var(--balloon-dark)" opacity=".32">
        <path d="M130 16C65 25 46 80 59 137s42 105 55 139M130 16c-23 29-35 60-32 109s20 118 25 151M130 16c23 29 35 60 32 109s-20 118-25 151M130 16c65 9 84 64 71 121s-42 105-55 139" />
      </g>
      <path
        d="M38 130q92 36 184-2"
        stroke="#fff7de"
        strokeWidth="3"
        opacity=".35"
      />
      {cosmetic === 'constellations' && (
        <g stroke="#fff6c3" strokeWidth="2" fill="#fff6c3">
          <path d="m81 99 46 37 48-44-20 95-56-3" fill="none" />
          {[
            [81, 99],
            [127, 136],
            [175, 92],
            [155, 187],
            [99, 184],
          ].map(([x, y]) => (
            <path key={x} d={`M${x} ${y - 7}l2 5 5 2-5 2-2 5-2-5-5-2 5-2Z`} />
          ))}
        </g>
      )}
      {cosmetic === 'ribbon' && (
        <g stroke="#ffdf82" strokeWidth="7" fill="none">
          <path d="M40 141q90 70 180 0M52 169q78 62 156 0" />
          <path d="m123 195 7 17 7-17" />
        </g>
      )}
      <path d="M108 276h44l-7 12h-30Z" fill="var(--balloon-dark)" />
      <path
        d="m111 279 8 32m30-32-8 32m-25-32 6 32m23-32-6 32"
        stroke="#59433a"
        strokeWidth="2.5"
      />
      <path
        className="burner"
        d="M130 315q-18-16 0-39 18 23 0 39"
        fill="#ffd47e"
      />
      <path
        d="M111 313h38l-5 29h-28Z"
        fill={`url(#${id}basket)`}
        stroke="#794830"
        strokeWidth="2"
      />
      <path
        d="M112 317h36m-34 8h32m-30 8h28m-23-18 2 26m7-26v26m9-26-2 26"
        stroke="#e4bf80"
        strokeWidth="1.5"
        opacity=".7"
      />
      <path
        d="M43 90q9-34 41-45"
        stroke="#fffdf0"
        strokeOpacity=".55"
        strokeWidth="8"
        strokeLinecap="round"
      />
    </svg>
  );
}
