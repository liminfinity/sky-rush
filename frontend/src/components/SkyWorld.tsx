import { useId } from 'react';
import type { Theme } from '../api/types';
/** Original scenery. Decorative motion never reads or generates gameplay values. */
export function SkyWorld({
  theme = 'GREEN',
  className = '',
}: {
  theme?: Theme;
  className?: string;
}) {
  const id = useId();
  const red = theme === 'RED';
  return (
    <div
      className={`sky-world world-${theme.toLowerCase()} ${className}`}
      aria-hidden="true"
    >
      <svg
        className="landscape"
        viewBox="0 0 1600 1000"
        preserveAspectRatio="xMidYMax slice"
      >
        <defs>
          <linearGradient id={id + 'sky'} x2="0" y2="1">
            <stop stopColor={red ? '#343954' : '#397d9e'} />
            <stop offset=".53" stopColor={red ? '#c37d7c' : '#83c8d4'} />
            <stop offset="1" stopColor={red ? '#ffdca0' : '#e3efc3'} />
          </linearGradient>
          <linearGradient id={id + 'sea'} x2="0" y2="1">
            <stop stopColor={red ? '#f3b68b' : '#94dacf'} />
            <stop offset="1" stopColor={red ? '#ab6372' : '#357f83'} />
          </linearGradient>
          <radialGradient id={id + 'sun'}>
            <stop stopColor="#fff6c9" stopOpacity=".65" />
            <stop offset="1" stopColor="#fff6c9" stopOpacity="0" />
          </radialGradient>
        </defs>
        <path fill={`url(#${id}sky)`} d="M0 0h1600v1000H0z" />
        <circle
          cx={red ? 1160 : 1230}
          cy={red ? 475 : 250}
          r="260"
          fill={`url(#${id}sun)`}
        />
        <circle
          cx={red ? 1160 : 1230}
          cy={red ? 475 : 250}
          r={red ? 82 : 52}
          fill="#fff2c4"
          opacity=".9"
        />
        {red ? (
          <>
            <path
              d="M0 727 177 595 242 639 450 360 603 615 681 560 870 730 1150 551 1353 697 1490 433 1600 571V1000H0Z"
              fill="#976f83"
            />
            <path d="m450 360-86 168 84-45 75 71Z" fill="#e5afb0" />
            <path d="m1490 433-78 142 66-27 71 66Z" fill="#e9b9ac" />
            <path
              d="M0 878 205 748 427 812 649 600 920 820 1124 737 1390 795 1600 707v293H0Z"
              fill="#645973"
            />
          </>
        ) : (
          <>
            <path
              d="M0 780Q96 584 235 695T482 657Q588 577 720 732T1000 698Q1090 583 1190 719T1600 658V1000H0Z"
              fill="#79b5b3"
            />
            <path
              d="M0 882 155 780 340 826 517 604 638 780 766 752 930 873 1250 753 1600 822V1000H0Z"
              fill="#508f95"
            />
            <path d="m517 604-69 135 69-34 47 19Z" fill="#cee3d2" />
          </>
        )}
        <path
          d="M0 886Q490 804 800 856T1600 850v150H0Z"
          fill={`url(#${id}sea)`}
        />
        <g
          fill="none"
          stroke={red ? '#fbd5ab' : '#d2eee0'}
          strokeWidth="3"
          opacity=".5"
        >
          <path d="M790 880h180m70 20h250M600 919h225m315 24h240M845 972h180" />
        </g>
        <path
          d="M0 787Q155 732 289 853T603 922L734 1000H0Z"
          fill={red ? '#4a4b61' : '#276d73'}
        />
        <path
          d="M1600 787q-190 9-297 119t-227 94h524Z"
          fill={red ? '#474559' : '#2b676b'}
        />
        <path
          d="M0 902q171-23 345 98H0Zm1600-27q-165 38-230 125h230Z"
          fill={red ? '#313d50' : '#1c515b'}
        />
        <g fill={red ? '#303c4d' : '#1c5860'}>
          <path d="m129 789-38 94h76Zm-8 37-42 86h94ZM1314 893l-30 73h60Zm145-58-41 105h82Z" />
          <path d="M122 874h12v94h-12Zm1186 65h10v44h-10Zm145-10h12v71h-12Z" />
        </g>
      </svg>
      <svg className="world-cloud cloud-far" viewBox="0 0 420 120">
        <path
          d="M0 104q25-38 77-30 7-61 66-48 40-51 96-4 49-5 60 49 77-11 112 34-194 25-411-1Z"
          fill="currentColor"
        />
      </svg>
      <svg className="world-cloud cloud-mid" viewBox="0 0 420 120">
        <path
          d="M0 104q25-38 77-30 7-61 66-48 40-51 96-4 49-5 60 49 77-11 112 34-194 25-411-1Z"
          fill="currentColor"
        />
      </svg>
      <svg className="world-cloud cloud-near" viewBox="0 0 420 120">
        <path
          d="M0 104q25-38 77-30 7-61 66-48 40-51 96-4 49-5 60 49 77-11 112 34-194 25-411-1Z"
          fill="currentColor"
        />
      </svg>
      <svg
        className="sky-birds"
        viewBox="0 0 200 80"
        fill="none"
        stroke="currentColor"
        strokeWidth="2.5"
        strokeLinecap="round"
      >
        <path d="M10 40q14-7 24 5 6-14 20-16M83 19q10-3 19 5 4-10 15-12M130 57q11-4 20 4 6-11 19-9" />
      </svg>
    </div>
  );
}
