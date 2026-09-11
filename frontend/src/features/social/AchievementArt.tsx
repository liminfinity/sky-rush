/** Original vector badges. Artwork is presentation only; unlocks come from the API. */
export function AchievementArt({ id }: { id: string }) {
  const veteran = id.startsWith('VETERAN');
  const streak = id.startsWith('WIN_STREAK');
  const booster = id.startsWith('BOOSTER');
  const win = id === 'FIRST_WIN' || id === 'BIG_WIN';
  return (
    <svg
      className="achievement-art"
      viewBox="0 0 80 80"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="m24 48-7 25 15-6 8 9 6-28M48 48l15 25-1-17 12-3-20-14"
        fill={streak ? '#c77760' : '#4b9b9c'}
      />
      <path
        d="M40 5 64 16 72 42 57 63H23L8 42l8-26Z"
        fill="#e6b963"
        stroke="#b88035"
        strokeWidth="2"
      />
      <circle
        cx="40"
        cy="34"
        r="24"
        fill={booster ? '#2e5a70' : streak ? '#70465d' : '#235b63'}
      />
      <path
        d="M22 22c5-8 20-13 30-3"
        stroke="#fff"
        opacity=".3"
        strokeWidth="3"
        strokeLinecap="round"
      />
      {booster ? (
        <>
          <path d="m44 16-17 22h12l-4 16 18-24H41Z" fill="#ffe6a0" />
          {id === 'BOOSTER_X4' && (
            <text
              x="59"
              y="58"
              textAnchor="middle"
              fill="#fff4d8"
              fontSize="16"
              fontWeight="800"
            >
              4
            </text>
          )}
        </>
      ) : streak ? (
        <path
          d="M42 15c5 15 17 16 14 29-3 16-30 17-33 1-2-9 6-16 9-22 0 10 7 12 10-8Z"
          fill="#ffce85"
        />
      ) : veteran ? (
        <>
          <path d="m17 29 18 7-8 11-12-9m48-9-18 7 8 11 12-9" fill="#a4dbdb" />
          <path
            d="m40 21 5 11 12 1-9 8 3 12-11-6-11 6 3-12-9-8 12-1Z"
            fill="#ffe7a5"
          />
          <text
            x="40"
            y="69"
            textAnchor="middle"
            fill="#443726"
            fontSize="12"
            fontWeight="800"
          >
            {id === 'VETERAN_100' ? '100' : '25'}
          </text>
        </>
      ) : win ? (
        <>
          <path
            d="M29 21h22v13a11 11 0 0 1-22 0Zm0 4h-7v7c0 6 5 9 10 9m19-16h7v7c0 6-5 9-10 9M40 45v9m-9 1h18"
            fill="#ffd989"
            stroke="#ffd989"
            strokeWidth="3"
            strokeLinejoin="round"
          />
        </>
      ) : id === 'HIGH_FLYER' ? (
        <>
          <path d="m19 47 17-25 12 17 6-8 12 16" fill="#a6d8d9" />
          <path d="m30 31 6-9 7 10-7-3Z" fill="#fff8de" />
          <path
            d="M19 50h43"
            stroke="#fff8de"
            strokeWidth="4"
            strokeLinecap="round"
          />
        </>
      ) : id === 'RISK_TAKER' ? (
        <>
          <circle cx="40" cy="34" r="16" stroke="#b2dad6" strokeWidth="2" />
          <path d="m48 21-4 18-12 9 4-18Z" fill="#ffdf9a" />
        </>
      ) : (
        <>
          <path
            d="M25 29a15 15 0 1 1 30 0c0 9-10 17-10 17H35S25 38 25 29Z"
            fill="#ffc781"
          />
          <path d="M40 14c-9 9-7 21 0 32 7-11 9-23 0-32Z" fill="#ef976e" />
          <path d="m34 48 2 8h8l2-8" fill="#fff0ca" />
        </>
      )}
    </svg>
  );
}
